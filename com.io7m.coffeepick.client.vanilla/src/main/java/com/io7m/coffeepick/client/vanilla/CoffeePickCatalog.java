/*
 * Copyright © 2018 Mark Raynsford <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.coffeepick.client.vanilla;

import com.io7m.coffeepick.api.CoffeePickCatalogEventRepositoryUpdate;
import com.io7m.coffeepick.api.CoffeePickCatalogEventRuntimeDownloadFinished;
import com.io7m.coffeepick.api.CoffeePickCatalogEventRuntimeDownloading;
import com.io7m.coffeepick.api.CoffeePickCatalogEventType;
import com.io7m.coffeepick.api.CoffeePickCatalogType;
import com.io7m.coffeepick.api.CoffeePickInventoryType;
import com.io7m.coffeepick.api.CoffeePickIsCancelledType;
import com.io7m.coffeepick.api.CoffeePickSearch;
import com.io7m.coffeepick.api.CoffeePickSearches;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryContextType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventUpdateType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderRegistryEventType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderRegistryType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryType;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimeDescriptionType;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.net.http.HttpResponse.BodyHandlers.ofInputStream;

/**
 * The default catalog implementation.
 */

public final class CoffeePickCatalog implements CoffeePickCatalogType
{
  private static final Logger LOG = LoggerFactory.getLogger(CoffeePickCatalog.class);

  private final RuntimeRepositoryContextType context;
  private final RuntimeRepositoryProviderRegistryType repository_providers;
  private final Disposable subscription;
  private final Subject<CoffeePickCatalogEventType> events;
  private final Map<URI, RuntimeRepositoryType> runtime_repositories;
  private final Map<URI, Disposable> runtime_repository_subscriptions;
  private final HttpClient http;

  private CoffeePickCatalog(
    final Subject<CoffeePickCatalogEventType> in_events,
    final HttpClient in_client,
    final RuntimeRepositoryContextType in_context,
    final RuntimeRepositoryProviderRegistryType in_repository_providers)
  {
    this.events =
      Objects.requireNonNull(in_events, "events");
    this.http =
      Objects.requireNonNull(in_client, "client");
    this.context =
      Objects.requireNonNull(in_context, "context");
    this.repository_providers =
      Objects.requireNonNull(in_repository_providers, "repository_providers");

    this.runtime_repositories =
      new ConcurrentHashMap<>(128);
    this.runtime_repository_subscriptions =
      new ConcurrentHashMap<>(128);

    this.repository_providers.repositoryProviders()
      .values()
      .forEach(this::addRepositoryProvider);

    this.subscription =
      this.repository_providers.events()
        .subscribe(this::onRepositoriesChanged);
  }

  /**
   * Create a new catalog.
   *
   * @param events       A subject to which events will be published
   * @param client       The HTTP client that will be used
   * @param context      The runtime repository context
   * @param repositories A repository registry
   *
   * @return A new catalog
   */

  public static CoffeePickCatalogType create(
    final Subject<CoffeePickCatalogEventType> events,
    final HttpClient client,
    final RuntimeRepositoryContextType context,
    final RuntimeRepositoryProviderRegistryType repositories)
  {
    return new CoffeePickCatalog(events, client, context, repositories);
  }

  /**
   * A writer that writes an archive described by {@code descriptions}, reading from the stream
   * {@code input}, and publishes status events to {@code events}.
   *
   * @param description The runtime descriptions
   * @param events      The event receiver
   * @param input       The input stream
   * @param cancelled   A function that returns {@code true} if the operation should be cancelled
   *
   * @return A writer
   */

  public static CoffeePickInventoryType.RuntimeCancellableArchiveWriterType publishingWriter(
    final RuntimeDescription description,
    final Subject<CoffeePickCatalogEventType> events,
    final CoffeePickIsCancelledType cancelled,
    final InputStream input)
  {
    Objects.requireNonNull(description, "descriptions");
    Objects.requireNonNull(events, "events");
    Objects.requireNonNull(cancelled, "cancelled");
    Objects.requireNonNull(input, "input");

    return output -> {
      final var expected = description.archiveSize();
      var received_now = 0L;
      var received_then = 0L;
      var time_then = Instant.now();
      final var buffer = new byte[4096];

      while (true) {
        if (cancelled.isCancelled()) {
          throw new CancellationException();
        }

        final var r = input.read(buffer);
        if (r == -1) {
          break;
        }
        received_now = received_now + (long) r;
        output.write(buffer, 0, r);

        final var time_now = Instant.now();
        if (Duration.between(time_then, time_now).getSeconds() >= 1L) {
          final var difference = (double) received_now - (double) received_then;

          events.onNext(
            CoffeePickCatalogEventRuntimeDownloading.of(
              description.id(),
              difference,
              expected,
              received_now));

          time_then = time_now;
          received_then = received_now;
        }
      }

      events.onNext(CoffeePickCatalogEventRuntimeDownloadFinished.of(description.id()));
    };
  }

  private void addRepositoryProvider(
    final RuntimeRepositoryProviderType provider)
  {
    final var uri = provider.uri();
    LOG.info("setting up repository from provider {} ({})", provider.name(), uri);

    try {
      final var repos = provider.openRepository(this.context);
      this.runtime_repositories.put(uri, repos);
      this.runtime_repository_subscriptions.put(
        uri,
        repos.events()
          .filter(event -> event instanceof RuntimeRepositoryEventUpdateType)
          .cast(RuntimeRepositoryEventUpdateType.class)
          .subscribe(event -> this.events.onNext(CoffeePickCatalogEventRepositoryUpdate.of(event))));

    } catch (final IOException e) {
      LOG.error("could not open repository {}: ", uri, e);
    }
  }

  private void onRepositoriesChanged(
    final RuntimeRepositoryProviderRegistryEventType event)
  {
    final var provider = event.repositoryProvider();
    switch (event.change()) {
      case ADDED: {
        this.addRepositoryProvider(provider);
        break;
      }
      case REMOVED: {
        this.removeRepository(provider);
        break;
      }
    }
  }

  private void removeRepository(
    final RuntimeRepositoryProviderType provider)
  {
    final var uri = provider.uri();
    LOG.info("shutting down repository from provider {} ({})", provider.name(), uri);
    this.runtime_repositories.remove(uri);
    final var sub = this.runtime_repository_subscriptions.remove(uri);
    if (sub != null) {
      sub.dispose();
    }
  }

  @Override
  public Observable<CoffeePickCatalogEventType> events()
  {
    return this.events;
  }

  @Override
  public Map<String, RuntimeDescription> search(
    final CoffeePickSearch parameters)
  {
    Objects.requireNonNull(parameters, "parameters");

    return this.runtime_repositories.values()
      .stream()
      .flatMap(m -> m.runtimes().values().stream())
      .filter(r -> CoffeePickSearches.matches(r, parameters))
      .collect(Collectors.toMap(RuntimeDescriptionType::id, Function.identity()));
  }

  @Override
  public Optional<RuntimeDescription> searchExact(
    final String id)
  {
    Objects.requireNonNull(id, "id");

    for (final var repository : this.runtime_repositories.values()) {
      final var runtimes = repository.runtimes();
      final var description = runtimes.get(id);
      if (description != null) {
        return Optional.of(description);
      }
    }
    return Optional.empty();
  }

  @Override
  public InputStream fetch(
    final String id)
    throws IOException
  {
    Objects.requireNonNull(id, "id");

    final var description =
      this.searchExact(id).orElseThrow(() -> new FileNotFoundException(
        new StringBuilder("No archive available with the given ID")
          .append(System.lineSeparator())
          .append("  ID: ")
          .append(id)
          .toString()));

    final var request =
      HttpRequest.newBuilder(description.archiveURI())
        .GET()
        .build();

    try {
      final var response = this.http.send(request, ofInputStream());
      if (response.statusCode() >= 400) {
        final var separator = System.lineSeparator();
        throw new IOException(
          new StringBuilder(128)
            .append("HTTP error")
            .append(separator)
            .append("  URI:         ")
            .append(description.archiveURI())
            .append(separator)
            .append("  Status code: ")
            .append(response.statusCode())
            .append(separator)
            .toString());
      }

      return response.body();
    } catch (final InterruptedException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void updateRepository(
    final URI uri,
    final CoffeePickIsCancelledType cancelled)
    throws Exception
  {
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(cancelled, "cancelled");

    final var repository = this.runtime_repositories.get(uri);
    if (repository != null) {
      repository.update(cancelled::isCancelled);
    }
  }
}

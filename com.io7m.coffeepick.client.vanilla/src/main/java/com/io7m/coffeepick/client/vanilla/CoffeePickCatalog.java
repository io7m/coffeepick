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

import com.io7m.coffeepick.api.CoffeePickCatalogEventRepositoryAdded;
import com.io7m.coffeepick.api.CoffeePickCatalogEventRepositoryRemoved;
import com.io7m.coffeepick.api.CoffeePickCatalogEventRepositoryUpdateFailed;
import com.io7m.coffeepick.api.CoffeePickCatalogEventRuntimeDownloadFinished;
import com.io7m.coffeepick.api.CoffeePickCatalogEventRuntimeDownloading;
import com.io7m.coffeepick.api.CoffeePickCatalogEventType;
import com.io7m.coffeepick.api.CoffeePickCatalogType;
import com.io7m.coffeepick.api.CoffeePickInventoryType;
import com.io7m.coffeepick.api.CoffeePickSearch;
import com.io7m.coffeepick.api.CoffeePickSearches;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryRegistryEventType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryRegistryType;
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
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The default catalog implementation.
 */

public final class CoffeePickCatalog implements CoffeePickCatalogType
{
  private static final Logger LOG = LoggerFactory.getLogger(CoffeePickCatalog.class);

  private final RuntimeRepositoryRegistryType repositories;
  private final Disposable subscription;
  private final Subject<CoffeePickCatalogEventType> events;
  private final Map<String, URI> runtime_repositories;
  private final Map<URI, Map<String, RuntimeDescription>> repository_runtimes;

  private CoffeePickCatalog(
    final Subject<CoffeePickCatalogEventType> in_events,
    final RuntimeRepositoryRegistryType in_repositories)
  {
    this.events =
      Objects.requireNonNull(in_events, "events");
    this.repositories =
      Objects.requireNonNull(in_repositories, "repositories");

    this.runtime_repositories =
      new HashMap<>(128);
    this.repository_runtimes =
      new HashMap<>(16);

    this.repositories.repositories()
      .values()
      .forEach(this::addRepository);

    this.subscription =
      this.repositories.events()
        .subscribe(this::onRepositoriesChanged);
  }

  private void onRepositoriesChanged(
    final RuntimeRepositoryRegistryEventType event)
  {
    switch (event.change()) {
      case ADDED: {
        this.addRepository(event.repository());
        break;
      }
      case REMOVED: {
        this.removeRepository(event.repository());
        break;
      }
    }
  }

  private void removeRepository(
    final RuntimeRepositoryType repository)
  {
    LOG.debug("shutting down repository {} ({})", repository.name(), repository.uri());

    final var uri = repository.uri();
    final var runtimes = this.repository_runtimes.getOrDefault(uri, Map.of());

    for (final var entry : runtimes.entrySet()) {
      this.runtime_repositories.remove(entry.getKey());
    }

    this.repository_runtimes.remove(uri);
    this.events.onNext(CoffeePickCatalogEventRepositoryRemoved.of(uri));
  }

  private void addRepository(
    final RuntimeRepositoryType repository)
  {
    LOG.debug("setting up repository {} ({})", repository.name(), repository.uri());

    final var uri = repository.uri();

    try {
      final var runtimes = repository.availableRuntimes();
      for (final var description : runtimes) {
        this.runtime_repositories.put(description.id(), uri);
      }

      final var runtimes_map =
        runtimes.stream()
          .collect(Collectors.toMap(RuntimeDescriptionType::id, Function.identity()));

      this.repository_runtimes.put(uri, runtimes_map);
      this.events.onNext(CoffeePickCatalogEventRepositoryAdded.of(uri));
    } catch (final IOException e) {
      this.events.onNext(CoffeePickCatalogEventRepositoryUpdateFailed.of(uri, e));
    }
  }

  /**
   * Create a new catalog.
   *
   * @param events       A subject to which events will be published
   * @param repositories A repository registry
   *
   * @return A new catalog
   */

  public static CoffeePickCatalogType create(
    final Subject<CoffeePickCatalogEventType> events,
    final RuntimeRepositoryRegistryType repositories)
  {
    return new CoffeePickCatalog(events, repositories);
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

    return this.repository_runtimes.values()
      .stream()
      .flatMap(m -> m.values().stream())
      .filter(r -> CoffeePickSearches.matches(r, parameters))
      .collect(Collectors.toMap(RuntimeDescriptionType::id, Function.identity()));
  }

  @Override
  public Optional<RuntimeDescription> searchExact(
    final String id)
  {
    Objects.requireNonNull(id, "id");

    return Optional.ofNullable(this.runtime_repositories.get(id))
      .flatMap(uri -> Optional.ofNullable(this.repository_runtimes.get(uri)))
      .flatMap(map -> Optional.ofNullable(map.get(id)));
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

    return description.archiveURI()
      .toURL()
      .openStream();
  }

  /**
   * A writer that writes an archive described by {@code description}, reading from the stream
   * {@code input}, and publishes status events to {@code events}.
   *
   * @param description The runtime description
   * @param events      The event receiver
   * @param input       The input stream
   *
   * @return A writer
   */

  public static CoffeePickInventoryType.RuntimeArchiveWriterType publishingWriter(
    final RuntimeDescription description,
    final Subject<CoffeePickCatalogEventType> events,
    final InputStream input)
  {
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(events, "events");
    Objects.requireNonNull(input, "input");

    return output -> {
      final var expected = description.archiveSize();
      var received_now = 0L;
      var received_then = 0L;
      var time_then = Instant.now();
      final var buffer = new byte[4096];

      while (true) {
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
}

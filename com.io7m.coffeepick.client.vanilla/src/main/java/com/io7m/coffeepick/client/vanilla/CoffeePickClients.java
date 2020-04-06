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

import com.io7m.coffeepick.api.CoffeePickCatalogEventType;
import com.io7m.coffeepick.api.CoffeePickCatalogType;
import com.io7m.coffeepick.api.CoffeePickClientProviderType;
import com.io7m.coffeepick.api.CoffeePickClientType;
import com.io7m.coffeepick.api.CoffeePickEventType;
import com.io7m.coffeepick.api.CoffeePickInventoryEventType;
import com.io7m.coffeepick.api.CoffeePickInventoryType;
import com.io7m.coffeepick.api.CoffeePickSearch;
import com.io7m.coffeepick.api.CoffeePickTaskEventFailed;
import com.io7m.coffeepick.api.CoffeePickTaskEventStarted;
import com.io7m.coffeepick.api.CoffeePickTaskEventSucceeded;
import com.io7m.coffeepick.api.CoffeePickVerification;
import com.io7m.coffeepick.client.vanilla.internal.CoffeePickStrings;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoriesServiceLoaderProvider;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryContextType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderRegistryType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryType;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.parser.api.CoffeePickParsersType;
import com.io7m.coffeepick.runtime.parser.api.CoffeePickSerializersType;
import com.io7m.coffeepick.runtime.parser.spi.FormatDescription;
import com.io7m.coffeepick.runtime.parser.spi.FormatVersion;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * The default client provider implementation.
 */

public final class CoffeePickClients implements CoffeePickClientProviderType
{
  private final RuntimeRepositoryProviderRegistryType repositories;

  private CoffeePickClients(
    final RuntimeRepositoryProviderRegistryType in_repositories)
  {
    this.repositories = Objects.requireNonNull(in_repositories, "repositories");
  }

  /**
   * Create a new client provider, picking up dependencies via ServiceLoader.
   *
   * @return A new client provider
   */

  public static CoffeePickClientProviderType create()
  {
    return createWith(RuntimeRepositoriesServiceLoaderProvider.create());
  }

  /**
   * Create a new client provider.
   *
   * @param repositories The repository registry
   *
   * @return A new client provider
   */

  public static CoffeePickClientProviderType createWith(
    final RuntimeRepositoryProviderRegistryType repositories)
  {
    return new CoffeePickClients(repositories);
  }

  @Override
  public CoffeePickClientType newClient(
    final CoffeePickParsersType parsers,
    final CoffeePickSerializersType serializers,
    final Path base_directory,
    final HttpClient http)
    throws IOException
  {
    Objects.requireNonNull(parsers, "parsers");
    Objects.requireNonNull(serializers, "serializers");
    Objects.requireNonNull(base_directory, "base_directory");
    Objects.requireNonNull(http, "http");

    final var events =
      BehaviorSubject.<CoffeePickEventType>create()
        .toSerialized();

    final var context =
      CoffeePickRuntimeRepositoryContext.open(base_directory, http);

    @SuppressWarnings("unchecked") final var catalog_events =
      (Subject<CoffeePickCatalogEventType>) (Object) events;
    final var catalog =
      CoffeePickCatalog.create(
        catalog_events,
        http,
        context,
        this.repositories);

    @SuppressWarnings("unchecked") final var inventory_events =
      (Subject<CoffeePickInventoryEventType>) (Object) events;
    final var inventory =
      CoffeePickInventory.open(
        inventory_events,
        base_directory.resolve("inventory"));

    return new Client(
      CoffeePickStrings.of(CoffeePickStrings.getResourceBundle()),
      events,
      inventory,
      catalog,
      context,
      this.repositories,
      base_directory,
      http,
      parsers,
      serializers);
  }

  private static final class Client implements CoffeePickClientType
  {
    private final CoffeePickInventoryType inventory;
    private final CoffeePickCatalogType catalog;
    private final RuntimeRepositoryContextType context;
    private final Path base_directory;
    private final RuntimeRepositoryProviderRegistryType repositories;
    private final ExecutorService executor;
    private final AtomicBoolean closed;
    private final CoffeePickStrings strings;
    private final Subject<CoffeePickEventType> events;
    private final HttpClient http;
    private final CoffeePickParsersType parsers;
    private final CoffeePickSerializersType serializers;

    Client(
      final CoffeePickStrings in_strings,
      final Subject<CoffeePickEventType> in_events,
      final CoffeePickInventoryType in_inventory,
      final CoffeePickCatalogType in_catalog,
      final RuntimeRepositoryContextType in_context,
      final RuntimeRepositoryProviderRegistryType in_repositories,
      final Path in_base_directory,
      final HttpClient in_http,
      final CoffeePickParsersType in_parsers,
      final CoffeePickSerializersType in_serializers)
    {
      this.strings =
        Objects.requireNonNull(in_strings, "strings");
      this.events =
        Objects.requireNonNull(in_events, "events");
      this.inventory =
        Objects.requireNonNull(in_inventory, "inventory");
      this.catalog =
        Objects.requireNonNull(in_catalog, "catalog");
      this.context =
        Objects.requireNonNull(in_context, "context");
      this.base_directory =
        Objects.requireNonNull(in_base_directory, "base_directory");
      this.repositories =
        Objects.requireNonNull(in_repositories, "repositories");
      this.http =
        Objects.requireNonNull(in_http, "http");
      this.parsers =
        Objects.requireNonNull(in_parsers, "parsers");
      this.serializers =
        Objects.requireNonNull(in_serializers, "serializers");

      this.executor = Executors.newFixedThreadPool(1, runnable -> {
        final var thread = new Thread(runnable);
        thread.setName(
          new StringBuilder(32)
            .append("com.io7m.coffeepick.client.vanilla.CoffeePickClients.")
            .append(thread.getId())
            .toString());
        return thread;
      });

      this.closed = new AtomicBoolean(false);
    }

    @Override
    public void close()
    {
      if (this.closed.compareAndSet(false, true)) {
        this.executor.shutdown();
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Observable<CoffeePickEventType> events()
    {
      return this.events;
    }

    @Override
    public CompletableFuture<Void> inventoryDelete(
      final String id)
    {
      Objects.requireNonNull(id, "id");
      this.checkNotClosed();
      return this.submit(
        this.strings.inventoryDelete(id),
        future -> {
          this.inventory.delete(id);
          return null;
        });
    }

    @Override
    public CompletableFuture<CoffeePickVerification> inventoryVerify(
      final String id)
    {
      Objects.requireNonNull(id, "id");
      this.checkNotClosed();
      return this.submit(
        this.strings.inventoryVerify(id),
        future -> this.inventory.verify(id, future::isCancelled)
      );
    }

    @Override
    public CompletableFuture<Map<String, RuntimeDescription>> inventorySearch(
      final CoffeePickSearch parameters)
    {
      Objects.requireNonNull(parameters, "parameters");
      this.checkNotClosed();
      return this.submit(
        this.strings.inventorySearch(),
        future -> this.inventory.search(parameters)
      );
    }

    @Override
    public CompletableFuture<Optional<RuntimeDescription>> inventorySearchExact(
      final String id)
    {
      Objects.requireNonNull(id, "id");
      this.checkNotClosed();
      return this.submit(
        this.strings.inventorySearch(),
        future -> this.inventory.searchExact(id)
      );
    }

    @Override
    public CompletableFuture<Map<String, RuntimeDescription>> catalogSearch(
      final CoffeePickSearch parameters)
    {
      Objects.requireNonNull(parameters, "parameters");
      this.checkNotClosed();
      return this.submit(
        this.strings.catalogSearch(),
        future -> this.catalog.search(parameters)
      );
    }

    @Override
    public CompletableFuture<Path> catalogDownload(
      final String id)
    {
      Objects.requireNonNull(id, "id");
      this.checkNotClosed();

      @SuppressWarnings("unchecked") final var catalog_events =
        (Subject<CoffeePickCatalogEventType>) (Object) this.events;

      return this.submit(
        this.strings.catalogDownload(id),
        future -> this.doDownload(id, catalog_events, future)
      );
    }

    private Path doDownload(
      final String id,
      final Subject<CoffeePickCatalogEventType> catalog_events,
      final CompletableFuture<Path> future)
      throws IOException, InterruptedException
    {
      final var description = this.catalog.searchExactOrFail(id);
      final var uri = description.archiveURI();

      final var request =
        HttpRequest.newBuilder(uri)
          .GET()
          .build();

      final var response =
        this.http.send(request, HttpResponse.BodyHandlers.ofInputStream());

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

      try (var input = response.body()) {
        return this.inventory.write(
          description,
          CoffeePickCatalog.publishingWriter(
            description, catalog_events, future::isCancelled, input));
      }
    }

    @Override
    public CompletableFuture<Path> catalogDownloadIfNecessary(
      final String id)
    {
      Objects.requireNonNull(id, "id");
      this.checkNotClosed();

      @SuppressWarnings("unchecked") final var catalog_events =
        (Subject<CoffeePickCatalogEventType>) (Object) this.events;

      return this.submit(
        this.strings.catalogDownload(id),
        future -> {
          final var result = this.inventory.pathOf(id);
          if (result.isPresent()) {
            return result.get();
          }
          return this.doDownload(id, catalog_events, future);
        });
    }

    @Override
    public CompletableFuture<Optional<Path>> inventoryPathOf(
      final String id)
    {
      Objects.requireNonNull(id, "id");
      this.checkNotClosed();

      return this.submit(
        this.strings.inventoryPathOf(id),
        future -> this.inventory.pathOf(id)
      );
    }

    @Override
    public CompletableFuture<Path> inventoryUnpack(
      final String id,
      final Path path,
      final Set<CoffeePickInventoryType.UnpackOption> options)
    {
      Objects.requireNonNull(id, "id");
      Objects.requireNonNull(path, "path");
      Objects.requireNonNull(options, "options");
      this.checkNotClosed();

      return this.submit(
        this.strings.inventoryUnpack(id, path),
        future -> this.inventory.unpack(id, path, future::isCancelled, options)
      );
    }

    @Override
    public CompletableFuture<Void> repositoryUpdate(
      final URI uri)
    {
      Objects.requireNonNull(uri, "uri");
      this.checkNotClosed();

      return this.submit(
        this.strings.repositoryUpdate(uri),
        future -> {
          this.catalog.updateRepository(uri);
          return null;
        });
    }

    @Override
    public CompletableFuture<List<RuntimeRepositoryType>> repositoryList()
    {
      return this.submit(
        this.strings.repositoryList(),
        future -> this.catalog.listRepositories()
          .stream()
          .sorted(Comparator.comparing(repos -> repos.description().id()))
          .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<Void> repositoryExport(
      final URI repository,
      final URI format,
      final Path output_path)
    {
      Objects.requireNonNull(repository, "uri");
      Objects.requireNonNull(format, "format");
      Objects.requireNonNull(output_path, "output_path");

      final CompletableFuture<FormatDescription> find_format =
        this.submit(
          this.strings.repositoryExport(repository, format, output_path),
          future -> this.doFindFormat(format)
        );

      return find_format.thenCompose(
        description -> this.repositoryExport(
          repository,
          description,
          output_path));
    }

    @Override
    public CompletableFuture<Void> repositoryExport(
      final URI repository,
      final FormatDescription format,
      final FormatVersion version,
      final Path output_path)
    {
      Objects.requireNonNull(repository, "uri");
      Objects.requireNonNull(format, "format");
      Objects.requireNonNull(version, "version");
      Objects.requireNonNull(output_path, "output_path");

      return this.submit(
        this.strings.repositoryExport(repository, format.name(), output_path),
        future -> this.doExport(repository, format, version, output_path)
      );
    }

    @Override
    public CompletableFuture<Void> repositoryExport(
      final URI repository,
      final FormatDescription format,
      final Path output_path)
    {
      Objects.requireNonNull(repository, "uri");
      Objects.requireNonNull(format, "format");
      Objects.requireNonNull(output_path, "output_path");

      final CompletableFuture<FormatVersion> find_format =
        this.submit(
          this.strings.repositoryExport(repository, format.name(), output_path),
          future -> this.doFindFormatVersion(format)
        );

      return find_format.thenCompose(
        version -> this.submit(
          this.strings.repositoryExport(repository, format.name(), output_path),
          future -> this.doExport(repository, format, version, output_path)
        )
      );
    }

    private Void doExport(
      final URI repository,
      final FormatDescription format,
      final FormatVersion version,
      final Path output_path)
      throws IOException
    {
      final var repos =
        this.catalog.listRepositories()
          .stream()
          .filter(p -> Objects.equals(p.description().id(), repository))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("No such repository: " + repository));

      try (var output = Files.newOutputStream(output_path)) {
        try (var serial = this.serializers.createSerializer(
          format,
          version,
          output)) {
          serial.serialize(repos.description());
        }
        output.flush();
      }
      return null;
    }

    private FormatVersion doFindFormatVersion(
      final FormatDescription format)
      throws IOException
    {
      final var supported = this.serializers.findSupportedVersions(format);
      if (supported.isEmpty()) {
        throw new IOException(
          new StringBuilder(128)
            .append("No suitable format provider.")
            .append(System.lineSeparator())
            .append("  Format: ")
            .append(format.name())
            .append(' ')
            .append(format.description())
            .append(System.lineSeparator())
            .toString());
      }
      return supported.last();
    }

    private FormatDescription doFindFormat(
      final URI format)
      throws IOException
    {
      return this.serializers.findFormat(format)
        .orElseThrow(() ->
                       new IOException(
                         new StringBuilder(128)
                           .append("No suitable format provider.")
                           .append(System.lineSeparator())
                           .append("  Format: ")
                           .append(format)
                           .append(System.lineSeparator())
                           .toString()));
    }

    private <T> CompletableFuture<T> submit(
      final String message,
      final TaskType<T> callable)
    {
      Objects.requireNonNull(message, "message");
      Objects.requireNonNull(callable, "callable");

      final var future = new CompletableFuture<T>();
      this.executor.execute(() -> {
        try {
          this.events.onNext(
            CoffeePickTaskEventStarted.builder()
              .setDescription(message)
              .build()
          );

          future.complete(callable.execute(future));

          this.events.onNext(
            CoffeePickTaskEventSucceeded.builder()
              .setDescription(message)
              .build()
          );
        } catch (final Throwable ex) {
          future.completeExceptionally(ex);

          this.events.onNext(
            CoffeePickTaskEventFailed.builder()
              .setDescription(message)
              .build()
          );
        }
      });

      return future;
    }

    private void checkNotClosed()
    {
      if (this.closed.get()) {
        throw new IllegalStateException("Client is closed");
      }
    }

    private interface TaskType<T>
    {
      T execute(CompletableFuture<T> future)
        throws Exception;
    }
  }
}

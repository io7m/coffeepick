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
import com.io7m.coffeepick.api.CoffeePickOperation;
import com.io7m.coffeepick.api.CoffeePickSearch;
import com.io7m.coffeepick.api.CoffeePickVerification;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoriesServiceLoader;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryRegistryType;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The default client provider implementation.
 */

public final class CoffeePickClients implements CoffeePickClientProviderType
{
  private final RuntimeRepositoryRegistryType repositories;

  private CoffeePickClients(
    final RuntimeRepositoryRegistryType in_repositories)
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
    return createWith(RuntimeRepositoriesServiceLoader.create());
  }

  /**
   * Create a new client provider.
   *
   * @param repositories The repository registry
   *
   * @return A new client provider
   */

  public static CoffeePickClientProviderType createWith(
    final RuntimeRepositoryRegistryType repositories)
  {
    return new CoffeePickClients(repositories);
  }

  @Override
  public CoffeePickClientType newClient(final Path base_directory)
    throws IOException
  {
    Objects.requireNonNull(base_directory, "base_directory");

    final var events =
      BehaviorSubject.<CoffeePickEventType>create()
        .toSerialized();

    @SuppressWarnings("unchecked") final var catalog_events =
      (Subject<CoffeePickCatalogEventType>) (Object) events;
    final var catalog =
      CoffeePickCatalog.create(catalog_events, this.repositories);

    @SuppressWarnings("unchecked") final var inventory_events =
      (Subject<CoffeePickInventoryEventType>) (Object) events;
    final var inventory =
      CoffeePickInventory.open(inventory_events, base_directory.resolve("inventory"));

    return new Client(events, inventory, catalog, this.repositories, base_directory);
  }

  private static final class Client implements CoffeePickClientType
  {
    private final CoffeePickInventoryType inventory;
    private final CoffeePickCatalogType catalog;
    private final Path base_directory;
    private final RuntimeRepositoryRegistryType repositories;
    private final ExecutorService executor;
    private final AtomicBoolean closed;
    private final Subject<CoffeePickEventType> events;
    private volatile BigInteger command_id;

    Client(
      final Subject<CoffeePickEventType> in_events,
      final CoffeePickInventoryType in_inventory,
      final CoffeePickCatalogType in_catalog,
      final RuntimeRepositoryRegistryType in_repositories,
      final Path in_base_directory)
    {
      this.events =
        Objects.requireNonNull(in_events, "events");
      this.inventory =
        Objects.requireNonNull(in_inventory, "inventory");
      this.catalog =
        Objects.requireNonNull(in_catalog, "catalog");
      this.base_directory =
        Objects.requireNonNull(in_base_directory, "base_directory");
      this.repositories =
        Objects.requireNonNull(in_repositories, "repositories");

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
      this.command_id = BigInteger.ZERO;
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
    public CoffeePickOperation<Void> inventoryDelete(
      final String id)
    {
      Objects.requireNonNull(id, "id");
      this.checkNotClosed();
      return this.submit(() -> {
        this.inventory.delete(id);
        return null;
      });
    }

    @Override
    public CoffeePickOperation<CoffeePickVerification> inventoryVerify(
      final String id)
    {
      Objects.requireNonNull(id, "id");
      this.checkNotClosed();
      return this.submit(() -> this.inventory.verify(id));
    }

    @Override
    public CoffeePickOperation<Map<String, RuntimeDescription>> inventorySearch(
      final CoffeePickSearch parameters)
    {
      Objects.requireNonNull(parameters, "parameters");
      this.checkNotClosed();
      return this.submit(() -> this.inventory.search(parameters));
    }

    @Override
    public CoffeePickOperation<Optional<RuntimeDescription>> inventorySearchExact(
      final String id)
    {
      Objects.requireNonNull(id, "id");
      this.checkNotClosed();
      return this.submit(() -> this.inventory.searchExact(id));
    }

    @Override
    public CoffeePickOperation<Map<String, RuntimeDescription>> catalogSearch(
      final CoffeePickSearch parameters)
    {
      Objects.requireNonNull(parameters, "parameters");
      this.checkNotClosed();
      return this.submit(() -> this.catalog.search(parameters));
    }

    @Override
    public CoffeePickOperation<Path> catalogDownload(
      final String id)
    {
      Objects.requireNonNull(id, "id");
      this.checkNotClosed();

      @SuppressWarnings("unchecked") final var catalog_events =
        (Subject<CoffeePickCatalogEventType>) (Object) this.events;

      return this.submit(() -> {
        final var description = this.catalog.searchExactOrFail(id);
        final var uri = description.archiveURI();
        try (var input = uri.toURL().openStream()) {
          return this.inventory.write(
            description, CoffeePickCatalog.publishingWriter(description, catalog_events, input));
        }
      });
    }

    private <T> CoffeePickOperation<T> submit(
      final Callable<T> callable)
    {
      Objects.requireNonNull(callable, "callable");

      final var id = this.command_id;
      this.command_id = id.add(BigInteger.ONE);

      final var future = new CompletableFuture<T>();
      this.executor.execute(() -> {
        try {
          future.complete(callable.call());
        } catch (final Throwable ex) {
          future.completeExceptionally(ex);
        }
      });

      return CoffeePickOperation.of(id, future);
    }

    private void checkNotClosed()
    {
      if (this.closed.get()) {
        throw new IllegalStateException("Client is closed");
      }
    }
  }
}

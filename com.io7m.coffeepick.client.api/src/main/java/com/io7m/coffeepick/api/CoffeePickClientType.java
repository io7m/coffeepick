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

package com.io7m.coffeepick.api;

import com.io7m.coffeepick.runtime.RuntimeDescription;
import io.reactivex.Observable;

import java.io.Closeable;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.io7m.coffeepick.api.CoffeePickInventoryType.UnpackOption;

/**
 * The CoffeePick client API.
 */

public interface CoffeePickClientType extends Closeable
{
  /**
   * @return A stream of events published by the client
   */

  Observable<CoffeePickEventType> events();

  /**
   * Delete the runtime with the given ID from the inventory.
   *
   * @param id The ID
   *
   * @return The operation in progress
   */

  CompletableFuture<Void> inventoryDelete(
    String id);

  /**
   * Verify the runtime with the given ID in the inventory.
   *
   * @param id The ID
   *
   * @return The operation in progress
   */

  CompletableFuture<CoffeePickVerification> inventoryVerify(
    String id);

  /**
   * Search for runtimes matching the given parameters in the inventory.
   *
   * @param parameters The search parameters
   *
   * @return The operation in progress
   */

  CompletableFuture<Map<String, RuntimeDescription>> inventorySearch(
    CoffeePickSearch parameters);

  /**
   * Search for all runtimes in the inventory.
   *
   * @return The operation in progress
   */

  default CompletableFuture<Map<String, RuntimeDescription>> inventorySearchAll()
  {
    return this.inventorySearch(
      CoffeePickSearch.builder()
        .build());
  }

  /**
   * Search for a runtime with the given ID in the inventory.
   *
   * @param id The ID
   *
   * @return The operation in progress
   */

  CompletableFuture<Optional<RuntimeDescription>> inventorySearchExact(
    String id);

  /**
   * Return the path of the archive of the runtime in the inventory, or nothing if the runtime isn't
   * installed.
   *
   * @param id The ID
   *
   * @return The operation in progress
   */

  CompletableFuture<Optional<Path>> inventoryPathOf(
    String id);

  /**
   * Unpack the runtime with the given ID to the given path.
   *
   * @param id      The ID
   * @param path    The target directory
   * @param options The unpacking options
   *
   * @return The operation in progress
   */

  CompletableFuture<Path> inventoryUnpack(
    String id,
    Path path,
    Set<UnpackOption> options);

  /**
   * Search for runtimes matching the given parameters in the catalog.
   *
   * @param parameters The search parameters
   *
   * @return The operation in progress
   */

  CompletableFuture<Map<String, RuntimeDescription>> catalogSearch(
    CoffeePickSearch parameters);

  /**
   * Search for all runtimes in the catalog.
   *
   * @return The operation in progress
   */

  default CompletableFuture<Map<String, RuntimeDescription>> catalogSearchAll()
  {
    return this.catalogSearch(
      CoffeePickSearch.builder()
        .build());
  }

  /**
   * Search for a runtime with the given ID in the catalog.
   *
   * @param id The ID
   *
   * @return The operation in progress
   */

  default CompletableFuture<Optional<RuntimeDescription>> catalogSearchExact(
    final String id)
  {
    return this.catalogSearch(
      CoffeePickSearch.builder()
        .setId(id)
        .build())
      .thenApply(results -> Optional.ofNullable(results.get(id)));
  }

  /**
   * Download the runtime with the given ID from the catalog, installing it into the inventory if
   * the download succeeds and the data is correctly verified.
   *
   * @param id The ID
   *
   * @return The operation in progress
   */

  CompletableFuture<Path> catalogDownload(
    String id);

  /**
   * Download the runtime with the given ID from the catalog, installing it into the inventory if
   * the download succeeds and the data is correctly verified. If the runtime is already in the
   * inventory, then skip the download.
   *
   * @param id The ID
   *
   * @return The operation in progress
   */

  CompletableFuture<Path> catalogDownloadIfNecessary(
    String id);

  /**
   * Update the repository with the given URI.
   *
   * @param uri The URI
   *
   * @return The operation in progress
   */

  CompletableFuture<Void> repositoryUpdate(
    URI uri);
}

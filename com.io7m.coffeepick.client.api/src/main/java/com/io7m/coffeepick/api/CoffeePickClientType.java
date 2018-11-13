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
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

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

  CoffeePickOperation<Void> inventoryDelete(
    String id);

  /**
   * Verify the runtime with the given ID in the inventory.
   *
   * @param id The ID
   *
   * @return The operation in progress
   */

  CoffeePickOperation<CoffeePickVerification> inventoryVerify(
    String id);

  /**
   * Search for runtimes matching the given parameters in the inventory.
   *
   * @param parameters The search parameters
   *
   * @return The operation in progress
   */

  CoffeePickOperation<Map<String, RuntimeDescription>> inventorySearch(
    CoffeePickSearch parameters);

  /**
   * Search for all runtimes in the inventory.
   *
   * @return The operation in progress
   */

  default CoffeePickOperation<Map<String, RuntimeDescription>> inventorySearchAll()
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

  CoffeePickOperation<Optional<RuntimeDescription>> inventorySearchExact(
    String id);

  /**
   * Search for runtimes matching the given parameters in the catalog.
   *
   * @param parameters The search parameters
   *
   * @return The operation in progress
   */

  CoffeePickOperation<Map<String, RuntimeDescription>> catalogSearch(
    CoffeePickSearch parameters);

  /**
   * Search for all runtimes in the catalog.
   *
   * @return The operation in progress
   */

  default CoffeePickOperation<Map<String, RuntimeDescription>> catalogSearchAll()
  {
    return this.catalogSearch(
      CoffeePickSearch.builder()
        .build());
  }

  /**
   * Download the runtime with the given ID from the catalog, installing it into the inventory if
   * the download succeeds and the data is correctly verified.
   *
   * @param id The ID
   *
   * @return The operation in progress
   */

  CoffeePickOperation<Path> catalogDownload(
    String id);
}

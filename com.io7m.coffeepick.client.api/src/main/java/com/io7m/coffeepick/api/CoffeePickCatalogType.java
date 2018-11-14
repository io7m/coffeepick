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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The interface exposed by the <i>catalog</i>. A <i>catalog</i> represents an aggregated set of
 * repositories over which searches can be performed. Essentially, the catalog is the set of
 * runtimes that the user can get. Contrast this to the <i>inventory</i>, which represents the set
 * of runtimes that the user currently has.
 *
 * @see CoffeePickInventoryType
 */

public interface CoffeePickCatalogType
{
  /**
   * Access the stream of events published by the catalog when the state of the catalog changes.
   *
   * @return A stream of events
   */

  Observable<CoffeePickCatalogEventType> events();

  /**
   * Search for all runtimes matching the given parameters.
   *
   * @param parameters The parameters
   *
   * @return The matching runtimes
   */

  Map<String, RuntimeDescription> search(CoffeePickSearch parameters);

  /**
   * @return All runtimes in the inventory
   */

  default Map<String, RuntimeDescription> searchAll()
  {
    return this.search(CoffeePickSearch.builder()
                         .build());
  }

  /**
   * @param id The ID of the runtime
   *
   * @return The runtime with the given ID
   */

  default Optional<RuntimeDescription> searchExact(
    final String id)
  {
    Objects.requireNonNull(id, "id");

    return Optional.ofNullable(
      this.search(
        CoffeePickSearch.builder()
          .setId(id)
          .build())
        .get(id));
  }

  /**
   * @param id The ID of the runtime
   *
   * @return The runtime with the given ID
   *
   * @throws FileNotFoundException If no runtime exists with the given ID
   */

  default RuntimeDescription searchExactOrFail(
    final String id)
    throws FileNotFoundException
  {
    Objects.requireNonNull(id, "id");

    return this.searchExact(id).orElseThrow(() -> new FileNotFoundException(
      new StringBuilder("No archive available with the given ID")
        .append(System.lineSeparator())
        .append("  ID: ")
        .append(id)
        .toString()));
  }

  /**
   * @param id The ID of the runtime
   *
   * @return An input stream representing the archive of the runtime
   *
   * @throws IOException On I/O errors
   */

  InputStream fetch(
    String id)
    throws IOException;

  /**
   * Update the repository with the given URI.
   *
   * @param uri The URI
   *
   * @throws Exception On errors
   */

  void updateRepository(
    URI uri)
    throws Exception;
}

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
import io.reactivex.rxjava3.core.Observable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;

/**
 * The interface exposed by the <i>inventory</i>. An <i>inventory</i> represents the set of runtimes
 * that the user currently has. Contrast this to the <i>catalog</i>, which represents the set of
 * runtimes that the user may get.
 *
 * @see CoffeePickCatalogType
 */

public interface CoffeePickInventoryType
{
  /**
   * Access the stream of events published by the inventory when the state of the inventory
   * changes.
   *
   * @return A stream of events
   */

  Observable<CoffeePickInventoryEventType> events();

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
   * Save a runtime. The method calls the given writer method when it needs to write an archive of
   * the runtime to disk.
   *
   * @param description The runtime runtimes
   * @param writer      A function that will be called to write data
   *
   * @return The path of the saved archive
   *
   * @throws IOException           On I/O errors
   * @throws CancellationException If {@code cancelled} returns {@code true} while the operation is
   *                               running
   */

  Path write(
    RuntimeDescription description,
    RuntimeCancellableArchiveWriterType writer)
    throws IOException, CancellationException;

  /**
   * Return the path of the given runtime in the inventory if it exists, or nothing if it does not.
   *
   * @param id The identifier
   *
   * @return The path to the runtime if it is installed
   *
   * @throws IOException On I/O errors
   */

  Optional<Path> pathOf(String id)
    throws IOException;

  /**
   * Unpack the runtime with the given ID to {@code path}. The method takes a function {@code
   * cancelled} that will be evaluated repeatedly and, if the function returns {@code true} at any
   * point, the operation will be cancelled.
   *
   * @param id        The runtime ID
   * @param path      The target path
   * @param options   The unpacking options
   * @param cancelled A function that returns {@code true} if the operation should be cancelled
   *
   * @return The path of the unpacked archive
   *
   * @throws IOException           On I/O errors
   * @throws CancellationException If {@code cancelled} returns {@code true} while the operation is
   *                               running
   */

  Path unpack(
    String id,
    Path path,
    CoffeePickIsCancelledType cancelled,
    Set<UnpackOption> options)
    throws IOException, CancellationException;

  /**
   * Unpack the runtime with the given ID to {@code path}. The method takes a function {@code
   * cancelled} that will be evaluated repeatedly and, if the function returns {@code true} at any
   * point, the operation will be cancelled.
   *
   * @param id        The runtime ID
   * @param path      The target path
   * @param cancelled A function that returns {@code true} if the operation should be cancelled
   *
   * @return The path of the unpacked archive
   *
   * @throws IOException           On I/O errors
   * @throws CancellationException If {@code cancelled} returns {@code true} while the operation is
   *                               running
   */

  default Path unpack(
    final String id,
    final Path path,
    final CoffeePickIsCancelledType cancelled)
    throws IOException, CancellationException
  {
    return this.unpack(id, path, cancelled, Set.of(UnpackOption.STRIP_NON_OWNER_WRITABLE));
  }

  /**
   * Unpack the runtime with the given ID to {@code path}.
   *
   * @param id   The runtime ID
   * @param path The target path
   *
   * @return The path of the unpacked archive
   *
   * @throws IOException On I/O errors
   */

  default Path unpack(
    final String id,
    final Path path)
    throws IOException
  {
    return this.unpack(id, path, () -> false, Set.of(UnpackOption.STRIP_NON_OWNER_WRITABLE));
  }

  /**
   * Delete the runtime with the given identifier. Does nothing if no runtime has the given
   * identifier.
   *
   * @param id The identifier
   *
   * @throws IOException On I/O errors
   */

  void delete(String id)
    throws IOException;

  /**
   * Verify the archive of the runtime with the given ID.
   *
   * @param id The identifier
   *
   * @return The verification results
   *
   * @throws IOException On I/O errors
   */

  default CoffeePickVerification verify(
    final String id)
    throws IOException
  {
    return this.verify(id, () -> false);
  }

  /**
   * Verify the archive of the runtime with the given ID. The method takes a function {@code
   * cancelled} that will be evaluated repeatedly and, if the function returns {@code true} at any
   * point, the operation will be cancelled.
   *
   * @param id        The identifier
   * @param cancelled A function that returns {@code true} if the operation should be cancelled
   *
   * @return The verification results
   *
   * @throws IOException           On I/O errors
   * @throws CancellationException If {@code cancelled} returns {@code true} while the operation is
   *                               running
   */

  CoffeePickVerification verify(
    String id,
    CoffeePickIsCancelledType cancelled)
    throws IOException, CancellationException;

  /**
   * Options for unpacking.
   */

  enum UnpackOption
  {
    /**
     * Strip the leading directory from archive files.
     */

    STRIP_LEADING_DIRECTORY,

    /**
     * On POSIX filesystems, remove write permissions for anything other than the owner.
     */

    STRIP_NON_OWNER_WRITABLE
  }

  /**
   * A function for writing archive data.
   */

  interface RuntimeCancellableArchiveWriterType
  {
    /**
     * Write data to the given stream. The function should throw {@code CancellationException} if
     * the write operation has been cancelled.
     *
     * @param stream An output stream
     *
     * @throws IOException           On I/O errors
     * @throws CancellationException If writing has been cancelled
     */

    void write(OutputStream stream)
      throws IOException, CancellationException;
  }
}

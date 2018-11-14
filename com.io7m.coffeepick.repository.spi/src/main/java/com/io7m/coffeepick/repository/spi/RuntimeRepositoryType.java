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

package com.io7m.coffeepick.repository.spi;

import com.io7m.coffeepick.runtime.RuntimeDescription;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * A repository of runtimes.
 */

public interface RuntimeRepositoryType
{
  /**
   * @return The URI that uniquely identifies this repository
   */

  URI uri();

  /**
   * @return The name of the repository
   */

  String name();

  /**
   * Obtain a read-only, immutable list of the available runtimes in the repository.
   *
   * @param context The current context
   *
   * @return The available runtimes
   *
   * @throws IOException On I/O errors
   */

  List<RuntimeDescription> availableRuntimes(
    RuntimeRepositoryContextType context)
    throws IOException;
}

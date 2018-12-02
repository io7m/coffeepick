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

package com.io7m.coffeepick.runtime.parser.api;

import com.io7m.coffeepick.runtime.parser.spi.FormatDescription;
import com.io7m.coffeepick.runtime.parser.spi.FormatVersion;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Optional;
import java.util.SortedSet;

/**
 * A provider of serializers.
 */

public interface CoffeePickSerializersType
{
  /**
   * @param format The output format
   *
   * @return The supported versions of the given format, if any
   */

  SortedSet<FormatVersion> findSupportedVersions(
    FormatDescription format);

  /**
   * Find the format with the given name.
   *
   * @param format The format name
   *
   * @return The format description if one exists
   */

  Optional<FormatDescription> findFormat(
    URI format);

  /**
   * Create a new serializer.
   *
   * @param format  The output format
   * @param version The output format version
   * @param output  The output stream
   *
   * @return A new serializer
   *
   * @throws IOException On I/O errors
   */

  CoffeePickSerializerType createSerializer(
    FormatDescription format,
    FormatVersion version,
    OutputStream output)
    throws IOException;

}

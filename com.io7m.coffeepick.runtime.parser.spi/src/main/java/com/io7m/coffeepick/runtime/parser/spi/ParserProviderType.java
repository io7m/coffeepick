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

package com.io7m.coffeepick.runtime.parser.spi;

import org.osgi.annotation.versioning.ProviderType;

import java.io.IOException;
import java.util.SortedSet;

/**
 * The type of parser providers.
 */

@ProviderType
public interface ParserProviderType
{
  /**
   * @return The format that this provider supports
   */

  FormatDescription parserFormatSupported();

  /**
   * @return The supported versions of the format
   */

  SortedSet<FormatVersion> parserFormatVersionsSupported();

  /**
   * @param request The parser request
   *
   * @return A new parser for the format
   *
   * @throws IOException On I/O or parser configuration errors
   */

  ParserType parserCreate(
    ParserRequest request)
    throws IOException;
}

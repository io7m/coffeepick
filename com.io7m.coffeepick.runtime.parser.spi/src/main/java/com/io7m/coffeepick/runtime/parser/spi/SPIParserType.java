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

import io.reactivex.Observable;
import org.osgi.annotation.versioning.ProviderType;

import java.io.Closeable;
import java.io.IOException;

/**
 * A parser.
 */

@ProviderType
public interface SPIParserType extends Closeable
{
  /**
   * @return The sequence of error events produced during parsing
   */

  Observable<ParseError> errors();

  /**
   * Parse something.
   *
   * @return The parsed value
   *
   * @throws IOException            On I/O errors
   * @throws ParserFailureException At the end of parsing if any error events have been raised, or
   *                                if parsing encounters an unrecoverable error
   */

  ParserResultType parse()
    throws IOException, ParserFailureException;
}

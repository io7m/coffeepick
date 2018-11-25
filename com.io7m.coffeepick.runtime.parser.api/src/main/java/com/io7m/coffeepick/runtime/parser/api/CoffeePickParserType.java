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

import com.io7m.coffeepick.runtime.parser.spi.ParseError;
import com.io7m.coffeepick.runtime.parser.spi.ParserFailureException;
import com.io7m.coffeepick.runtime.parser.spi.ParserResultType;
import io.reactivex.Observable;

import java.io.Closeable;
import java.io.IOException;

/**
 * A parser.
 */

public interface CoffeePickParserType extends Closeable
{
  /**
   * @return An observable stream of warnings and errors
   */

  Observable<ParseError> errors();

  /**
   * @return The name of the provider used to construct this parser
   */

  String provider();

  /**
   * Run the parser.
   *
   * @return The result of parsing
   *
   * @throws IOException            On I/O errors
   * @throws ParserFailureException On fatal parse errors
   */

  ParserResultType parse()
    throws IOException, ParserFailureException;
}

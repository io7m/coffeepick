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
import com.io7m.coffeepick.runtime.parser.spi.SPIParserType;
import io.reactivex.Observable;

import java.io.IOException;
import java.util.Objects;

final class CoffeePickParser implements CoffeePickParserType
{
  private final SPIParserType parser;
  private final String provider;

  CoffeePickParser(
    final String in_provider,
    final SPIParserType in_parser)
  {
    this.provider =
      Objects.requireNonNull(in_provider, "provider");
    this.parser =
      Objects.requireNonNull(in_parser, "parser");
  }

  @Override
  public Observable<ParseError> errors()
  {
    return this.parser.errors();
  }

  @Override
  public String provider()
  {
    return this.provider;
  }

  @Override
  public ParserResultType parse()
    throws IOException, ParserFailureException
  {
    return this.parser.parse();
  }

  @Override
  public void close()
    throws IOException
  {
    this.parser.close();
  }
}

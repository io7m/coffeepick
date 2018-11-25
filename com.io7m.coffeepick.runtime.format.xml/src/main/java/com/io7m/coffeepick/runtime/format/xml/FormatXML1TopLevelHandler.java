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

package com.io7m.coffeepick.runtime.format.xml;

import com.io7m.coffeepick.runtime.parser.spi.ParsedRepository;
import com.io7m.coffeepick.runtime.parser.spi.ParsedRuntime;
import com.io7m.coffeepick.runtime.parser.spi.ParserResultType;
import org.xml.sax.Attributes;
import org.xml.sax.ext.Locator2;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A content handler for parsing top-level values.
 */

public final class FormatXML1TopLevelHandler
  extends FormatXMLAbstractContentHandler<ParserResultType, ParserResultType>
{
  /**
   * Construct a handler.
   *
   * @param in_locator An XML locator
   */

  public FormatXML1TopLevelHandler(
    final Locator2 in_locator)
  {
    super(in_locator, Optional.empty());
  }

  @Override
  protected Map<String, Supplier<FormatXMLContentHandlerType<ParserResultType>>> onWantChildHandlers()
  {
    return Map.of(
      "runtime",
      () -> new FormatXML1RuntimeHandler(Optional.empty(), super.locator())
        .map(ParsedRuntime::of),
      "runtime-repository",
      () -> new FormatXML1RepositoryHandler(super.locator())
        .map(ParsedRepository::of)
    );
  }

  @Override
  protected String onWantHandlerName()
  {
    return FormatXML1TopLevelHandler.class.getSimpleName();
  }

  @Override
  protected Optional<ParserResultType> onElementFinishDirectly(
    final String namespace,
    final String name,
    final String qname)
  {
    return Optional.of(this.get());
  }

  @Override
  protected void onElementStartDirectly(
    final String namespace,
    final String name,
    final String qname,
    final Attributes attributes)
  {

  }

  @Override
  protected void onChildResultReceived(
    final ParserResultType value)
  {
    this.finish(value);
  }
}

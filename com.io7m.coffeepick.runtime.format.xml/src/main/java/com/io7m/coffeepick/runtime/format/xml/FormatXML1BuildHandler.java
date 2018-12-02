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

import com.io7m.coffeepick.runtime.RuntimeBuild;
import org.xml.sax.Attributes;
import org.xml.sax.ext.Locator2;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

/**
 * A content handler for parsing build information.
 */

public final class FormatXML1BuildHandler
  extends FormatXMLAbstractContentHandler<String, RuntimeBuild>
{
  private String build_number;
  private OffsetDateTime build_time;

  /**
   * Construct a handler.
   *
   * @param locator The XML locator
   */

  public FormatXML1BuildHandler(
    final Locator2 locator)
  {
    super(locator, Optional.of("build"));
  }

  @Override
  protected Map<String, Supplier<FormatXMLContentHandlerType<String>>> onWantChildHandlers()
  {
    return Map.of();
  }

  @Override
  protected String onWantHandlerName()
  {
    return FormatXML1BuildHandler.class.getSimpleName();
  }

  @Override
  protected Optional<RuntimeBuild> onElementFinishDirectly(
    final String namespace,
    final String name,
    final String qname)
  {
    return Optional.of(
      RuntimeBuild.builder()
        .setBuildNumber(this.build_number)
        .setTime(this.build_time)
        .build());
  }

  @Override
  protected void onElementStartDirectly(
    final String namespace,
    final String name,
    final String qname,
    final Attributes attributes)
  {
    this.build_number =
      attributes.getValue("number");
    this.build_time =
      OffsetDateTime.from(ISO_OFFSET_DATE_TIME.parse(attributes.getValue("time")));
  }

  @Override
  protected void onChildResultReceived(
    final String value)
  {

  }
}

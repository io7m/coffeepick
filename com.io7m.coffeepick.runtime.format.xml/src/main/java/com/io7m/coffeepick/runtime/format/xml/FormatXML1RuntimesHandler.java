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

import com.io7m.coffeepick.runtime.RuntimeDescription;
import org.xml.sax.Attributes;
import org.xml.sax.ext.Locator2;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A content handler for parsing sets of runtimes.
 */

public final class FormatXML1RuntimesHandler
  extends FormatXMLAbstractContentHandler<RuntimeDescription, List<RuntimeDescription>>
{
  private final ArrayList<RuntimeDescription> runtimes;
  private final URI repository;

  /**
   * Construct a handler.
   *
   * @param in_repository The repository URI
   * @param in_locator    An XML locator
   */

  public FormatXML1RuntimesHandler(
    final URI in_repository,
    final Locator2 in_locator)
  {
    super(
      in_locator,
      Optional.of("runtimes"));

    this.repository =
      Objects.requireNonNull(in_repository, "repository");
    this.runtimes =
      new ArrayList<>();
  }

  @Override
  protected Map<String, Supplier<FormatXMLContentHandlerType<RuntimeDescription>>> onWantChildHandlers()
  {
    return Map.of(
      "runtime",
      () -> new FormatXML1RuntimeHandler(Optional.of(this.repository), super.locator()));
  }

  @Override
  protected String onWantHandlerName()
  {
    return FormatXML1RuntimesHandler.class.getSimpleName();
  }

  @Override
  protected Optional<List<RuntimeDescription>> onElementFinishDirectly(
    final String namespace,
    final String name,
    final String qname)
  {
    return Optional.of(this.runtimes);
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
  protected void onChildResultReceived(final RuntimeDescription value)
  {
    this.runtimes.add(value);
  }
}

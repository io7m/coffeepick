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

import com.io7m.coffeepick.runtime.RuntimeHash;
import com.io7m.junreachable.UnreachableCodeException;
import org.xml.sax.Attributes;
import org.xml.sax.ext.Locator2;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A content handler that can parse hash values.
 */

public final class FormatXML1HashHandler extends FormatXMLAbstractContentHandler<Void, RuntimeHash>
{
  private final RuntimeHash.Builder hash_builder;

  /**
   * Construct a handler.
   *
   * @param locator The XML locator
   */

  public FormatXML1HashHandler(
    final Locator2 locator)
  {
    super(locator, Optional.of("hash"));
    this.hash_builder = RuntimeHash.builder();
  }

  @Override
  protected String onWantHandlerName()
  {
    return FormatXML1HashHandler.class.getSimpleName();
  }

  @Override
  protected Map<String, Supplier<FormatXMLContentHandlerType<Void>>> onWantChildHandlers()
  {
    return Map.of();
  }

  @Override
  protected Optional<RuntimeHash> onElementFinishDirectly(
    final String namespace,
    final String name,
    final String qname)
  {
    return Optional.of(this.hash_builder.build());
  }

  @Override
  protected void onElementStartDirectly(
    final String namespace,
    final String name,
    final String qname,
    final Attributes attributes)
  {
    this.hash_builder.setAlgorithm(attributes.getValue("algorithm"));
    this.hash_builder.setValue(attributes.getValue("value"));
  }

  @Override
  protected void onChildResultReceived(
    final Void value)
  {
    throw new UnreachableCodeException();
  }
}

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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A content handler that simply applies a function to the results of another handler.
 *
 * @param <A> The type of source values
 * @param <B> The type of target values
 */

public final class FormatXMLMappingHandler<A, B> implements FormatXMLContentHandlerType<B>
{
  private final Function<A, B> function;
  private final FormatXMLContentHandlerType<A> handler;

  /**
   * Construct a handler.
   *
   * @param in_handler  The original handler
   * @param in_function The mapping function
   */

  public FormatXMLMappingHandler(
    final FormatXMLContentHandlerType<A> in_handler,
    final Function<A, B> in_function)
  {
    this.handler =
      Objects.requireNonNull(in_handler, "handler");
    this.function =
      Objects.requireNonNull(in_function, "function");
  }

  @Override
  public String toString()
  {
    return new StringBuilder(128)
      .append("[map ")
      .append(this.handler)
      .append(']')
      .toString();
  }

  @Override
  public <C> FormatXMLContentHandlerType<C> map(
    final Function<B, C> f)
  {
    return new FormatXMLMappingHandler<>(this, f);
  }

  @Override
  public void onElementStarted(
    final String namespace,
    final String name,
    final String qname,
    final Attributes attributes)
    throws SAXException
  {
    this.handler.onElementStarted(namespace, name, qname, attributes);
  }

  @Override
  public Optional<B> onElementFinished(
    final String namespace,
    final String name,
    final String qname)
    throws SAXException
  {
    return this.handler.onElementFinished(namespace, name, qname)
      .map(this.function);
  }

  @Override
  public void onCharacters(
    final char[] ch,
    final int start,
    final int length)
    throws SAXException
  {
    this.handler.onCharacters(ch, start, length);
  }

  @Override
  public B get()
  {
    return this.function.apply(this.handler.get());
  }
}

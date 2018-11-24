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

import com.io7m.junreachable.UnreachableCodeException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.Locator2;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An abstract implementation of the content handler interface.
 *
 * @param <A> The type of values returned by child handlers
 * @param <B> The type of result values
 */

public abstract class FormatXMLAbstractContentHandler<A, B> implements FormatXMLContentHandlerType<B>
{
  private final Locator2 locator;
  private final Optional<String> direct;
  private FormatXMLContentHandlerType<A> handler;
  private B result;

  protected FormatXMLAbstractContentHandler(
    final Locator2 in_locator,
    final Optional<String> in_direct)
  {
    this.locator =
      Objects.requireNonNull(in_locator, "locator");
    this.direct =
      Objects.requireNonNull(in_direct, "direct");
  }

  @Override
  public final <C> FormatXMLContentHandlerType<C> map(final Function<B, C> f)
  {
    return new FormatXMLMappingHandler<>(this, f);
  }

  @Override
  public final String toString()
  {
    return new StringBuilder(128)
      .append("[")
      .append(this.onWantHandlerName())
      .append(" ")
      .append(this.direct)
      .append(" [")
      .append(String.join("|", this.onWantChildHandlers().keySet()))
      .append("]]")
      .toString();
  }

  @Override
  public final void onElementStarted(
    final String namespace,
    final String name,
    final String qname,
    final Attributes attributes)
    throws SAXException
  {
    Objects.requireNonNull(namespace, "namespace");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(qname, "qname");
    Objects.requireNonNull(attributes, "attributes");

    final var current = this.handler;
    if (current != null) {
      current.onElementStarted(namespace, name, qname, attributes);
      return;
    }

    final var handler_supplier = this.onWantChildHandlers().get(name);
    if (handler_supplier != null) {
      final var new_handler = handler_supplier.get();
      Objects.requireNonNull(new_handler, "new_handler");
      this.handler = new_handler;
      new_handler.onElementStarted(namespace, name, qname, attributes);
      return;
    }

    if (this.direct.isPresent()) {
      final var direct_name = this.direct.get();
      if (Objects.equals(direct_name, name)) {
        this.onElementStartDirectly(namespace, name, qname, attributes);
        return;
      }
    }

    final var sb = new StringBuilder(128);
    sb.append("This handler does not recognize this element");
    sb.append(System.lineSeparator());
    sb.append("  Received: ");
    sb.append(name);
    sb.append(System.lineSeparator());
    sb.append("  Expected: One of ");
    sb.append(String.join("|", this.onWantChildHandlers().keySet()));
    sb.append(System.lineSeparator());

    if (this.direct.isPresent()) {
      sb.append("  Expected: ");
      sb.append(this.direct.get());
      sb.append(System.lineSeparator());
    }

    throw new SAXParseException(sb.toString(), this.locator);
  }

  private void finishChildHandlerIfNecessary(
    final Optional<A> child_value)
  {
    if (child_value.isPresent()) {
      this.onChildResultReceived(child_value.get());
      this.handler = null;
    }
  }

  protected final Locator2 locator()
  {
    return this.locator;
  }

  protected abstract String onWantHandlerName();

  protected abstract Map<String, Supplier<FormatXMLContentHandlerType<A>>> onWantChildHandlers();

  protected abstract Optional<B> onElementFinishDirectly(
    String namespace,
    String name,
    String qname)
    throws SAXException;

  protected abstract void onElementStartDirectly(
    String namespace,
    String name,
    String qname,
    Attributes attributes)
    throws SAXException;

  /**
   * A value was received from a child handler.
   *
   * @param value The result value
   */

  protected abstract void onChildResultReceived(A value);

  @Override
  public final Optional<B> onElementFinished(
    final String namespace,
    final String name,
    final String qname)
    throws SAXException
  {
    Objects.requireNonNull(namespace, "namespace");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(qname, "qname");

    final var current = this.handler;
    if (current != null) {
      final var sub_result = current.onElementFinished(namespace, name, qname);
      this.finishChildHandlerIfNecessary(sub_result);
      return Optional.empty();
    }

    if (this.direct.isPresent()) {
      final var direct_name = this.direct.get();
      if (Objects.equals(direct_name, name)) {
        final var result_opt = this.onElementFinishDirectly(namespace, name, qname);
        result_opt.ifPresent(this::finish);
        return result_opt;
      }
    }

    throw new UnreachableCodeException();
  }

  protected final void finish(final B r)
  {
    this.result = Objects.requireNonNull(r, "r");
  }

  @Override
  public final void onCharacters(
    final char[] ch,
    final int start,
    final int length)
    throws SAXException
  {
    Objects.requireNonNull(ch, "ch");

    final var current = this.handler;
    if (current != null) {
      current.onCharacters(ch, start, length);
    }

    throw new UnreachableCodeException();
  }

  @Override
  public final B get()
  {
    if (this.result == null) {
      throw new IllegalStateException("Handler has not completed");
    }
    return this.result;
  }
}

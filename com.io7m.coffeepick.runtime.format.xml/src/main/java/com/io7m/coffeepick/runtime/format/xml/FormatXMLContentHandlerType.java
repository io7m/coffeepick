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

import java.util.Optional;
import java.util.function.Function;

/**
 * A content handler that produces values of type {@code A}
 *
 * @param <A> The type of produced values
 */

public interface FormatXMLContentHandlerType<A>
{
  /**
   * Apply {@code f} to the results of this content handler.
   *
   * @param f   The function
   * @param <B> The type of returned values
   *
   * @return A content handler that produces values of type {@code B}
   */

  <B> FormatXMLContentHandlerType<B> map(Function<A, B> f);

  /**
   * An XML element has been started.
   *
   * @param namespace  The namespace URI
   * @param name       The local element name
   * @param qname      The fully qualified name
   * @param attributes The attributes
   *
   * @throws SAXException On errors
   */

  void onElementStarted(
    String namespace,
    String name,
    String qname,
    Attributes attributes)
    throws SAXException;

  /**
   * An XML element has finished.
   *
   * @param namespace The namespace URI
   * @param name      The local element name
   * @param qname     The fully qualified name
   *
   * @return A value of {@code A} if the given element finished the content
   *
   * @throws SAXException On errors
   */

  Optional<A> onElementFinished(
    String namespace,
    String name,
    String qname)
    throws SAXException;

  /**
   * Text was received.
   *
   * @param ch     The character buffer
   * @param start  The offset of the start of the data in the buffer
   * @param length The length of the data in the buffer
   *
   * @throws SAXException On errors
   */

  void onCharacters(
    char[] ch,
    int start,
    int length)
    throws SAXException;

  /**
   * @return The completed value
   */

  A get();
}

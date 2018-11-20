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
import org.xml.sax.SAXException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A content handler for parsing sets of runtimes.
 */

public final class FormatXML1RuntimesHandler
  implements FormatXMLContentHandlerType<List<RuntimeDescription>>
{
  private final URI repository;
  private final ArrayList<RuntimeDescription> runtimes;
  private FormatXMLContentHandlerType<RuntimeDescription> handler;

  /**
   * Construct a handler.
   *
   * @param in_repository The repository URI
   */

  public FormatXML1RuntimesHandler(
    final URI in_repository)
  {
    this.repository =
      Objects.requireNonNull(in_repository, "repository");
    this.runtimes = new ArrayList<>();
  }

  @Override
  public void onElementStarted(
    final String namespace_uri,
    final String local_name,
    final String qualified_name,
    final Attributes attributes)
    throws SAXException
  {
    switch (local_name) {
      case "runtimes": {
        break;
      }
      case "runtime": {
        this.handler = new FormatXML1RuntimeHandler(this.repository);
        this.handler.onElementStarted(namespace_uri, local_name, qualified_name, attributes);
        break;
      }
      default: {
        this.handler.onElementStarted(namespace_uri, local_name, qualified_name, attributes);
        break;
      }
    }
  }

  @Override
  public void onElementFinished(
    final String namespace_uri,
    final String local_name,
    final String qualified_name)
    throws SAXException
  {
    switch (local_name) {
      case "runtimes": {
        break;
      }
      case "runtime": {
        this.handler.onElementFinished(namespace_uri, local_name, qualified_name);
        this.runtimes.add(this.handler.get());
        this.handler = null;
        break;
      }
      default: {
        this.handler.onElementFinished(namespace_uri, local_name, qualified_name);
        break;
      }
    }
  }

  @Override
  public void onCharacters(
    final char[] ch,
    final int start,
    final int length)
    throws SAXException
  {
    this.handler.onCharacters(ch, length, start);
  }

  @Override
  public List<RuntimeDescription> get()
  {
    return this.runtimes;
  }
}

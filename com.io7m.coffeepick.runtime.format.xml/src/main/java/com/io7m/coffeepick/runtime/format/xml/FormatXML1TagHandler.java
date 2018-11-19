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

/**
 * A content handler for parsing tags.
 */

public final class FormatXML1TagHandler
  implements FormatXMLContentHandlerType<String>
{
  private String tag;

  /**
   * Construct a handler.
   */

  public FormatXML1TagHandler()
  {

  }

  @Override
  public void onElementStarted(
    final String namespace_uri,
    final String local_name,
    final String qualified_name,
    final Attributes attributes)
  {
    switch (local_name) {
      case "tag": {
        this.tag = attributes.getValue("name");
        break;
      }
    }
  }

  @Override
  public void onElementFinished(
    final String namespace_uri,
    final String local_name,
    final String qualified_name)
  {

  }

  @Override
  public void onCharacters(
    final char[] ch,
    final int start,
    final int length)
  {

  }

  @Override
  public String get()
  {
    return this.tag;
  }
}

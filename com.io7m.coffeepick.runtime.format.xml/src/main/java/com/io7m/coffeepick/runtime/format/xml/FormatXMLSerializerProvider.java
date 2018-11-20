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

import com.io7m.coffeepick.runtime.parser.spi.FormatDescription;
import com.io7m.coffeepick.runtime.parser.spi.FormatVersion;
import com.io7m.coffeepick.runtime.parser.spi.SerializerProviderType;
import com.io7m.coffeepick.runtime.parser.spi.SerializerType;
import org.osgi.service.component.annotations.Component;

import java.io.OutputStream;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An XML format provider.
 */

@Component(service = SerializerProviderType.class)
public final class FormatXMLSerializerProvider implements SerializerProviderType
{
  private final FormatXMLElements elements;

  /**
   * Construct a provider.
   */

  public FormatXMLSerializerProvider()
  {
    this.elements = new FormatXMLElements();
  }

  @Override
  public FormatDescription serializerFormatSupported()
  {
    return FormatXMLConstants.FORMAT;
  }

  @Override
  public SortedSet<FormatVersion> serializerFormatVersionsSupported()
  {
    return new TreeSet<>(FormatXMLConstants.VERSIONS);
  }

  @Override
  public SerializerType serializerCreate(final OutputStream output)
  {
    return new FormatXMLSerializer(this.elements, output);
  }
}

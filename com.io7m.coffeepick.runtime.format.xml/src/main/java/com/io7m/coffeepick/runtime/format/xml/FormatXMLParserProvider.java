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
import com.io7m.coffeepick.runtime.parser.spi.ParserProviderType;
import com.io7m.coffeepick.runtime.parser.spi.ParserRequest;
import com.io7m.coffeepick.runtime.parser.spi.ParserType;
import com.io7m.jxe.core.JXEHardenedSAXParsers;
import com.io7m.jxe.core.JXEXInclude;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.io7m.coffeepick.runtime.format.xml.FormatXMLConstants.FORMAT;
import static com.io7m.coffeepick.runtime.format.xml.FormatXMLConstants.SCHEMAS;
import static com.io7m.coffeepick.runtime.format.xml.FormatXMLConstants.VERSIONS;

/**
 * An XML format provider.
 */

@Component(service = ParserProviderType.class)
public final class FormatXMLParserProvider implements ParserProviderType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(FormatXMLParserProvider.class);

  private final JXEHardenedSAXParsers parsers;

  /**
   * Construct a provider.
   */

  public FormatXMLParserProvider()
  {
    this.parsers = new JXEHardenedSAXParsers();
  }

  @Override
  public FormatDescription parserFormatSupported()
  {
    return FORMAT;
  }

  @Override
  public SortedSet<FormatVersion> parserFormatVersionsSupported()
  {
    return new TreeSet<>(VERSIONS);
  }

  @Override
  public ParserType parserCreate(
    final ParserRequest request)
    throws IOException
  {
    Objects.requireNonNull(request, "request");

    try {
      return new FormatXMLParser(
        request,
        this.parsers.createXMLReader(Optional.empty(), JXEXInclude.XINCLUDE_DISABLED, SCHEMAS));
    } catch (final ParserConfigurationException | SAXException e) {
      throw new IOException(e);
    }
  }
}

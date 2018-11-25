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

import com.io7m.coffeepick.runtime.parser.spi.ParserResultType;
import com.io7m.coffeepick.runtime.parser.spi.SPIProbeResultType;
import com.io7m.coffeepick.runtime.parser.spi.SPIProbeSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.ext.Locator2;

import java.net.URI;
import java.util.Objects;

import static com.io7m.coffeepick.runtime.format.xml.FormatXMLConstants.SCHEMA_1_0_NAMESPACE;

/**
 * A proving content handler.
 */

public final class FormatXMLProbeHandler extends DefaultHandler2
{
  private static final Logger LOG = LoggerFactory.getLogger(FormatXMLProbeHandler.class);

  private final URI file_uri;
  private Locator2 locator;
  private boolean failed;
  private FormatXMLContentHandlerType<ParserResultType> handler;
  private SPIProbeSuccess result;

  /**
   * Construct a dispatcher.
   *
   * @param in_file_uri The URI of the file being parsed
   */

  public FormatXMLProbeHandler(
    final URI in_file_uri)
  {
    this.file_uri =
      Objects.requireNonNull(in_file_uri, "file_uri");
  }

  /**
   * @return The probe result
   */

  public SPIProbeResultType result()
  {
    return this.result;
  }

  @Override
  public void startElement(
    final String uri,
    final String name,
    final String qname,
    final Attributes attributes)
    throws SAXException
  {
    throw new StoppedParsing();
  }

  @Override
  public void startPrefixMapping(
    final String prefix,
    final String uri)
    throws SAXException
  {
    if (LOG.isTraceEnabled()) {
      LOG.trace("startPrefixMapping: {} {}", prefix, uri);
    }

    if (Objects.equals(uri, SCHEMA_1_0_NAMESPACE.toString())) {
      LOG.debug("using 1.0 schema handler");

      this.result =
        SPIProbeSuccess.builder()
          .setFormat(FormatXMLConstants.FORMAT)
          .setVersion(FormatXMLConstants.VERSION_1_0)
          .build();
      return;
    }

    throw new SAXParseException(
      "Unrecognized schema namespace URI: " + uri,
      null,
      this.file_uri.toString(), 1, 0);
  }

  @Override
  public void setDocumentLocator(
    final Locator in_locator)
  {
    this.locator = (Locator2) Objects.requireNonNull(in_locator, "locator");
  }

  @Override
  public void warning(
    final SAXParseException e)
  {

  }

  @Override
  public void error(
    final SAXParseException e)
    throws SAXParseException
  {
    this.failed = true;
    throw e;
  }

  @Override
  public void fatalError(
    final SAXParseException e)
    throws SAXException
  {
    this.failed = true;
    throw e;
  }

  static final class StoppedParsing extends SAXException
  {
    StoppedParsing()
    {

    }
  }
}

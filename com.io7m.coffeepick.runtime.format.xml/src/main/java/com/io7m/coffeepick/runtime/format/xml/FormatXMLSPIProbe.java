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

import com.io7m.coffeepick.runtime.parser.spi.SPIParserRequest;
import com.io7m.coffeepick.runtime.parser.spi.SPIProbeResultType;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.util.Objects;

/**
 * An XML probe.
 */

public final class FormatXMLSPIProbe
{
  private final SPIParserRequest request;
  private final XMLReader reader;

  /**
   * Construct a parser.
   *
   * @param in_request The parse request
   * @param in_reader  The XML reader
   */

  public FormatXMLSPIProbe(
    final SPIParserRequest in_request,
    final XMLReader in_reader)
  {
    this.request =
      Objects.requireNonNull(in_request, "request");
    this.reader =
      Objects.requireNonNull(in_reader, "reader");
  }

  /**
   * Execute the probe.
   *
   * @return The probe result
   *
   * @throws IOException On I/O errors
   */

  public SPIProbeResultType probe()
    throws IOException
  {
    final var handler = new FormatXMLProbeHandler(this.request.file());
    this.reader.setContentHandler(handler);
    this.reader.setErrorHandler(handler);

    try {
      this.reader.parse(new InputSource(this.request.stream()));
      return handler.result();
    } catch (final FormatXMLProbeHandler.StoppedParsing e) {
      return handler.result();
    } catch (final SAXException e) {
      throw new IOException(e);
    }
  }
}

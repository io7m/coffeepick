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

import com.io7m.coffeepick.runtime.parser.spi.ParseError;
import com.io7m.coffeepick.runtime.parser.spi.ParserFailureException;
import com.io7m.coffeepick.runtime.parser.spi.ParserResultType;
import com.io7m.coffeepick.runtime.parser.spi.SPIParserRequest;
import com.io7m.coffeepick.runtime.parser.spi.SPIParserType;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An XML parser.
 */

public final class FormatXMLSPIParser implements SPIParserType
{
  private final AtomicBoolean closed;
  private final SPIParserRequest request;
  private final XMLReader reader;
  private final PublishSubject<ParseError> events;

  /**
   * Construct a parser.
   *
   * @param in_request The parse request
   * @param in_reader  The XML reader
   */

  public FormatXMLSPIParser(
    final SPIParserRequest in_request,
    final XMLReader in_reader)
  {
    this.request =
      Objects.requireNonNull(in_request, "request");
    this.reader =
      Objects.requireNonNull(in_reader, "reader");
    this.events =
      PublishSubject.create();
    this.closed =
      new AtomicBoolean(false);
  }

  @Override
  public void close()
    throws IOException
  {
    if (this.closed.compareAndSet(false, true)) {
      this.request.stream().close();
      this.events.onComplete();
    }
  }

  @Override
  public Observable<ParseError> errors()
  {
    return this.events;
  }

  @Override
  public ParserResultType parse()
    throws IOException, ParserFailureException
  {
    if (this.closed.get()) {
      throw new IllegalStateException("Parser is closed!");
    }

    final var handler = new FormatXMLVersionedHandlerDispatcher(this.events, this.request.file());
    this.reader.setContentHandler(handler);
    this.reader.setErrorHandler(handler);

    try {
      this.reader.parse(new InputSource(this.request.stream()));
      if (handler.failed()) {
        throw new ParserFailureException("Parsing failed");
      }

      return handler.result();
    } catch (final SAXException e) {
      throw new ParserFailureException(e);
    }
  }
}

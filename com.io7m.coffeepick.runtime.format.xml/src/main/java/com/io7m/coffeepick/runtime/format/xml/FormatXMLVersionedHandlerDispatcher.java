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

import com.io7m.coffeepick.runtime.RuntimeRepositoryDescription;
import com.io7m.coffeepick.runtime.parser.spi.ParseError;
import com.io7m.jlexing.core.LexicalPosition;
import io.reactivex.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DefaultHandler2;

import java.net.URI;
import java.util.Objects;

import static com.io7m.coffeepick.runtime.format.xml.FormatXMLConstants.SCHEMA_1_0_NAMESPACE;
import static com.io7m.coffeepick.runtime.parser.spi.ParseErrorType.Severity.ERROR;
import static com.io7m.coffeepick.runtime.parser.spi.ParseErrorType.Severity.WARNING;

/**
 * An initial content handler responsible for dispatching requests to a version-specific handler.
 */

public final class FormatXMLVersionedHandlerDispatcher extends DefaultHandler2
{
  private static final Logger LOG = LoggerFactory.getLogger(FormatXMLVersionedHandlerDispatcher.class);

  private final URI file_uri;
  private final PublishSubject<ParseError> events;
  private Locator locator;
  private boolean failed;
  private FormatXMLContentHandlerType<RuntimeRepositoryDescription> handler;

  /**
   * Construct a dispatcher.
   *
   * @param in_events   The event receiver
   * @param in_file_uri The URI of the file being parsed
   */

  public FormatXMLVersionedHandlerDispatcher(
    final PublishSubject<ParseError> in_events,
    final URI in_file_uri)
  {
    this.file_uri =
      Objects.requireNonNull(in_file_uri, "file_uri");
    this.events =
      Objects.requireNonNull(in_events, "events");
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
      this.handler = new FormatXML1RepositoryHandler();
      return;
    }

    throw new SAXParseException(
      "Unrecognized schema namespace URI: " + uri,
      null,
      this.file_uri.toString(), 1, 0);
  }

  @Override
  public void startElement(
    final String namespace_uri,
    final String local_name,
    final String qualified_name,
    final Attributes attributes)
    throws SAXException
  {
    LOG.trace("startElement: {} {} {}", namespace_uri, local_name, qualified_name);

    if (this.handler == null) {
      throw new SAXException("No usable namespace found in document");
    }

    if (this.failed) {
      return;
    }

    this.handler.onElementStarted(namespace_uri, local_name, qualified_name, attributes);
  }

  @Override
  public void endElement(
    final String namespace_uri,
    final String local_name,
    final String qualified_name)
    throws SAXException
  {
    LOG.trace("endElement: {} {} {}", namespace_uri, local_name, qualified_name);

    if (this.failed) {
      return;
    }

    this.handler.onElementFinished(namespace_uri, local_name, qualified_name);
  }

  @Override
  public void characters(
    final char[] ch,
    final int start,
    final int length)
    throws SAXException
  {
    if (this.failed) {
      return;
    }

    this.handler.onCharacters(ch, start, length);
  }

  @Override
  public void setDocumentLocator(
    final Locator in_locator)
  {
    this.locator = Objects.requireNonNull(in_locator, "locator");
  }

  @Override
  public void warning(
    final SAXParseException e)
  {
    this.events.onNext(
      ParseError.builder()
        .setException(e)
        .setSeverity(WARNING)
        .setMessage(e.getMessage())
        .setLexical(LexicalPosition.<URI>builder()
                      .setColumn(this.locator.getColumnNumber())
                      .setLine(this.locator.getLineNumber())
                      .setFile(this.file_uri)
                      .build())
        .build());
  }

  @Override
  public void error(
    final SAXParseException e)
  {
    this.failed = true;
    this.events.onNext(
      ParseError.builder()
        .setException(e)
        .setSeverity(ERROR)
        .setMessage(e.getMessage())
        .setLexical(LexicalPosition.<URI>builder()
                      .setColumn(this.locator.getColumnNumber())
                      .setLine(this.locator.getLineNumber())
                      .setFile(this.file_uri)
                      .build())
        .build());
  }

  @Override
  public void fatalError(
    final SAXParseException e)
    throws SAXException
  {
    this.failed = true;
    this.events.onNext(
      ParseError.builder()
        .setException(e)
        .setSeverity(ERROR)
        .setMessage(e.getMessage())
        .setLexical(LexicalPosition.<URI>builder()
                      .setColumn(this.locator.getColumnNumber())
                      .setLine(this.locator.getLineNumber())
                      .setFile(this.file_uri)
                      .build())
        .build());
    throw e;
  }

  /**
   * @return The parsed repository
   */

  public RuntimeRepositoryDescription description()
  {
    return this.handler.get();
  }

  /**
   * @return {@code true} if any parse errors were encountered
   */

  public boolean failed()
  {
    return this.failed;
  }
}

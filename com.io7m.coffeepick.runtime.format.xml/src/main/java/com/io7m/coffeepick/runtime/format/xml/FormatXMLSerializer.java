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
import com.io7m.coffeepick.runtime.parser.spi.SerializerType;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * An XML serializer.
 */

public final class FormatXMLSerializer implements SerializerType
{
  private static final byte[] XML_DECLARATION =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes(UTF_8);

  private final OutputStream output;
  private final AtomicBoolean closed;
  private final FormatXMLElements elements;

  /**
   * Construct a serializer.
   *
   * @param in_elements The element producer
   * @param in_output   The output stream
   */

  public FormatXMLSerializer(
    final FormatXMLElements in_elements,
    final OutputStream in_output)
  {
    this.elements =
      Objects.requireNonNull(in_elements, "elements");
    this.output =
      Objects.requireNonNull(in_output, "output");
    this.closed =
      new AtomicBoolean(false);
  }

  @Override
  public void serialize(
    final RuntimeRepositoryDescription repository)
    throws IOException
  {
    Objects.requireNonNull(repository, "repository");

    try {
      final var document = this.elements.ofRepository(repository);

      final var transformers = TransformerFactory.newInstance();
      transformers.setAttribute("indent-number", Integer.valueOf(4));

      final var transformer = transformers.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

      this.output.write(XML_DECLARATION);
      this.output.write(System.lineSeparator().getBytes(UTF_8));

      final var source = new DOMSource(document);
      final var result = new StreamResult(this.output);
      transformer.transform(source, result);

    } catch (final ParserConfigurationException | TransformerConfigurationException e) {
      throw new IllegalStateException(e);
    } catch (final TransformerException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void close()
    throws IOException
  {
    if (this.closed.compareAndSet(false, true)) {
      this.output.flush();
      this.output.close();
    }
  }
}

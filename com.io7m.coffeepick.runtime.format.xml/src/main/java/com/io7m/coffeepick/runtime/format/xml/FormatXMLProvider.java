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
import com.io7m.jxe.core.JXESchemaDefinition;
import com.io7m.jxe.core.JXESchemaResolutionMappings;
import com.io7m.jxe.core.JXEXInclude;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An XML format provider.
 */

@Component(service = ParserProviderType.class)
public final class FormatXMLProvider implements ParserProviderType
{
  static final URI SCHEMA_1_0_NAMESPACE =
    URI.create("urn:com.io7m.coffeepick:xml:1.0");

  private static final JXESchemaDefinition SCHEMA_1_0 =
    JXESchemaDefinition.builder()
      .setNamespace(SCHEMA_1_0_NAMESPACE)
      .setFileIdentifier("file::schema-1.0.xsd")
      .setLocation(FormatXMLProvider.class.getResource(
        "/com/io7m/coffeepick/runtime/format/xml/schema-1.0.xsd"))
      .build();

  private static final JXESchemaResolutionMappings SCHEMAS =
    JXESchemaResolutionMappings.builder()
      .putMappings(SCHEMA_1_0_NAMESPACE, SCHEMA_1_0)
      .build();

  private static final FormatDescription FORMAT =
    FormatDescription.builder()
      .setDescription("XML format")
      .setMimeType("application/coffeepick+xml")
      .setName(URI.create("urn:com.io7m.coffeepick:xml"))
      .build();

  private static final List<FormatVersion> VERSIONS =
    List.of(
      FormatVersion.builder()
        .setMajor(1)
        .setMinor(0)
        .build());

  private static final Logger LOG =
    LoggerFactory.getLogger(FormatXMLProvider.class);

  private final JXEHardenedSAXParsers parsers;

  /**
   * Construct a provider.
   */

  public FormatXMLProvider()
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

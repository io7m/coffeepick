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
import com.io7m.jxe.core.JXESchemaDefinition;
import com.io7m.jxe.core.JXESchemaResolutionMappings;

import java.net.URI;
import java.util.List;

/**
 * XML constants.
 */

public final class FormatXMLConstants
{
  /**
   * The XML namespace of the version 1.0 schema.
   */

  public static final URI SCHEMA_1_0_NAMESPACE =
    URI.create("urn:com.io7m.coffeepick:xml:1.0");

  /**
   * The version 1.0 schema definition.
   */

  public static final JXESchemaDefinition SCHEMA_1_0 =
    JXESchemaDefinition.builder()
      .setNamespace(SCHEMA_1_0_NAMESPACE)
      .setFileIdentifier("file::schema-1.0.xsd")
      .setLocation(FormatXMLSPISerializerProvider.class.getResource(
        "/com/io7m/coffeepick/runtime/format/xml/schema-1.0.xsd"))
      .build();

  /**
   * The set of schemas supported by this provider.
   */

  public static final JXESchemaResolutionMappings SCHEMAS =
    JXESchemaResolutionMappings.builder()
      .putMappings(SCHEMA_1_0_NAMESPACE, SCHEMA_1_0)
      .build();

  /**
   * The description of the format supported by this provider.
   */

  public static final FormatDescription FORMAT =
    FormatDescription.builder()
      .setDescription("XML format")
      .setMimeType("application/coffeepick+xml")
      .setName(URI.create("urn:com.io7m.coffeepick:xml"))
      .build();

  /**
   * Format version 1.0.
   */

  public static final FormatVersion VERSION_1_0 =
    FormatVersion.builder()
      .setMajor(1)
      .setMinor(0)
      .build();

  /**
   * The format versions supported by this provider.
   */

  public static final List<FormatVersion> VERSIONS =
    List.of(VERSION_1_0);

  private FormatXMLConstants()
  {

  }
}

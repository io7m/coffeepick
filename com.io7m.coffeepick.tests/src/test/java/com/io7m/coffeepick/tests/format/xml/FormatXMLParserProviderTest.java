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

package com.io7m.coffeepick.tests.format.xml;

import com.io7m.coffeepick.runtime.RuntimeConfiguration;
import com.io7m.coffeepick.runtime.RuntimeHash;
import com.io7m.coffeepick.runtime.RuntimeVersions;
import com.io7m.coffeepick.runtime.format.xml.FormatXMLSPIParserProvider;
import com.io7m.coffeepick.runtime.parser.spi.ParseError;
import com.io7m.coffeepick.runtime.parser.spi.ParserFailureException;
import com.io7m.coffeepick.runtime.parser.spi.ParserResultType;
import com.io7m.coffeepick.runtime.parser.spi.SPIParserRequest;
import com.io7m.coffeepick.runtime.parser.spi.SPIParserType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Set;

/**
 * Tests for the XML provider.
 */

public final class FormatXMLParserProviderTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(FormatXMLParserProviderTest.class);

  private static ArrayList<ParseError> logEvents(
    final SPIParserType parser)
  {
    final var events = parser.errors();
    final var event_log = new ArrayList<ParseError>();
    events.subscribe(event -> {
      switch (event.severity()) {
        case WARNING:
          LOG.warn("{}", event);
          break;
        case ERROR:
          LOG.error("{}", event);
          break;
      }
      event_log.add(event);
    });
    return event_log;
  }

  private static InputStream resource(final String name)
    throws IOException
  {
    final var url = FormatXMLParserProviderTest.class.getResource(
      "/com/io7m/coffeepick/tests/" + name);
    if (url == null) {
      throw new FileNotFoundException(name);
    }
    return url.openStream();
  }

  @Test
  public void testValidRuntime()
    throws Exception
  {
    final var provider = new FormatXMLSPIParserProvider();
    final var parser =
      provider.parserCreate(
        SPIParserRequest.builder()
          .setFile(URI.create("urn:file"))
          .setStream(resource("valid-runtime.xml"))
          .build());

    final var event_log = logEvents(parser);
    final var result = parser.parse();

    Assertions.assertTrue(result instanceof ParserResultType.ParsedRuntimeType);
    final var runtime = ((ParserResultType.ParsedRuntimeType) result).runtime();

    Assertions.assertEquals(
      RuntimeConfiguration.JRE, runtime.configuration());
    Assertions.assertEquals(
      "linux", runtime.platform());
    Assertions.assertEquals(
      "x64", runtime.architecture());
    Assertions.assertEquals(
      URI.create("https://www.example.com/jre.tar.gz"), runtime.archiveURI());
    Assertions.assertEquals(
      100L, runtime.archiveSize());
    Assertions.assertEquals(
      RuntimeVersions.parse("11"), runtime.version());
    Assertions.assertEquals(
      "hotspot", runtime.vm());
    Assertions.assertEquals(
      Set.of("large-heap", "production"), runtime.tags());
    Assertions.assertEquals(
      RuntimeHash.builder()
        .setAlgorithm("SHA-256")
        .setValue("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824")
        .build(),
      runtime.archiveHash());

    Assertions.assertTrue(runtime.build().isPresent());
    Assertions.assertEquals(
      OffsetDateTime.parse("2018-01-01T00:00:00+00:00"),
      runtime.build().get().time());
    Assertions.assertEquals(
      "b23",
      runtime.build().get().buildNumber());

    Assertions.assertEquals(0L, (long) event_log.size());
  }

  @Test
  public void testValidRepository()
    throws Exception
  {
    final var provider = new FormatXMLSPIParserProvider();
    final var parser =
      provider.parserCreate(
        SPIParserRequest.builder()
          .setFile(URI.create("urn:file"))
          .setStream(resource("valid-repos.xml"))
          .build());

    final var event_log = logEvents(parser);
    final var result = parser.parse();

    Assertions.assertTrue(result instanceof ParserResultType.ParsedRepositoryType);
    final var repository = ((ParserResultType.ParsedRepositoryType) result).repository();

    Assertions.assertEquals(
      URI.create("urn:com.io7m.coffeepick.example"),
      repository.id());
    Assertions.assertEquals(
      OffsetDateTime.parse("2018-01-01T00:00:00+00:00"),
      repository.updated().get());

    final var runtimes = repository.runtimes();
    Assertions.assertEquals(2L, (long) runtimes.size());
    Assertions.assertTrue(runtimes.containsKey(
      "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"));

    {
      final var runtime =
        runtimes.get("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");

      Assertions.assertEquals(
        RuntimeConfiguration.JRE, runtime.configuration());
      Assertions.assertEquals(
        "linux", runtime.platform());
      Assertions.assertEquals(
        "x64", runtime.architecture());
      Assertions.assertEquals(
        URI.create("https://www.example.com/jre.tar.gz"), runtime.archiveURI());
      Assertions.assertEquals(
        100L, runtime.archiveSize());

      Assertions.assertTrue(runtime.build().isPresent());
      Assertions.assertEquals(
        OffsetDateTime.parse("2018-01-01T00:00:00+00:00"),
        runtime.build().get().time());
      Assertions.assertEquals(
        "b23",
        runtime.build().get().buildNumber());

      Assertions.assertEquals(
        RuntimeVersions.parse("11"), runtime.version());
      Assertions.assertEquals(
        "hotspot", runtime.vm());
      Assertions.assertEquals(
        Set.of("large-heap", "production"), runtime.tags());
      Assertions.assertEquals(
        RuntimeHash.builder()
          .setAlgorithm("SHA-256")
          .setValue("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824")
          .build(),
        runtime.archiveHash());
    }

    {
      final var runtime =
        runtimes.get("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9823");

      Assertions.assertEquals(
        RuntimeConfiguration.JDK, runtime.configuration());
      Assertions.assertEquals(
        "windows", runtime.platform());
      Assertions.assertEquals(
        "x32", runtime.architecture());
      Assertions.assertEquals(
        URI.create("https://www.example.com/jre2.tar.gz"), runtime.archiveURI());
      Assertions.assertEquals(
        200L, runtime.archiveSize());
      Assertions.assertEquals(
        RuntimeVersions.parse("11.0.1"), runtime.version());
      Assertions.assertEquals(
        "openj9", runtime.vm());
      Assertions.assertEquals(
        Set.of("production"), runtime.tags());
      Assertions.assertEquals(
        RuntimeHash.builder()
          .setAlgorithm("SHA-256")
          .setValue("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9823")
          .build(),
        runtime.archiveHash());
    }

    Assertions.assertEquals(0L, (long) event_log.size());
  }

  @Test
  public void testInvalidNoNamespace()
    throws Exception
  {
    final var provider = new FormatXMLSPIParserProvider();
    final var parser =
      provider.parserCreate(
        SPIParserRequest.builder()
          .setFile(URI.create("urn:file"))
          .setStream(resource("invalid-no-namespace.xml"))
          .build());

    final var event_log = logEvents(parser);
    Assertions.assertThrows(ParserFailureException.class, parser::parse);
    Assertions.assertEquals(1L, (long) event_log.size());
  }

  @Test
  public void testInvalidWrongNamespace()
    throws Exception
  {
    final var provider = new FormatXMLSPIParserProvider();
    final var parser =
      provider.parserCreate(
        SPIParserRequest.builder()
          .setFile(URI.create("urn:file"))
          .setStream(resource("invalid-wrong-namespace.xml"))
          .build());

    final var event_log = logEvents(parser);
    Assertions.assertThrows(ParserFailureException.class, parser::parse);
    Assertions.assertEquals(1L, (long) event_log.size());
  }

  @Test
  public void testInvalidUnexpectedRoot()
    throws Exception
  {
    final var provider = new FormatXMLSPIParserProvider();
    final var parser =
      provider.parserCreate(
        SPIParserRequest.builder()
          .setFile(URI.create("urn:file"))
          .setStream(resource("invalid-unexpected-root.xml"))
          .build());

    final var event_log = logEvents(parser);
    Assertions.assertThrows(ParserFailureException.class, parser::parse);
    Assertions.assertEquals(0L, (long) event_log.size());
  }
}

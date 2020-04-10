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

import com.io7m.coffeepick.runtime.RuntimeBuild;
import com.io7m.coffeepick.runtime.RuntimeConfiguration;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimeHash;
import com.io7m.coffeepick.runtime.RuntimeRepositoryBranding;
import com.io7m.coffeepick.runtime.RuntimeRepositoryDescription;
import com.io7m.coffeepick.runtime.RuntimeVersions;
import com.io7m.coffeepick.runtime.format.xml.FormatXMLElements;
import com.io7m.coffeepick.runtime.format.xml.FormatXMLSPIParserProvider;
import com.io7m.coffeepick.runtime.parser.spi.ParsedRepository;
import com.io7m.coffeepick.runtime.parser.spi.ParsedRuntime;
import com.io7m.coffeepick.runtime.parser.spi.ParserResultType;
import com.io7m.coffeepick.runtime.parser.spi.SPIParserRequest;
import com.io7m.junreachable.UnreachableCodeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Tests for the XML tree creation functions.
 */

public final class FormatXMLElementsTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(FormatXMLElementsTest.class);

  private static ParserResultType dumpAndValidate(
    final Document document)
    throws Exception
  {
    try (var output = new ByteArrayOutputStream()) {
      final var transformers = TransformerFactory.newInstance();
      transformers.setAttribute("indent-number", Integer.valueOf(4));

      final var transformer = transformers.newTransformer();
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(
        "{http://xml.apache.org/xslt}indent-amount",
        "5");

      final var source = new DOMSource(document);
      final var result = new StreamResult(output);
      transformer.transform(source, result);

      final var text = output.toString(UTF_8);
      LOG.debug("document:\n{}", text);

      final var provider = new FormatXMLSPIParserProvider();
      try (var parser = provider.parserCreate(
        SPIParserRequest.builder()
          .setStream(new ByteArrayInputStream(output.toByteArray()))
          .setFile(URI.create("urn:input"))
          .build())) {

        parser.errors().forEach(event -> LOG.error("{}", event));
        return parser.parse();
      }
    }
  }

  public static void dumpAndValidateRepository(
    final Document document,
    final RuntimeRepositoryDescription repository)
    throws Exception
  {
    final var received = dumpAndValidate(document);
    switch (received.kind()) {
      case REPOSITORY: {
        final var parsed_repository = (ParsedRepository) received;
        Assertions.assertEquals(repository, parsed_repository.repository());
        break;
      }
      case RUNTIME: {
        throw new UnreachableCodeException();
      }
    }
  }

  public static void dumpAndValidateRuntime(
    final Document document,
    final RuntimeDescription runtime)
    throws Exception
  {
    final var received = dumpAndValidate(document);
    switch (received.kind()) {
      case REPOSITORY: {
        throw new UnreachableCodeException();
      }
      case RUNTIME: {
        final var parsed_runtime = (ParsedRuntime) received;
        Assertions.assertEquals(runtime, parsed_runtime.runtime());
      }
    }
  }

  @Test
  public void testEmptyRepository()
    throws Exception
  {
    final var repository =
      RuntimeRepositoryDescription.builder()
        .setBranding(
          RuntimeRepositoryBranding.builder()
            .setLogo(URI.create("https://www.example.com/logo.png"))
            .setSite(URI.create("https://www.example.com/"))
            .setSubtitle("Subtitle")
            .setTitle("Title")
            .build()
        )
        .setUpdated(OffsetDateTime.now(ZoneId.of("UTC")))
        .setId(URI.create("urn:repository"))
        .build();

    final var document =
      new FormatXMLElements()
        .ofRepository(repository);

    dumpAndValidateRepository(document, repository);
  }

  @Test
  public void testSimpleRepository()
    throws Exception
  {
    final var runtime_0 =
      RuntimeDescription.builder()
        .setBuild(
          RuntimeBuild.builder()
            .setBuildNumber("b23")
            .setTime(OffsetDateTime.parse("2018-01-01T00:00:00+00:00"))
            .build())
        .setConfiguration(RuntimeConfiguration.JDK)
        .setRepository(URI.create("urn:repository"))
        .setVersion(RuntimeVersions.parse("11.0.0"))
        .setArchiveHash(RuntimeHash.of("SHA-256", "abcd"))
        .setArchitecture("x64")
        .setPlatform("linux")
        .setArchiveURI(URI.create("http://example.com/a.tar.gz"))
        .setArchiveSize(100L)
        .setVm("hotspot")
        .addTags("production")
        .build();

    final var runtime_1 =
      RuntimeDescription.builder()
        .setConfiguration(RuntimeConfiguration.JDK)
        .setRepository(URI.create("urn:repository"))
        .setVersion(RuntimeVersions.parse("11.0.0"))
        .setArchiveHash(RuntimeHash.of("SHA-256", "bdec"))
        .setArchitecture("x64")
        .setPlatform("linux")
        .setArchiveURI(URI.create("http://example.com/b.tar.gz"))
        .setArchiveSize(100L)
        .setVm("hotspot")
        .addTags("tag0")
        .build();

    final var runtime_2 =
      RuntimeDescription.builder()
        .setConfiguration(RuntimeConfiguration.JDK)
        .setRepository(URI.create("urn:repository"))
        .setVersion(RuntimeVersions.parse("11.0.0"))
        .setArchiveHash(RuntimeHash.of("SHA-256", "ffff"))
        .setArchitecture("x64")
        .setPlatform("linux")
        .setArchiveURI(URI.create("http://example.com/c.tar.gz"))
        .setArchiveSize(100L)
        .setVm("hotspot")
        .addTags("tag1")
        .addTags("tag2")
        .addTags("tag3")
        .build();

    final var repository =
      RuntimeRepositoryDescription.builder()
        .setBranding(
          RuntimeRepositoryBranding.builder()
            .setLogo(URI.create("https://www.example.com/logo.png"))
            .setSite(URI.create("https://www.example.com/"))
            .setSubtitle("Subtitle")
            .setTitle("Title")
            .build()
        )
        .setUpdated(OffsetDateTime.now(ZoneId.of("UTC")))
        .setId(URI.create("urn:repository"))
        .putRuntimes(runtime_0.id(), runtime_0)
        .putRuntimes(runtime_1.id(), runtime_1)
        .putRuntimes(runtime_2.id(), runtime_2)
        .build();

    final var document =
      new FormatXMLElements()
        .ofRepository(repository);

    dumpAndValidateRepository(document, repository);
  }

  @Test
  public void testSimpleRuntime()
    throws Exception
  {
    final var runtime =
      RuntimeDescription.builder()
        .setBuild(
          RuntimeBuild.builder()
            .setBuildNumber("b23")
            .setTime(OffsetDateTime.parse("2018-01-01T00:00:00+00:00"))
            .build())
        .setConfiguration(RuntimeConfiguration.JDK)
        .setRepository(URI.create("urn:repository"))
        .setVersion(RuntimeVersions.parse("11.0.0"))
        .setArchiveHash(RuntimeHash.of("SHA-256", "abcd"))
        .setArchitecture("x64")
        .setPlatform("linux")
        .setArchiveURI(URI.create("http://example.com/a.tar.gz"))
        .setArchiveSize(100L)
        .setVm("hotspot")
        .addTags("production")
        .build();

    final var document =
      new FormatXMLElements()
        .document();
    final var runtime_element =
      FormatXMLElements.ofRuntime(document, runtime, true);

    document.appendChild(runtime_element);
    dumpAndValidateRuntime(document, runtime);
  }
}

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

package com.io7m.coffeepick.tests.parser.api;

import com.io7m.coffeepick.runtime.format.xml.FormatXMLSPIParserProvider;
import com.io7m.coffeepick.runtime.parser.api.CoffeePickParseRequest;
import com.io7m.coffeepick.runtime.parser.api.CoffeePickParsers;
import com.io7m.coffeepick.runtime.parser.spi.FormatDescription;
import com.io7m.coffeepick.runtime.parser.spi.FormatVersion;
import com.io7m.coffeepick.runtime.parser.spi.ParsedRepository;
import com.io7m.coffeepick.runtime.parser.spi.ParsedRuntime;
import com.io7m.coffeepick.runtime.parser.spi.ParserFailureException;
import com.io7m.coffeepick.runtime.parser.spi.SPIParserProviderType;
import com.io7m.coffeepick.runtime.parser.spi.SPIParserType;
import com.io7m.coffeepick.runtime.parser.spi.SPIProbeSuccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class CoffeePickParsersTest
{
  private static InputStream resource(final String name)
    throws IOException
  {
    final var url = CoffeePickParsersTest.class.getResource(
      "/com/io7m/coffeepick/tests/" + name);
    if (url == null) {
      throw new FileNotFoundException(name);
    }
    return url.openStream();
  }

  /**
   * If no providers are available, trying to create a parser fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNoProviders()
    throws Exception
  {
    final var parsers = CoffeePickParsers.createFrom(List.of());

    try (var stream = resource("valid-repos.xml")) {
      final var request =
        CoffeePickParseRequest.builder()
          .setUri(URI.create("file:valid-repos.xml"))
          .setStream(stream)
          .build();

      Assertions.assertThrows(ParserFailureException.class, () -> parsers.createParser(request));
    }
  }

  /**
   * The provider that claims to support the highest version is chosen over other providers.
   *
   * @throws Exception On errors
   */

  @Test
  public void testProviderWithHighestVersion()
    throws Exception
  {
    final var provider_0 =
      Mockito.mock(SPIParserProviderType.class);
    final var provider_1 =
      Mockito.mock(SPIParserProviderType.class);
    final var provider_2 =
      Mockito.mock(SPIParserProviderType.class);
    final var expected_parser =
      Mockito.mock(SPIParserType.class);

    Mockito.when(provider_0.parserFormatVersionsSupported())
      .thenReturn(new TreeSet<>(Set.of(
        FormatVersion.of(1, 0),
        FormatVersion.of(1, 1),
        FormatVersion.of(1, 2)
      )));

    Mockito.when(provider_0.toString())
      .thenReturn("Provider 0");

    Mockito.when(provider_0.probe(Mockito.any()))
      .thenReturn(
        SPIProbeSuccess.builder()
          .setVersion(FormatVersion.of(1, 2))
          .setFormat(
            FormatDescription.builder()
              .setName(URI.create("urn:provider0"))
              .setMimeType("application/xml")
              .setDescription("Provider 0")
              .build())
          .build());

    Mockito.when(provider_1.parserFormatVersionsSupported())
      .thenReturn(new TreeSet<>(Set.of(
        FormatVersion.of(1, 0),
        FormatVersion.of(1, 1),
        FormatVersion.of(2, 0)
      )));

    Mockito.when(provider_1.parserName())
      .thenReturn("Provider 1");

    Mockito.when(provider_1.toString())
      .thenReturn("Provider 1");

    Mockito.when(provider_1.probe(Mockito.any()))
      .thenReturn(
        SPIProbeSuccess.builder()
          .setVersion(FormatVersion.of(2, 0))
          .setFormat(
            FormatDescription.builder()
              .setName(URI.create("urn:provider1"))
              .setMimeType("application/xml")
              .setDescription("Provider 1")
              .build())
          .build());

    Mockito.when(provider_1.parserCreate(Mockito.any()))
      .thenReturn(expected_parser);

    Mockito.when(provider_2.parserFormatVersionsSupported())
      .thenReturn(new TreeSet<>(Set.of(
        FormatVersion.of(0, 0),
        FormatVersion.of(0, 1),
        FormatVersion.of(0, 2)
      )));

    Mockito.when(provider_2.toString())
      .thenReturn("Provider 2");

    Mockito.when(provider_2.probe(Mockito.any()))
      .thenReturn(
        SPIProbeSuccess.builder()
          .setVersion(FormatVersion.of(0, 2))
          .setFormat(
            FormatDescription.builder()
              .setName(URI.create("urn:provider2"))
              .setMimeType("application/xml")
              .setDescription("Provider 2")
              .build())
          .build());

    final var parsers =
      CoffeePickParsers.createFrom(List.of(provider_0, provider_1, provider_2));

    try (var stream = resource("valid-repos.xml")) {
      final var request =
        CoffeePickParseRequest.builder()
          .setUri(URI.create("file:valid-repos.xml"))
          .setStream(stream)
          .build();

      try (final var created_parser = parsers.createParser(request)) {
        Assertions.assertSame("Provider 1", created_parser.provider());
      }
    }
  }

  /**
   * The XML provider works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testXMLProviderRepository()
    throws Exception
  {
    final var parsers =
      CoffeePickParsers.createFrom(List.of(new FormatXMLSPIParserProvider()));

    try (var stream = resource("valid-repos.xml")) {
      final var request =
        CoffeePickParseRequest.builder()
          .setUri(URI.create("file:valid-repos.xml"))
          .setStream(stream)
          .build();

      try (final var created_parser = parsers.createParser(request)) {
        final var result = created_parser.parse();
        Assertions.assertTrue(result instanceof ParsedRepository);
      }
    }
  }

  /**
   * The XML provider works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testXMLProviderRuntime()
    throws Exception
  {
    final var parsers =
      CoffeePickParsers.createFrom(List.of(new FormatXMLSPIParserProvider()));

    try (var stream = resource("valid-runtime.xml")) {
      final var request =
        CoffeePickParseRequest.builder()
          .setUri(URI.create("file:valid-runtime.xml"))
          .setStream(stream)
          .build();

      try (final var created_parser = parsers.createParser(request)) {
        final var result = created_parser.parse();
        Assertions.assertTrue(result instanceof ParsedRuntime);
      }
    }
  }
}

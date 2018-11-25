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

package com.io7m.coffeepick.tests.parser.spi;

import com.io7m.coffeepick.runtime.parser.spi.SPIParserProviderType;
import com.io7m.coffeepick.runtime.parser.spi.SPISerializerProviderType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public final class FormatProvidersTest
{
  @Test
  public void testParserProvidersAreAvailable()
  {
    final var providers =
      ServiceLoader.load(SPIParserProviderType.class)
        .stream()
        .map(ServiceLoader.Provider::get)
        .collect(Collectors.toList());

    Assertions.assertTrue(
      providers.stream()
        .anyMatch(p -> {
          final var mime = p.parserFormatSupported().mimeType();
          return Objects.equals(mime, "application/coffeepick+xml");
        }),
      "XML available");
  }

  @Test
  public void testSerializerProvidersAreAvailable()
  {
    final var providers =
      ServiceLoader.load(SPISerializerProviderType.class)
        .stream()
        .map(ServiceLoader.Provider::get)
        .collect(Collectors.toList());

    Assertions.assertTrue(
      providers.stream()
        .anyMatch(p -> {
          final var mime = p.serializerFormatSupported().mimeType();
          return Objects.equals(mime, "application/coffeepick+xml");
        }),
      "XML available");
  }
}

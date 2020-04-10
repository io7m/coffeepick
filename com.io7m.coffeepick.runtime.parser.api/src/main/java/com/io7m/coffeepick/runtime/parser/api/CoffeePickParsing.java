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

package com.io7m.coffeepick.runtime.parser.api;

import com.io7m.coffeepick.runtime.parser.spi.ParserFailureException;
import com.io7m.coffeepick.runtime.parser.spi.SPIParserProviderType;
import com.io7m.coffeepick.runtime.parser.spi.SPIParserRequest;
import com.io7m.coffeepick.runtime.parser.spi.SPIProbeFailure;
import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

final class CoffeePickParsing
{
  private static final Logger LOG = LoggerFactory.getLogger(CoffeePickParsers.class);
  private static final int BUFFER_SIZE = 4096;

  private CoffeePickParsing()
  {

  }

  private static SPIParserProviderType findCandidateSupportingHighestVersion(
    final List<SPIParserProviderType> candidates)
  {
    return candidates.stream()
      .max(Comparator.comparing(provider -> provider.parserFormatVersionsSupported().last()))
      .get();
  }

  private static boolean canSupportStream(
    final SPIParserProviderType provider,
    final URI stream_name,
    final byte[] data)
  {
    try (var stream = new ByteArrayInputStream(data)) {
      final var probe_request =
        SPIParserRequest.builder()
          .setFile(stream_name)
          .setStream(stream)
          .build();

      final var result = provider.probe(probe_request);
      switch (result.kind()) {
        case PROBE_SUCCESS: {
          return true;
        }
        case PROBE_FAILURE: {
          final var failure = (SPIProbeFailure) result;
          final var name = provider.getClass().getCanonicalName();
          final var error_opt = failure.error();
          if (error_opt.isPresent()) {
            LOG.debug(
              "provider {} cannot support stream {}: ", name, stream_name, error_opt.get());
          } else {
            LOG.debug(
              "provider {} cannot support stream {}: (no exception logged)", name, stream_name);
          }
          return false;
        }
      }

      throw new UnreachableCodeException();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static CoffeePickParserType createParserProbing(
    final List<SPIParserProviderType> available_providers,
    final CoffeePickParseRequest request)
    throws IOException, ParserFailureException
  {
    /*
     * Read a buffer of the start of the stream and then rewind it to the start. This buffer
     * of data is used by parser providers to probe format information.
     */

    final var stream = new BufferedInputStream(request.stream(), BUFFER_SIZE);
    stream.mark(BUFFER_SIZE);

    final var buffer = stream.readNBytes(BUFFER_SIZE);
    stream.reset();

    final var stream_name = request.uri();
    final var candidates =
      available_providers.stream()
        .filter(provider -> canSupportStream(provider, stream_name, buffer))
        .collect(Collectors.toList());

    if (candidates.isEmpty()) {
      throw noSuitableProviders(available_providers, stream_name);
    }

    /*
     * Find the "best" available provider.
     */

    final var selection =
      findCandidateSupportingHighestVersion(candidates);

    final var spi_request =
      SPIParserRequest.builder()
        .setStream(stream)
        .setFile(request.uri())
        .build();

    return new CoffeePickParser(
      selection.parserName(),
      selection.parserCreate(spi_request));
  }

  private static ParserFailureException noSuitableProviders(
    final List<SPIParserProviderType> providers,
    final URI stream_name)
  {
    final var builder =
      new StringBuilder(128)
        .append("No suitable parser implementation available to handle stream.")
        .append(System.lineSeparator())
        .append("  Stream: ")
        .append(stream_name)
        .append(System.lineSeparator())
        .append("  Providers:")
        .append(System.lineSeparator());

    providers.forEach(provider -> {
      builder.append("    ");
      builder.append(provider.getClass().getCanonicalName());
      builder.append(System.lineSeparator());
    });

    return new ParserFailureException(builder.toString());
  }
}

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

import com.io7m.coffeepick.runtime.RuntimeRepositoryDescription;
import com.io7m.coffeepick.runtime.parser.spi.FormatDescription;
import com.io7m.coffeepick.runtime.parser.spi.FormatVersion;
import com.io7m.coffeepick.runtime.parser.spi.SPISerializerProviderType;
import com.io7m.coffeepick.runtime.parser.spi.SPISerializerType;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

final class CoffeePickSerializing
{
  private CoffeePickSerializing()
  {

  }

  static CoffeePickSerializerType createSerializer(
    final List<SPISerializerProviderType> providers,
    final FormatDescription format,
    final FormatVersion version,
    final OutputStream output)
    throws IOException
  {
    final var provider_opt =
      providers.stream()
        .filter(provider -> isSuitableProvider(provider, format, version))
        .findFirst();

    return new Serializer(provider_opt.orElseThrow(
      () -> new IOException(
        new StringBuilder(128)
          .append("No suitable format provider.")
          .append(System.lineSeparator())
          .append("  Format: ")
          .append(format.name())
          .append(' ')
          .append(format.description())
          .append(System.lineSeparator())
          .toString())).serializerCreate(output));
  }

  private static boolean isSuitableProvider(
    final SPISerializerProviderType provider,
    final FormatDescription format,
    final FormatVersion version)
  {
    return Objects.equals(provider.serializerFormatSupported(), format)
      && provider.serializerFormatVersionsSupported().contains(version);
  }

  static SortedSet<FormatVersion> findSupportedVersions(
    final List<SPISerializerProviderType> providers,
    final FormatDescription format)
  {
    final var versions = new TreeSet<FormatVersion>();
    for (final var provider : providers) {
      if (Objects.equals(provider.serializerFormatSupported(), format)) {
        versions.addAll(provider.serializerFormatVersionsSupported());
      }
    }
    return versions;
  }

  static Optional<FormatDescription> findFormat(
    final List<SPISerializerProviderType> providers,
    final URI format)
  {
    for (final var provider : providers) {
      final var description = provider.serializerFormatSupported();
      if (Objects.equals(description.name(), format)) {
        return Optional.of(description);
      }
    }
    return Optional.empty();
  }

  private static final class Serializer implements CoffeePickSerializerType
  {
    private final SPISerializerType serializer;
    private final AtomicBoolean closed;

    Serializer(final SPISerializerType in_serializer)
    {
      this.serializer = Objects.requireNonNull(in_serializer, "serializer");
      this.closed = new AtomicBoolean(false);
    }

    @Override
    public void serialize(
      final RuntimeRepositoryDescription repository)
      throws IOException
    {
      Objects.requireNonNull(repository, "repository");

      if (this.closed.get()) {
        throw new IllegalStateException("Serializer is closed");
      }
      this.serializer.serialize(repository);
    }

    @Override
    public void close()
      throws IOException
    {
      if (this.closed.compareAndSet(false, true)) {
        this.serializer.close();
      }
    }
  }
}

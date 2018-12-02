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

import com.io7m.coffeepick.runtime.parser.spi.FormatDescription;
import com.io7m.coffeepick.runtime.parser.spi.FormatVersion;
import com.io7m.coffeepick.runtime.parser.spi.SPISerializerProviderType;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.SortedSet;
import java.util.stream.Collectors;

/**
 * The default serializers implementation.
 */

public final class CoffeePickSerializers implements CoffeePickSerializersType
{
  private final List<SPISerializerProviderType> providers;

  private CoffeePickSerializers(
    final List<SPISerializerProviderType> in_providers)
  {
    this.providers = List.copyOf(Objects.requireNonNull(in_providers, "providers"));
  }

  /**
   * Create a new parser provider, loading parsers via {@link ServiceLoader}.
   *
   * @return A new parser provider
   */

  public static CoffeePickSerializersType createFromServiceLoader()
  {
    return createFrom(
      ServiceLoader.load(SPISerializerProviderType.class)
        .stream()
        .map(ServiceLoader.Provider::get)
        .collect(Collectors.toUnmodifiableList()));
  }

  /**
   * Create a new parser provider, using parsers from the given list of providers.
   *
   * @param in_providers The list of providers
   *
   * @return A new parser provider
   */

  public static CoffeePickSerializersType createFrom(
    final List<SPISerializerProviderType> in_providers)
  {
    return new CoffeePickSerializers(in_providers);
  }

  @Override
  public SortedSet<FormatVersion> findSupportedVersions(
    final FormatDescription format)
  {
    Objects.requireNonNull(format, "format");
    return CoffeePickSerializing.findSupportedVersions(this.providers, format);
  }

  @Override
  public Optional<FormatDescription> findFormat(final URI format)
  {
    Objects.requireNonNull(format, "format");
    return CoffeePickSerializing.findFormat(this.providers, format);
  }

  @Override
  public CoffeePickSerializerType createSerializer(
    final FormatDescription format,
    final FormatVersion version,
    final OutputStream output)
    throws IOException
  {
    Objects.requireNonNull(format, "format");
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(output, "output");
    return CoffeePickSerializing.createSerializer(this.providers, format, version, output);
  }
}

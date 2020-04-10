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
import net.jcip.annotations.GuardedBy;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;

/**
 * The OSGi implementation of the {@link CoffeePickSerializersType} interface.
 */

@Component(service = CoffeePickSerializersType.class)
public final class CoffeePickSerializersOSGi implements CoffeePickSerializersType
{
  private final Object provider_lock;
  @GuardedBy("provider_lock")
  private final ArrayList<SPISerializerProviderType> providers;

  /**
   * Construct a parser provider.
   */

  public CoffeePickSerializersOSGi()
  {
    this.providers = new ArrayList<>(32);
    this.provider_lock = new Object();
  }

  /**
   * A serializer provider has become available.
   *
   * @param provider The provider
   */

  @Reference(
    cardinality = ReferenceCardinality.MULTIPLE,
    policy = ReferencePolicy.DYNAMIC,
    policyOption = ReferencePolicyOption.GREEDY,
    unbind = "onSerializerProviderUnavailable")
  public void onSerializerProviderAvailable(
    final SPISerializerProviderType provider)
  {
    synchronized (this.provider_lock) {
      this.providers.add(Objects.requireNonNull(provider, "provider"));
    }
  }

  /**
   * A serializer provider has become unavailable.
   *
   * @param provider The provider
   */

  public void onSerializerProviderUnavailable(
    final SPISerializerProviderType provider)
  {
    synchronized (this.provider_lock) {
      this.providers.remove(provider);
    }
  }

  @Override
  public SortedSet<FormatVersion> findSupportedVersions(
    final FormatDescription format)
  {
    Objects.requireNonNull(format, "format");
    return CoffeePickSerializing.findSupportedVersions(
      this.availableProvidersSnapshot(), format);
  }

  @Override
  public Optional<FormatDescription> findFormat(
    final URI format)
  {
    Objects.requireNonNull(format, "format");
    return CoffeePickSerializing.findFormat(
      this.availableProvidersSnapshot(), format);
  }

  private List<SPISerializerProviderType> availableProvidersSnapshot()
  {
    final List<SPISerializerProviderType> available;
    synchronized (this.provider_lock) {
      available = List.copyOf(this.providers);
    }
    return available;
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

    return CoffeePickSerializing.createSerializer(
      this.availableProvidersSnapshot(), format, version, output);
  }
}

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
import net.jcip.annotations.GuardedBy;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The OSGi implementation of the {@link CoffeePickParsersType} interface.
 */

@Component(service = CoffeePickParsersType.class)
public final class CoffeePickParsersOSGi implements CoffeePickParsersType
{
  private final Object provider_lock;
  @GuardedBy("provider_lock")
  private final ArrayList<SPIParserProviderType> providers;

  /**
   * Construct a parser provider.
   */

  public CoffeePickParsersOSGi()
  {
    this.providers = new ArrayList<>(32);
    this.provider_lock = new Object();
  }

  /**
   * A parser provider has become available.
   *
   * @param provider The provider
   */

  @Reference(
    cardinality = ReferenceCardinality.MULTIPLE,
    policy = ReferencePolicy.DYNAMIC,
    policyOption = ReferencePolicyOption.GREEDY,
    unbind = "onParserProviderUnavailable")
  public void onParserProviderAvailable(
    final SPIParserProviderType provider)
  {
    synchronized (this.provider_lock) {
      this.providers.add(Objects.requireNonNull(provider, "provider"));
    }
  }

  /**
   * A parser provider has become unavailable.
   *
   * @param provider The provider
   */

  public void onParserProviderUnavailable(
    final SPIParserProviderType provider)
  {
    synchronized (this.provider_lock) {
      this.providers.remove(provider);
    }
  }

  @Override
  public CoffeePickParserType createParser(
    final CoffeePickParseRequest request)
    throws IOException, ParserFailureException
  {
    Objects.requireNonNull(request, "request");

    final List<SPIParserProviderType> available;
    synchronized (this.provider_lock) {
      available = List.copyOf(this.providers);
    }

    return CoffeePickParsing.createParserProbing(available, request);
  }
}

/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> http://io7m.com
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

package com.io7m.coffeepick.adoptopenjdk_v3;

import com.io7m.coffeepick.repository.spi.RuntimeRepositoryContextType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryType;
import net.adoptopenjdk.v3.api.AOV3ClientProviderType;
import net.adoptopenjdk.v3.vanilla.AOV3Clients;
import org.osgi.service.component.annotations.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

@Component(service = RuntimeRepositoryProviderType.class)
public final class AOJRepositoryProvider implements
  RuntimeRepositoryProviderType
{
  /**
   * The URI of the provider.
   */

  public static final URI PROVIDER_URI =
    URI.create("urn:net.adoptopenjdk");

  /**
   * The name of the provider.
   */

  public static final String PROVIDER_NAME =
    "net.adoptopenjdk";

  private final AOV3ClientProviderType clients;

  /**
   * Construct a provider.
   */

  public AOJRepositoryProvider()
  {
    this(new AOV3Clients());
  }

  /**
   * Construct a provider.
   *
   * @param inClients A provider of AdoptOpenJDK clients
   */

  public AOJRepositoryProvider(
    final AOV3ClientProviderType inClients)
  {
    this.clients = Objects.requireNonNull(inClients, "clients");
  }

  @Override
  public URI uri()
  {
    return PROVIDER_URI;
  }

  @Override
  public String name()
  {
    return PROVIDER_NAME;
  }

  @Override
  public RuntimeRepositoryType openRepository(
    final RuntimeRepositoryContextType context)
    throws IOException
  {
    return AOJRepository.create(
      this.clients.createClient(),
      this,
      context
    );
  }
}

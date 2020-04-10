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

package com.io7m.coffeepick.jdk_java_net;

import com.io7m.coffeepick.repository.spi.RuntimeRepositoryContextType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryType;
import org.osgi.service.component.annotations.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

/**
 * A jdk.java.net repository provider.
 */

@Component(service = RuntimeRepositoryProviderType.class)
public final class OJNRepositoryProvider implements RuntimeRepositoryProviderType
{
  private static final URI URI = java.net.URI.create("urn:net.java.jdk");

  /**
   * Construct a provider.
   */

  public OJNRepositoryProvider()
  {

  }

  @Override
  public URI uri()
  {
    return URI;
  }

  @Override
  public String name()
  {
    return "net.java.jdk";
  }

  @Override
  public RuntimeRepositoryType openRepository(
    final RuntimeRepositoryContextType context)
    throws IOException
  {
    Objects.requireNonNull(context, "context");
    return new OJNRepository(this);
  }
}

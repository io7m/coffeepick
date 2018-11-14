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

package com.io7m.coffeepick.repository.spi;

import io.reactivex.Observable;
import net.jcip.annotations.ThreadSafe;
import org.osgi.annotation.versioning.ConsumerType;

import java.net.URI;
import java.util.Map;

/**
 * A registry of runtime repository providers.
 *
 * Implementations are required to be thread-safe.
 */

@ConsumerType
@ThreadSafe
public interface RuntimeRepositoryProviderRegistryType
{
  /**
   * @return A stream of events indicating repository changes
   */

  Observable<RuntimeRepositoryProviderRegistryEventType> events();

  /**
   * @return The currently available repository providers
   */

  Map<URI, RuntimeRepositoryProviderType> repositoryProviders();
}

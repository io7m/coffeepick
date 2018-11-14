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
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import net.jcip.annotations.ThreadSafe;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A ServiceLoader-based repository registry.
 */

@ThreadSafe
public final class RuntimeRepositoriesServiceLoaderProvider
  implements RuntimeRepositoryProviderRegistryType
{
  private final Map<URI, RuntimeRepositoryProviderType> providers;
  private final Map<URI, RuntimeRepositoryProviderType> providers_read;
  private final Subject<RuntimeRepositoryProviderRegistryEventType> events;

  private RuntimeRepositoriesServiceLoaderProvider(
    final Map<URI, RuntimeRepositoryProviderType> in_repositories)
  {
    this.providers = Objects.requireNonNull(in_repositories, "providers");
    this.providers_read = Collections.unmodifiableMap(this.providers);
    this.events = PublishSubject.<RuntimeRepositoryProviderRegistryEventType>create().toSerialized();
  }

  /**
   * Create a new registry that looks for repository provider implementations using ServiceLoader.
   *
   * @return A new registry
   */

  public static RuntimeRepositoryProviderRegistryType create()
  {
    return new RuntimeRepositoriesServiceLoaderProvider(
      ServiceLoader.load(RuntimeRepositoryProviderType.class)
        .stream()
        .map(ServiceLoader.Provider::get)
        .collect(Collectors.toConcurrentMap(
          RuntimeRepositoryProviderType::uri,
          Function.identity())));
  }

  @Override
  public Observable<RuntimeRepositoryProviderRegistryEventType> events()
  {
    return this.events;
  }

  @Override
  public Map<URI, RuntimeRepositoryProviderType> repositoryProviders()
  {
    return this.providers_read;
  }
}

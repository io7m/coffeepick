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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderRegistryEventType.Change.ADDED;
import static com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderRegistryEventType.Change.REMOVED;

/**
 * An OSGi-based repository registry.
 */

@Component(service = RuntimeRepositoryProviderRegistryType.class)
public final class RuntimeRepositoriesOSGiProvider implements RuntimeRepositoryProviderRegistryType
{
  private final Map<URI, RuntimeRepositoryProviderType> providers;
  private final Subject<RuntimeRepositoryProviderRegistryEventType> events;
  private final Map<URI, RuntimeRepositoryProviderType> providers_read;

  /**
   * Construct an empty registry.
   */

  public RuntimeRepositoriesOSGiProvider()
  {
    this.providers = new ConcurrentHashMap<>(8);
    this.providers_read = Collections.unmodifiableMap(this.providers);
    this.events = PublishSubject.<RuntimeRepositoryProviderRegistryEventType>create().toSerialized();
  }

  /**
   * A repository provider became available.
   *
   * @param provider The repository provider
   */

  @Reference(
    service = RuntimeRepositoryProviderType.class,
    cardinality = ReferenceCardinality.MULTIPLE,
    policy = ReferencePolicy.DYNAMIC,
    policyOption = ReferencePolicyOption.GREEDY,
    unbind = "onRepositoryRemoved")
  public void onRepositoryAdded(
    final RuntimeRepositoryProviderType provider)
  {
    Objects.requireNonNull(provider, "repositoryProvider");

    this.providers.put(provider.uri(), provider);
    this.events.onNext(RuntimeRepositoryProviderRegistryEvent.of(ADDED, provider));
  }

  /**
   * A repository provider became unavailable.
   *
   * @param provider The repository provider
   */

  public void onRepositoryRemoved(
    final RuntimeRepositoryProviderType provider)
  {
    this.providers.remove(provider.uri());
    this.events.onNext(RuntimeRepositoryProviderRegistryEvent.of(REMOVED, provider));
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

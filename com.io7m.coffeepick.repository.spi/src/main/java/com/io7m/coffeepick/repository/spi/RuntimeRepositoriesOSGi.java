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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.io7m.coffeepick.repository.spi.RuntimeRepositoryRegistryEventType.Change.ADDED;
import static com.io7m.coffeepick.repository.spi.RuntimeRepositoryRegistryEventType.Change.REMOVED;

/**
 * An OSGi-based repository registry.
 */

@Component(service = RuntimeRepositoryRegistryType.class)
public final class RuntimeRepositoriesOSGi implements RuntimeRepositoryRegistryType
{
  private final Object repository_lock;
  private final Map<URI, RuntimeRepositoryType> repositories;
  private final Subject<RuntimeRepositoryRegistryEventType> events;

  /**
   * Construct an empty registry.
   */

  public RuntimeRepositoriesOSGi()
  {
    this.repository_lock = new Object();
    this.repositories = new HashMap<>(8);
    this.events = PublishSubject.<RuntimeRepositoryRegistryEventType>create().toSerialized();
  }

  /**
   * A repository became available.
   *
   * @param repository The repository
   */

  @Reference(
    service = RuntimeRepositoryType.class,
    cardinality = ReferenceCardinality.MULTIPLE,
    policy = ReferencePolicy.DYNAMIC,
    policyOption = ReferencePolicyOption.GREEDY,
    unbind = "onRepositoryRemoved")
  public void onRepositoryAdded(
    final RuntimeRepositoryType repository)
  {
    Objects.requireNonNull(repository, "repository");

    synchronized (this.repository_lock) {
      this.repositories.put(repository.uri(), repository);
    }
    this.events.onNext(RuntimeRepositoryRegistryEvent.of(ADDED, repository));
  }

  /**
   * A repository became unavailable.
   *
   * @param repository The repository
   */

  public void onRepositoryRemoved(
    final RuntimeRepositoryType repository)
  {
    synchronized (this.repository_lock) {
      this.repositories.remove(repository.uri());
    }
    this.events.onNext(RuntimeRepositoryRegistryEvent.of(REMOVED, repository));
  }

  @Override
  public Observable<RuntimeRepositoryRegistryEventType> events()
  {
    return this.events;
  }

  @Override
  public Map<URI, RuntimeRepositoryType> repositories()
  {
    synchronized (this.repository_lock) {
      return Map.copyOf(this.repositories);
    }
  }
}

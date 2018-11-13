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

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A ServiceLoader-based repository registry.
 */

public final class RuntimeRepositoriesServiceLoader implements RuntimeRepositoryRegistryType
{
  private final Map<URI, RuntimeRepositoryType> repositories;
  private final PublishSubject<RuntimeRepositoryRegistryEventType> events;

  private RuntimeRepositoriesServiceLoader(
    final Map<URI, RuntimeRepositoryType> in_repositories)
  {
    this.repositories = Objects.requireNonNull(in_repositories, "repositories");
    this.events = PublishSubject.create();
  }

  /**
   * Create a new registry that looks for repository implementations using ServiceLoader.
   *
   * @return A new registry
   */

  public static RuntimeRepositoryRegistryType create()
  {
    return new RuntimeRepositoriesServiceLoader(
      ServiceLoader.load(RuntimeRepositoryType.class)
        .stream()
        .map(ServiceLoader.Provider::get)
        .collect(Collectors.toMap(RuntimeRepositoryType::uri, Function.identity())));
  }

  @Override
  public Observable<RuntimeRepositoryRegistryEventType> events()
  {
    return this.events;
  }

  @Override
  public Map<URI, RuntimeRepositoryType> repositories()
  {
    return this.repositories;
  }
}

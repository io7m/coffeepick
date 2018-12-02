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

package com.io7m.coffeepick.shipilev_net;

import com.io7m.coffeepick.repository.spi.RuntimeRepositoryContextType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventUpdateFailed;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventUpdateFinished;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventUpdateRunning;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventUpdateStarted;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryType;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimeRepositoryDescription;
import com.io7m.coffeepick.runtime.database.RuntimeDescriptionDatabase;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * A repository based on the raw AdoptOpenJDK data.
 */

@Component(service = RuntimeRepositoryType.class)
public final class ASRepository implements RuntimeRepositoryType
{
  private static final Logger LOG = LoggerFactory.getLogger(ASRepository.class);

  private final HttpClient http;
  private final RuntimeDescriptionDatabase database;
  private final Subject<RuntimeRepositoryEventType> events;
  private final ASRepositoryProvider provider;

  /**
   * Construct a repository.
   *
   * @param in_http     The HTTP client used for requests
   * @param in_provider The owning provider
   * @param context     The runtime context
   */

  ASRepository(
    final HttpClient in_http,
    final ASRepositoryProvider in_provider,
    final RuntimeRepositoryContextType context)
    throws IOException
  {
    this.http =
      Objects.requireNonNull(in_http, "http");
    this.provider =
      Objects.requireNonNull(in_provider, "provider");

    this.database =
      RuntimeDescriptionDatabase.open(context.cacheDirectory().resolve("shipilev.net"));

    Objects.requireNonNull(context, "context");
    this.events = PublishSubject.<RuntimeRepositoryEventType>create().toSerialized();
  }

  @Override
  public Observable<RuntimeRepositoryEventType> events()
  {
    return this.events;
  }

  @Override
  public RuntimeRepositoryProviderType provider()
  {
    return this.provider;
  }

  @Override
  public void update(
    final BooleanSupplier cancelled)
    throws Exception
  {
    Objects.requireNonNull(cancelled, "cancelled");

    try {
      this.events.onNext(
        RuntimeRepositoryEventUpdateStarted.builder()
          .setRepository(this.provider.uri())
          .build());

      this.events.onNext(
        RuntimeRepositoryEventUpdateRunning.builder()
          .setRepository(this.provider.uri())
          .setProgress(0.0)
          .build());

      final var files = ASFileList.fetch(this.http);
      final var resolver = ASArchiveResolver.create(this.http);
      final var runtime_list = resolver.resolve(files);

      /*
       * There may be runtimes with duplicate hashes in the case of zero-length files.
       */

      final var runtimes = new HashMap<String, RuntimeDescription>(runtime_list.size());
      for (final var runtime : runtime_list) {
        final var runtime_id = runtime.id();
        if (runtimes.containsKey(runtime_id)) {
          LOG.debug("duplicate runtime id: {}", runtime_id);
          LOG.debug("  original: {}", runtime.archiveURI());
          LOG.debug("  new:      {}", runtimes.get(runtime_id).archiveURI());
        }
        runtimes.put(runtime_id, runtime);
      }

      var index = 0;
      for (final var runtime : runtimes.values()) {
        if (cancelled.getAsBoolean()) {
          throw new CancellationException();
        }

        this.database.add(runtime);
        this.events.onNext(
          RuntimeRepositoryEventUpdateRunning.builder()
            .setRepository(this.provider.uri())
            .setProgress((double) index / (double) runtimes.size())
            .build());
        ++index;
      }

      for (final var database_runtime : this.database.descriptions().keySet()) {
        if (!runtimes.containsKey(database_runtime)) {
          this.database.delete(database_runtime);
        }
      }

      this.events.onNext(
        RuntimeRepositoryEventUpdateFinished.builder()
          .setRepository(this.provider.uri())
          .build());
    } catch (final Exception e) {
      this.events.onNext(
        RuntimeRepositoryEventUpdateFailed.builder()
          .setRepository(this.provider.uri())
          .setException(e)
          .build());
      throw e;
    }
  }

  @Override
  public Map<String, RuntimeDescription> runtimes()
  {
    return this.database.descriptions();
  }

  @Override
  public RuntimeRepositoryDescription description()
  {
    return RuntimeRepositoryDescription.builder()
      .setId(this.provider.uri())
      .setRuntimes(Map.copyOf(this.runtimes()))
      .build();
  }
}

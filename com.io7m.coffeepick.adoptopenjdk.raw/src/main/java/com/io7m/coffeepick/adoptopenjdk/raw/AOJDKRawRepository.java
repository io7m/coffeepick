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

package com.io7m.coffeepick.adoptopenjdk.raw;

import com.io7m.coffeepick.repository.spi.RuntimeRepositoryContextType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventUpdateFailed;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventUpdateFinished;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventUpdateRunning;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventUpdateStarted;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryType;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.net.http.HttpClient.Redirect.NORMAL;
import static java.net.http.HttpResponse.BodyHandlers.ofInputStream;

/**
 * A repository based on the raw AdoptOpenJDK data.
 */

@Component(service = RuntimeRepositoryType.class)
public final class AOJDKRawRepository implements RuntimeRepositoryType
{
  private static final Logger LOG = LoggerFactory.getLogger(AOJDKRawRepository.class);

  private static final List<URI> URIS =
    List.of(
      URI.create(
        "https://raw.githubusercontent.com/AdoptOpenJDK/openjdk8-binaries/master/releases.json"),
      URI.create(
        "https://raw.githubusercontent.com/AdoptOpenJDK/openjdk9-binaries/master/releases.json"),
      URI.create(
        "https://raw.githubusercontent.com/AdoptOpenJDK/openjdk11-binaries/master/releases.json"));

  private final HttpClient http;
  private final AOJDKRuntimeDescriptionDatabase database;
  private final AOJDKRawRepositoryProvider provider;
  private final Subject<RuntimeRepositoryEventType> events;
  private final AOJDKArchiveResolver resolver;

  /**
   * Construct a repository.
   *
   * @param in_provider The owning provider
   * @param context     The runtime context
   */

  AOJDKRawRepository(
    final AOJDKRawRepositoryProvider in_provider,
    final RuntimeRepositoryContextType context)
    throws IOException
  {
    this.provider = Objects.requireNonNull(in_provider, "provider");
    Objects.requireNonNull(context, "context");

    this.http =
      HttpClient.newBuilder()
        .followRedirects(NORMAL)
        .build();

    this.database =
      AOJDKRuntimeDescriptionDatabase.open(context.cacheDirectory().resolve("adoptopenjdk.raw"));

    this.events = PublishSubject.<RuntimeRepositoryEventType>create().toSerialized();
    this.resolver = AOJDKArchiveResolver.create();
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
  public void update()
    throws Exception
  {
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

      final var archives = new ArrayList<AOJDKArchive>(128);
      for (final var uri : URIS) {
        try {
          final var request =
            HttpRequest.newBuilder(uri)
              .GET()
              .build();

          final var response = this.http.send(request, ofInputStream());
          if (response.statusCode() >= 400) {
            final var separator = System.lineSeparator();
            throw new IOException(
              new StringBuilder(128)
                .append("HTTP error")
                .append(separator)
                .append("  URI:         ")
                .append(uri)
                .append(separator)
                .append("  Status code: ")
                .append(response.statusCode())
                .append(separator)
                .toString());
          }

          final var parser = AOJDKDataParser.create(response.body());
          archives.addAll(parser.parse());
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      var index = 0;
      for (final var archive : archives) {
        try {
          final var release = this.resolver.resolveOne(archive);
          this.database.add(release);
        } catch (final IOException e) {
          LOG.error("unable to resolve archive {}: ", archive.archiveURI(), e);
        }

        this.events.onNext(
          RuntimeRepositoryEventUpdateRunning.builder()
            .setRepository(this.provider.uri())
            .setProgress((double) index / (double) archives.size())
            .build());
        ++index;
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
}

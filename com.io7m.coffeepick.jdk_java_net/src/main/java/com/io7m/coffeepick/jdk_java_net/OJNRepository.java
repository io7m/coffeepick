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

import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventUpdateFinished;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventUpdateStarted;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryType;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimeDescriptionType;
import com.io7m.coffeepick.runtime.RuntimeDescriptions;
import com.io7m.coffeepick.runtime.RuntimeRepositoryBranding;
import com.io7m.coffeepick.runtime.RuntimeRepositoryDescription;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A repository based on https://jdk.openjdk.net
 */

public final class OJNRepository implements RuntimeRepositoryType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(OJNRepository.class);
  private static final String BUILDS =
    "/com/io7m/coffeepick/jdk_java_net/build.properties";

  private final ConcurrentHashMap<String, RuntimeDescription> runtimes;
  private final Map<String, RuntimeDescription> runtimesRead;
  private final OJNRepositoryProvider provider;
  private final Subject<RuntimeRepositoryEventType> events;
  private volatile RuntimeRepositoryDescription description;

  OJNRepository(
    final OJNRepositoryProvider in_provider)
    throws IOException
  {
    this.provider = Objects.requireNonNull(in_provider, "provider");
    this.runtimes = new ConcurrentHashMap<>(128);
    this.runtimesRead = Collections.unmodifiableMap(this.runtimes);

    this.description =
      RuntimeRepositoryDescription.builder()
        .setBranding(createBranding())
        .setId(this.provider.uri())
        .setRuntimes(Map.copyOf(this.runtimes))
        .setUpdated(Optional.empty())
        .build();

    this.events = PublishSubject.<RuntimeRepositoryEventType>create().toSerialized();
    this.update(() -> false);
  }

  private static RuntimeDescription loadBuild(
    final String file)
  {
    LOG.debug("loading build {}", file);

    try (var build_stream = OJNRepository.class.getResourceAsStream(file)) {
      final var build = new Properties();
      build.load(build_stream);
      return RuntimeDescriptions.parseFromProperties(build);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static RuntimeRepositoryBranding createBranding()
  {
    try {
      return RuntimeRepositoryBranding.builder()
        .setLogo(logoURI())
        .setSite(URI.create("https://jdk.java.net"))
        .setSubtitle("Java Development Kit builds, from Oracle")
        .setTitle("jdk.java.net")
        .build();
    } catch (final URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private static URI logoURI()
    throws URISyntaxException
  {
    return OJNRepository.class.getResource(
      "/com/io7m/coffeepick/jdk_java_net/duke128.png"
    ).toURI();
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
    throws IOException
  {
    Objects.requireNonNull(cancelled, "cancelled");

    try {
      try (var stream = OJNRepository.class.getResourceAsStream(BUILDS)) {
        final var builds = new Properties();
        builds.load(stream);

        final var updated =
          OffsetDateTime.parse(builds.getProperty("coffeepick.updated"));
        final var names =
          builds.getProperty("coffeepick.builds").split(" ");

        this.events.onNext(
          RuntimeRepositoryEventUpdateStarted.builder()
            .setRepository(this.provider.uri())
            .build());

        final var next_runtimes =
          Stream.of(names)
            .map(OJNRepository::loadBuild)
            .collect(Collectors.toMap(
              RuntimeDescriptionType::id,
              Function.identity()));

        this.runtimes.clear();
        this.runtimes.putAll(next_runtimes);
        this.description = this.description.withUpdated(updated);

        this.events.onNext(
          RuntimeRepositoryEventUpdateFinished.builder()
            .setRepository(this.provider.uri())
            .build());
      }
    } catch (final UncheckedIOException e) {
      throw e.getCause();
    }
  }

  @Override
  public Map<String, RuntimeDescription> runtimes()
  {
    return this.runtimesRead;
  }

  @Override
  public RuntimeRepositoryDescription description()
  {
    return this.description;
  }
}

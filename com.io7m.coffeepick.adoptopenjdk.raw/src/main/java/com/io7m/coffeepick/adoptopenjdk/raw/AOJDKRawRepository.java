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
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryType;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;
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

  /**
   * Construct a repository.
   */

  public AOJDKRawRepository()
  {
    this.http =
      HttpClient.newBuilder()
        .followRedirects(NORMAL)
        .build();
  }

  @Override
  public URI uri()
  {
    return URI.create("https://www.github.com/AdoptOpenJDK/");
  }

  @Override
  public String name()
  {
    return "adoptopenjdk.raw";
  }

  @Override
  public List<RuntimeDescription> availableRuntimes(
    final RuntimeRepositoryContextType context)
    throws IOException
  {
    Objects.requireNonNull(context, "context");

    final var cache =
      AOJDKRuntimeDescriptionDatabase.open(context.cacheDirectory().resolve("adoptopenjdk.raw"));

    for (final var uri : URIS) {
      try {
        final var request =
          HttpRequest.newBuilder(uri)
            .GET()
            .build();

        final var parser = AOJDKDataParser.create(this.http.send(request, ofInputStream()).body());
        final var resolver = AOJDKArchiveResolver.create();
        final var archives = parser.parse();
        final var releases = resolver.resolve(archives);
        for (final var release : releases) {
          cache.add(release);
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    return List.copyOf(cache.descriptions().values());
  }
}

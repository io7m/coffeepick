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

package com.io7m.coffeepick.adoptopenjdk.v1;

import com.io7m.coffeepick.repository.spi.RuntimeRepositoryType;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimeHash;
import com.io7m.coffeepick.runtime.RuntimeVMs;
import net.adoptopenjdk.api.AdoptOpenJDK;
import net.adoptopenjdk.api.AdoptOpenJDKRequestsType;
import net.adoptopenjdk.spi.AOAPIRequestsType;
import net.adoptopenjdk.spi.AOBinary;
import net.adoptopenjdk.spi.AOException;
import net.adoptopenjdk.spi.AOVariant;
import net.adoptopenjdk.v1.AOv1Requests;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A repository based on the V1 AdoptOpenJDK API.
 */

@Component(service = RuntimeRepositoryType.class)
public final class AOJDKv1Repository implements RuntimeRepositoryType
{
  private static final Logger LOG = LoggerFactory.getLogger(AOJDKv1Repository.class);

  private final AdoptOpenJDKRequestsType api;
  private final HttpClient http;

  private static final Pattern OPENJDK_VERSION_PATTERN =
    Pattern.compile("openjdk([0-9]+)-.*");

  /**
   * Construct a repository.
   */

  public AOJDKv1Repository()
  {
    try {
      final AOAPIRequestsType v1 = AOv1Requests.open();
      this.api = AdoptOpenJDK.wrap(v1);
      this.http =
        HttpClient.newBuilder()
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();
    } catch (final AOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public URI uri()
  {
    return URI.create("https://api.adoptopenjdk.net/v1/");
  }

  @Override
  public String name()
  {
    return "adoptopenjdk.v1";
  }

  @Override
  public List<RuntimeDescription> availableRuntimes()
    throws IOException
  {
    final List<AOVariant> variants;
    try {
      variants = this.api.variants();
    } catch (final AOException e) {
      throw new IOException(e);
    }

    final var descriptions = new ArrayList<RuntimeDescription>(128);
    for (final var variant : variants) {
      final var vm = guessVM(variant);
      final var version = guessVersion(variant);

      try {
        final var releases = this.api.releasesForVariant(variant.name());
        for (final var release : releases) {
          for (final var binary : release.binaries()) {
            descriptions.add(this.loadBinary(binary, vm, version));
          }
        }
      } catch (final AOException | IOException e) {
        LOG.error("error loading releases: ", e);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    return descriptions;
  }

  private static Runtime.Version guessVersion(
    final AOVariant variant)
    throws IOException
  {
    final var matches = OPENJDK_VERSION_PATTERN.matcher(variant.name());
    if (matches.matches()) {
      return Runtime.Version.parse(matches.group(1));
    }

    final var separator = System.lineSeparator();
    throw new IOException(
      new StringBuilder(128).append(
        "Received unparseable variant version")
        .append(separator)
        .append("  Name: ")
        .append(variant.name())
        .append(separator)
        .toString());
  }

  private static String guessVM(
    final AOVariant variant)
  {
    if (variant.name().contains("hotspot")) {
      return RuntimeVMs.HOTSPOT.vmName();
    }
    if (variant.name().contains("openj9")) {
      return RuntimeVMs.OPENJ9.vmName();
    }
    return "hotspot";
  }

  private RuntimeDescription loadBinary(
    final AOBinary binary,
    final String vm,
    final Runtime.Version version)
    throws IOException, InterruptedException
  {
    final var architecture = binary.architecture().toLowerCase();
    final var platform = binary.operatingSystem().toLowerCase();
    final var hash = this.fetchHash(binary.checksumLink());

    return RuntimeDescription.builder()
      .setArchiveHash(hash)
      .setArchiveURI(binary.link())
      .setArchiveSize(binary.size().longValue())
      .setArchitecture(architecture)
      .setPlatform(platform)
      .setVm(vm)
      .setVersion(version)
      .addTags("adoptopenjdk")
      .addTags("production")
      .build();
  }

  private RuntimeHash fetchHash(final URI link)
    throws IOException, InterruptedException
  {
    final var request =
      HttpRequest.newBuilder(link)
        .GET()
        .build();

    final var response = this.http.send(request, HttpResponse.BodyHandlers.ofString());
    final var body_text = response.body();
    final var sections = List.of(body_text.split(" "));
    if (sections.size() >= 1) {
      return RuntimeHash.of("SHA-256", sections.get(0).toLowerCase());
    }

    final var separator = System.lineSeparator();
    throw new IOException(
      new StringBuilder(128).append(
        "Received unparseable hash value")
        .append(separator)
        .append("  URI:   ")
        .append(link)
        .append(separator)
        .append("  Value: ")
        .append(body_text, 0, Math.min(body_text.length(), 16))
        .append(" (rest of value truncated for error message display)")
        .append(separator)
        .toString());
  }
}

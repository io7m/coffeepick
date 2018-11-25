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

import com.io7m.coffeepick.runtime.RuntimeConfiguration;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimeHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;

import static com.io7m.coffeepick.adoptopenjdk.raw.AOJDKRawRepositoryProvider.PROVIDER_URI;

/**
 * Resolve a list of archives. A resolver takes a list of archive runtimes parsed from the
 * AdoptOpenJDK git repository and turns them into runtime runtimes by transforming metadata and
 * fetching checksums from the remote server.
 */

public final class AOJDKArchiveResolver
{
  private static final Logger LOG = LoggerFactory.getLogger(AOJDKArchiveResolver.class);

  private final HttpClient http;

  private AOJDKArchiveResolver(
    final HttpClient in_http)
  {
    this.http = Objects.requireNonNull(in_http, "http");
  }

  /**
   * Create a new resolver.
   *
   * @param http The HTTP client used for requests
   *
   * @return A resolver
   */

  public static AOJDKArchiveResolver create(
    final HttpClient http)
  {
    return new AOJDKArchiveResolver(http);
  }

  private static RuntimeHash unparseableHash(
    final URI link,
    final String text,
    final IllegalArgumentException e)
    throws IOException
  {
    final var separator = System.lineSeparator();
    throw new IOException(
      new StringBuilder(128)
        .append(
          "Received unparseable hash value")
        .append(separator)
        .append("  URI:   ")
        .append(link)
        .append(separator)
        .append("  Value: ")
        .append(text, 0, Math.min(text.length(), 16))
        .append(" (rest of value truncated for error message display)")
        .append(separator)
        .toString(),
      e);
  }

  private static RuntimeConfiguration mapConfiguration(
    final AOJDKFilenameMetadata metadata)
  {
    if (metadata.isJRE()) {
      return RuntimeConfiguration.JRE;
    }
    return RuntimeConfiguration.JDK;
  }

  private RuntimeHash fetchHash(final URI link)
    throws IOException, InterruptedException
  {
    LOG.debug("fetching checksum: {}", link);

    final var request =
      HttpRequest.newBuilder(link)
        .GET()
        .build();

    final var response = this.http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() >= 400) {
      final var separator = System.lineSeparator();
      throw new IOException(
        new StringBuilder(128)
          .append("HTTP error")
          .append(separator)
          .append("  URI:         ")
          .append(link)
          .append(separator)
          .append("  Status code: ")
          .append(response.statusCode())
          .append(separator)
          .toString());
    }

    final var body_text = response.body();
    final var sections = List.of(body_text.split(" "));
    if (sections.size() >= 1) {
      try {
        return RuntimeHash.of("SHA-256", sections.get(0).toLowerCase());
      } catch (final IllegalArgumentException e) {
        return unparseableHash(link, body_text, e);
      }
    }
    return unparseableHash(link, body_text, null);
  }

  /**
   * Resolve a single archive.
   *
   * @param archive The archive
   *
   * @return A runtime runtimes
   *
   * @throws IOException On I/O errors
   */

  public RuntimeDescription resolveOne(
    final AOJDKArchive archive)
    throws IOException
  {
    Objects.requireNonNull(archive, "archive");

    try {
      final var hash = this.fetchHash(archive.archiveChecksumURI());

      final var builder = RuntimeDescription.builder();
      builder.setRepository(PROVIDER_URI);
      builder.setConfiguration(mapConfiguration(archive.metadata()));
      builder.setArchitecture(archive.metadata().architecture());
      builder.setArchiveHash(hash);
      builder.setArchiveSize(archive.archiveSize());
      builder.setArchiveURI(archive.archiveURI());
      builder.setPlatform(archive.metadata().platform());
      builder.setVersion(Runtime.Version.parse(archive.metadata().number()));
      builder.setVm(archive.metadata().vm());

      if (archive.metadata().isLargeHeap()) {
        builder.addTags("large-heap");
      }

      builder.addTags("production");
      return builder.build();
    } catch (final InterruptedException e) {
      throw new IOException(e);
    }
  }
}

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

package com.io7m.coffeepick.shipilev_net.internal;

import com.io7m.coffeepick.runtime.RuntimeConfiguration;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimeHash;
import com.io7m.coffeepick.shipilev_net.ASRepositoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static java.net.http.HttpResponse.BodyHandlers.ofInputStream;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A resolver for shipilev.net builds.
 */

public final class ASArchiveResolver
{
  private static final Logger LOG =
    LoggerFactory.getLogger(ASArchiveResolver.class);

  private static final String BASE_URI =
    "https://builds.shipilev.net";

  private static final Pattern WHITESPACE =
    Pattern.compile("\\s+");

  private final HttpClient client;

  private ASArchiveResolver(
    final HttpClient in_client)
  {
    this.client = Objects.requireNonNull(in_client, "client");
  }

  /**
   * Create a new resolver.
   *
   * @param client The HTTP client that will be used
   *
   * @return A new resolver
   */

  public static ASArchiveResolver create(
    final HttpClient client)
  {
    return new ASArchiveResolver(client);
  }

  private static Optional<RuntimeDescription> runtimeOf(
    final URI checksum_file,
    final Map<String, String> checksums,
    final String directory,
    final ASFile file,
    final URI uri)
  {
    final var builder =
      RuntimeDescription.builder()
        .setArchiveHash(hashOf(checksum_file, checksums, file))
        .setRepository(ASRepositoryProvider.PROVIDER_URI);

    final var metadata_opt = ASFilenameMetadataParsing.parseFilename(
      directory,
      file.name());
    if (metadata_opt.isPresent()) {
      final var metadata = metadata_opt.get();
      builder.setArchitecture(metadata.architecture());
      builder.setBuild(metadata.build());
      builder.setPlatform(metadata.platform());
      builder.setVersion(metadata.version());
      builder.setVm("hotspot");
      builder.setConfiguration(RuntimeConfiguration.JDK);
      builder.setArchiveURI(uri);
      builder.setArchiveSize(file.size());
      builder.addAllTags(metadata.extraTags());
      builder.addTags("nightly");
      return Optional.of(builder.build());
    }

    return Optional.empty();
  }

  private static RuntimeHash hashOf(
    final URI checksum_file,
    final Map<String, String> checksums,
    final ASFile file)
  {
    final var name = file.name();
    if (checksums.containsKey(name)) {
      return RuntimeHash.of("SHA1", checksums.get(name));
    }

    throw new IllegalStateException(
      new StringBuilder(128)
        .append("Checksum missing for file")
        .append(System.lineSeparator())
        .append("  Checksums: ")
        .append(checksum_file)
        .append(System.lineSeparator())
        .append("  File: ")
        .append(name)
        .append(System.lineSeparator())
        .toString());
  }

  private static HashMap<String, Set<ASFile>> groupByDirectory(
    final Collection<ASFile> files)
  {
    final HashMap<String, Set<ASFile>> grouped = new HashMap<>(files.size());
    for (final var file : files) {
      final Set<ASFile> values;
      final var directory = file.directory();
      if (grouped.containsKey(directory)) {
        values = grouped.get(directory);
      } else {
        values = new HashSet<>();
      }

      values.add(file);
      grouped.put(directory, values);
    }
    return grouped;
  }

  /**
   * Resolve a collection of files.
   *
   * @param files The file collection
   *
   * @return A list of resolved runtime descriptions
   *
   * @throws IOException          On I/O errors
   * @throws InterruptedException If the operation is interrupted
   */

  public List<RuntimeDescription> resolve(
    final Collection<ASFile> files)
    throws IOException, InterruptedException
  {
    Objects.requireNonNull(files, "files");

    final var runtimes = new ArrayList<RuntimeDescription>(files.size());
    final var grouped = groupByDirectory(files);
    for (final var directory : grouped.keySet()) {
      final var checksum_uri = URI.create(String.format(
        "%s/%s/SHA1SUMS",
        BASE_URI,
        directory));
      LOG.debug("{}", checksum_uri);

      final var checksums = this.fetchChecksums(checksum_uri);
      for (final var file : grouped.get(directory)) {
        final var uri = URI.create(String.format(
          "%s/%s/%s",
          BASE_URI,
          directory,
          file.name()));
        LOG.debug("{}", uri);
        runtimeOf(checksum_uri, checksums, directory, file, uri)
          .ifPresent(runtimes::add);
      }
    }

    return runtimes;
  }

  private Map<String, String> fetchChecksums(
    final URI checksum_uri)
    throws IOException, InterruptedException
  {
    final var request =
      HttpRequest.newBuilder(checksum_uri)
        .GET()
        .build();

    final var response = this.client.send(request, ofInputStream());
    if (response.statusCode() >= 400) {
      final var separator = System.lineSeparator();
      throw new IOException(
        new StringBuilder(128)
          .append("HTTP error")
          .append(separator)
          .append("  URI:         ")
          .append(checksum_uri)
          .append(separator)
          .append("  Status code: ")
          .append(response.statusCode())
          .append(separator)
          .toString());
    }

    final Map<String, String> checksums = new HashMap<>(128);
    try (var stream = response.body()) {
      try (var buffered = new BufferedReader(new InputStreamReader(
        stream,
        UTF_8))) {
        buffered.lines().forEach(line -> {
          final var segments = List.of(WHITESPACE.split(line.trim()));
          if (segments.size() == 2) {
            checksums.put(segments.get(1), segments.get(0));
          }
        });
      }
    }

    return checksums;
  }
}

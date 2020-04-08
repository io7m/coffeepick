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

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.net.http.HttpResponse.BodyHandlers.ofInputStream;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * File list parsing.
 */

public final class ASFileList
{
  private static final URI FILE_LIST_URI =
    URI.create("https://builds.shipilev.net/file-sizes-list.xz");
  private static final Pattern TWO_COMPONENT_PATH =
    Pattern.compile("(.+)/(.+)");
  private static final Pattern SIZE_AND_NAME =
    Pattern.compile("\\s+([0-9]+)\\s+\\./(.+)");

  private ASFileList()
  {

  }

  /**
   * Fetch the latest file list.
   *
   * @param client The HTTP client
   *
   * @return A list of files
   *
   * @throws IOException          On I/O errors
   * @throws InterruptedException If the operation is interrupted
   */

  public static SortedSet<ASFile> fetch(
    final HttpClient client)
    throws IOException, InterruptedException
  {
    Objects.requireNonNull(client, "client");

    final var request =
      HttpRequest.newBuilder(FILE_LIST_URI)
        .GET()
        .build();

    final var response = client.send(request, ofInputStream());
    if (response.statusCode() >= 400) {
      final var separator = System.lineSeparator();
      throw new IOException(
        new StringBuilder(128)
          .append("HTTP error")
          .append(separator)
          .append("  URI:         ")
          .append(FILE_LIST_URI)
          .append(separator)
          .append("  Status code: ")
          .append(response.statusCode())
          .append(separator)
          .toString());
    }

    try (var stream = response.body()) {
      try (var input = new XZCompressorInputStream(stream)) {
        try (var buffered = new BufferedReader(new InputStreamReader(
          input,
          UTF_8))) {
          return buffered.lines()
            .flatMap(ASFileList::toFile)
            .filter(ASFileList::isRelease)
            .collect(Collectors.toCollection(TreeSet::new));
        }
      }
    }
  }

  private static Stream<ASFile> toFile(
    final String line)
  {
    final var size_name = SIZE_AND_NAME.matcher(line);
    if (size_name.matches()) {
      final var size = Long.parseUnsignedLong(size_name.group(1));
      final var file = size_name.group(2);
      final var two_component = TWO_COMPONENT_PATH.matcher(file);
      if (two_component.matches()) {
        final var directory = two_component.group(1);
        final var file_name = two_component.group(2);
        return Stream.of(
          ASFile.builder()
            .setSize(size)
            .setDirectory(directory)
            .setName(file_name)
            .build());
      }
    }
    return Stream.empty();
  }

  private static boolean isRelease(
    final ASFile file)
  {
    final var name = file.name();
    return isOpenJDKBuild(name) && isArchive(name);
  }

  private static boolean isArchive(final String name)
  {
    return name.endsWith(".tar.gz") || name.endsWith(".tar.xz");
  }

  private static boolean isOpenJDKBuild(final String name)
  {
    return name.startsWith("openjdk-") && (!name.contains("-latest-"));
  }

  private static boolean isBuild(
    final String line)
  {
    return line.startsWith("./openjdk-");
  }
}

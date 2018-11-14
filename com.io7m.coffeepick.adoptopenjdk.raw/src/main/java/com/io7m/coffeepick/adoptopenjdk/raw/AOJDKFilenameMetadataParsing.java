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

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Functions to parse metadata from AdoptOpenJDK file names.
 */

public final class AOJDKFilenameMetadataParsing
{
  // CHECKSTYLE:OFF
  private static final Pattern TIMESTAMP_REGEX =
    Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}");
  private static final Pattern VERSION_REGEX =
    Pattern.compile("[0-9]{2}_[0-9]+|8u[0-9]+-?b[0-9X]+|[0-9]+\\.[0-9]+\\.[0-9]+_[0-9]+");
  private static final Pattern REGEX =
    Pattern.compile(
      "OpenJDK(?<num>[0-9]+)U?(?<type>-jre|-jdk)?_(?<arch>[0-9a-zA-Z-]+)_(?<os>[0-9a-zA-Z]+)_(?<impl>[0-9a-zA-Z]+)_?(?<heap>[0-9a-zA-Z]+)?.*_(?<tsOrVersion>" + TIMESTAMP_REGEX + "|" + VERSION_REGEX + ")(?<randSuffix>[-0-9A-Za-z\\._]+)?\\.(?<extension>tar\\.gz|zip)");
  private static final Pattern OLD_TIMESTAMP_REGEX =
    Pattern.compile("[0-9]{4}[0-9]{2}[0-9]{2}[0-9]{2}[0-9]{2}");
  private static final Pattern OLD_REGEX =
    Pattern.compile(
      "OpenJDK(?<num>[0-9]+)U?(?<type>-[0-9a-zA-Z]+)?_(?<arch>[0-9a-zA-Z]+)_(?<os>[0-9a-zA-Z]+).*_?(?<ts>" + OLD_TIMESTAMP_REGEX + ")?.(?<extension>tar.gz|zip)");
  // CHECKSTYLE:ON

  private AOJDKFilenameMetadataParsing()
  {

  }

  /**
   * Parse metadata from the given filename.
   *
   * @param name The name
   *
   * @return Parsed metadata, or nothing if parsing could not proceed
   */

  public static Optional<AOJDKFilenameMetadata> parseFilename(
    final String name)
  {
    Objects.requireNonNull(name, "name");

    final var matcher = REGEX.matcher(name);
    if (matcher.matches()) {
      return parseFromRegex(matcher);
    }

    final var old_matcher = OLD_REGEX.matcher(name);
    if (old_matcher.matches()) {
      return parseFromOldRegex(old_matcher);
    }

    return Optional.empty();
  }

  private static Optional<AOJDKFilenameMetadata> parseFromOldRegex(
    final Matcher matcher)
  {
    final var number = matcher.group("num");
    final var type = matcher.group("type");
    final var arch = matcher.group("arch");
    final var os = matcher.group("os");
    final var ts = matcher.group("ts");

    final var builder = AOJDKFilenameMetadata.builder();
    builder.setLargeHeap(false);
    parseOldVersionString(number, ts, builder);
    parseJRE(type, builder);
    builder.setNumber(number);
    builder.setArchitecture(arch);
    builder.setPlatform(mapOS(os));
    builder.setVm("hotspot");
    return Optional.of(builder.build());
  }

  private static void parseOldVersionString(
    final String number,
    final String timestamp,
    final AOJDKFilenameMetadata.Builder builder)
  {
    if (timestamp == null) {
      builder.setVersionString(number);
    } else {
      builder.setVersionString(timestamp);
    }
    builder.setTimestampedVersion(false);
  }

  private static Optional<AOJDKFilenameMetadata> parseFromRegex(
    final Matcher matcher)
  {
    final var number = matcher.group("num");
    final var type = matcher.group("type");
    final var arch = matcher.group("arch");
    final var os = matcher.group("os");
    final var impl = matcher.group("impl");
    final var heap = matcher.group("heap");
    final var ts_or_version = matcher.group("tsOrVersion");

    final var builder = AOJDKFilenameMetadata.builder();
    parseHeap(heap, builder);
    parseVersion(ts_or_version, builder);
    parseJRE(type, builder);
    builder.setNumber(number);
    builder.setArchitecture(arch);
    builder.setPlatform(mapOS(os));
    builder.setVm(impl);
    return Optional.of(builder.build());
  }

  private static void parseJRE(
    final String type,
    final AOJDKFilenameMetadata.Builder builder)
  {
    builder.setJRE(Objects.equals(type, "-jre"));
  }

  private static void parseVersion(
    final String version,
    final AOJDKFilenameMetadata.Builder builder)
  {
    builder.setVersionString(version);
    builder.setTimestampedVersion(TIMESTAMP_REGEX.matcher(version).matches());
  }

  private static void parseHeap(
    final String heap,
    final AOJDKFilenameMetadata.Builder builder)
  {
    builder.setLargeHeap(false);

    if (heap != null) {
      if (Objects.equals(heap, "linuxXL")) {
        builder.setLargeHeap(true);
      }
    }
  }

  private static String mapOS(
    final String os)
  {
    switch (os) {
      case "mac":
        return "macos";
      default:
        return os;
    }
  }
}

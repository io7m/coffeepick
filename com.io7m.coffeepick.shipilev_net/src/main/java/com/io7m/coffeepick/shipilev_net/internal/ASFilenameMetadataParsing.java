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

import com.io7m.coffeepick.runtime.RuntimeBuild;
import com.io7m.coffeepick.runtime.RuntimeVersions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Functions to parse metadata from shipilev.net file names.
 */

public final class ASFilenameMetadataParsing
{
  private static final Pattern BUILD_NUMBERS_WITH_DATE =
    Pattern.compile(
      "^([a-z0-9]+)-([0-9]{4})([0-9]{2})([0-9]{2})-jdk-([a-z0-9+.]+)-(.*)");

  private static final Pattern BUILD_PLATFORM_ARCH_TAGS =
    Pattern.compile(
      "^([a-z0-9]+)-([a-z0-9_-]+)-(release|fastdebug|slowdebug)(.*)");

  private static final Set<String> TAG_NAMES =
    Set.of(
      "loom",
      "panama",
      "portola",
      "shenandoah",
      "valhalla",
      "zgc",
      "redhat");

  private ASFilenameMetadataParsing()
  {

  }

  /**
   * Parse metadata from the given filename.
   *
   * @param tag  The build tag
   * @param name The name
   *
   * @return Parsed metadata, or nothing if parsing could not proceed
   */

  public static Optional<ASFilenameMetadata> parseFilename(
    final String tag,
    final String name)
  {
    Objects.requireNonNull(tag, "tag");
    Objects.requireNonNull(name, "name");

    final var without_tag = name.replace(tag + "-", "");
    final var builder = ASFilenameMetadata.builder();
    final var date_matcher = BUILD_NUMBERS_WITH_DATE.matcher(without_tag);
    if (date_matcher.matches()) {
      final var build_number = date_matcher.group(1);
      final var build_year = date_matcher.group(2);
      final var build_month = date_matcher.group(3);
      final var build_day = date_matcher.group(4);
      final var build_version = date_matcher.group(5);
      final var rest = date_matcher.group(6);

      final var time =
        OffsetDateTime.of(
          Integer.parseUnsignedInt(build_year),
          Integer.parseUnsignedInt(build_month),
          Integer.parseUnsignedInt(build_day),
          0,
          0,
          0,
          0,
          ZoneOffset.UTC);

      builder.setVersion(RuntimeVersions.parse(build_version));
      builder.setBuild(
        RuntimeBuild.builder()
          .setBuildNumber(build_number)
          .setTime(time)
          .build());

      final var platform_matcher = BUILD_PLATFORM_ARCH_TAGS.matcher(rest);
      if (platform_matcher.matches()) {
        final var platform = platform_matcher.group(1);
        final var arch = platform_matcher.group(2);
        final var tags = platform_matcher.group(3);
        builder.setPlatform(platform);
        builder.setArchitecture(mapArchitecture(arch));
        builder.addExtraTags(tags);
        builder.addAllExtraTags(inferTags(tag));
        return Optional.of(builder.build());
      }
    }

    return Optional.empty();
  }

  private static String mapArchitecture(
    final String arch)
  {
    switch (arch) {
      case "x86":
        return "x32";
      case "x86_64":
        return "x64";
      default:
        return arch;
    }
  }

  private static Set<String> inferTags(
    final String tag)
  {
    final HashSet<String> tags = new HashSet<>();
    for (final var name : TAG_NAMES) {
      inferTag(tags, tag, name);
    }
    return tags;
  }

  private static void inferTag(
    final HashSet<String> tags,
    final String tag,
    final String name)
  {
    if (tag.contains(name)) {
      tags.add(name);
    }
  }
}

/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> http://io7m.com
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

package com.io7m.coffeepick.runtime;

import java.math.BigInteger;
import java.util.regex.Pattern;

/**
 * Functions over runtime versions.
 */

public final class RuntimeVersions
{
  private RuntimeVersions()
  {

  }

  /**
   * A pattern for simple major versions.
   */

  private static Pattern VERSION_MAJOR_PATTERN =
    Pattern.compile(
      "^([0-9]+)$"
    );

  /**
   * A pattern for major.minor versions.
   */

  private static Pattern VERSION_MAJOR_MINOR_PATTERN =
    Pattern.compile(
      "^([0-9]+)\\.([0-9]+)$"
    );

  /**
   * A pattern for major.minor.patch versions.
   */

  private static Pattern VERSION_MAJOR_MINOR_PATCH_PATTERN =
    Pattern.compile(
      "^([0-9]+)\\.([0-9]+)\\.([0-9]+)$"
    );

  /**
   * A pattern for major.minor.patch+build versions.
   */

  private static Pattern VERSION_MAJOR_MINOR_PATCH_BUILD_PATTERN =
    Pattern.compile(
      "^([0-9]+)\\.([0-9]+)\\.([0-9]+)\\+([0-9]+)$"
    );

  /**
   * Parse a version string.
   *
   * @param text The version string
   *
   * @return A parsed version
   *
   * @throws IllegalArgumentException On parse errors
   */

  public static RuntimeVersion parse(
    final String text)
    throws IllegalArgumentException
  {
    final var trimmed = text.trim();

    final var majorMatcher =
      VERSION_MAJOR_PATTERN.matcher(trimmed);
    if (majorMatcher.matches()) {
      return RuntimeVersion.builder()
        .setMajor(new BigInteger(majorMatcher.group(1)))
        .setMinor(BigInteger.ZERO)
        .setPatch(BigInteger.ZERO)
        .build();
    }

    final var minorMatcher =
      VERSION_MAJOR_MINOR_PATTERN.matcher(trimmed);
    if (minorMatcher.matches()) {
      return RuntimeVersion.builder()
        .setMajor(new BigInteger(minorMatcher.group(1)))
        .setMinor(new BigInteger(minorMatcher.group(2)))
        .setPatch(BigInteger.ZERO)
        .build();
    }

    final var patchMatcher =
      VERSION_MAJOR_MINOR_PATCH_PATTERN.matcher(trimmed);
    if (patchMatcher.matches()) {
      return RuntimeVersion.builder()
        .setMajor(new BigInteger(patchMatcher.group(1)))
        .setMinor(new BigInteger(patchMatcher.group(2)))
        .setPatch(new BigInteger(patchMatcher.group(3)))
        .build();
    }

    final var allMatcher =
      VERSION_MAJOR_MINOR_PATCH_BUILD_PATTERN.matcher(trimmed);
    if (allMatcher.matches()) {
      return RuntimeVersion.builder()
        .setMajor(new BigInteger(allMatcher.group(1)))
        .setMinor(new BigInteger(allMatcher.group(2)))
        .setPatch(new BigInteger(allMatcher.group(3)))
        .setBuild(new BigInteger(allMatcher.group(4)))
        .build();
    }

    final var separator = System.lineSeparator();
    throw new IllegalArgumentException(
      new StringBuilder(64)
        .append("Unparseable runtime version")
        .append(separator)
        .append("  Expected: A runtime version")
        .append(separator)
        .append("  Received: ")
        .append(text)
        .append(separator)
        .toString()
    );
  }
}

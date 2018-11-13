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

package com.io7m.coffeepick.shell;

import com.io7m.coffeepick.runtime.RuntimeVersionRange;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Functions to parse version ranges.
 */

public final class CoffeePickShellVersionRanges
{
  private static final Pattern PATTERN_INC_LOWER_INC_UPPER =
    Pattern.compile("\\[([0-9a-z\\-+]+)[ ]*,([0-9a-z\\-+]+)[ ]*]");
  private static final Pattern PATTERN_INC_LOWER_EXC_UPPER =
    Pattern.compile("\\[([0-9a-z\\-+]+)[ ]*,([0-9a-z\\-+]+)[ ]*\\)");
  private static final Pattern PATTERN_EXC_LOWER_INC_UPPER =
    Pattern.compile("\\(([0-9a-z\\-+]+)[ ]*,([0-9a-z\\-+]+)[ ]*]");
  private static final Pattern PATTERN_EXC_LOWER_EXC_UPPER =
    Pattern.compile("\\(([0-9a-z\\-+]+)[ ]*,([0-9a-z\\-+]+)[ ]*\\)");
  private CoffeePickShellVersionRanges()
  {

  }

  /**
   * Parse a version range.
   *
   * @param text The input text
   *
   * @return A parsed version range
   *
   * @throws IllegalArgumentException If the input cannot be parsed as a version range
   */

  public static RuntimeVersionRange parse(
    final String text)
    throws IllegalArgumentException
  {
    Objects.requireNonNull(text, "text");

    try {
      {
        final var matcher = PATTERN_INC_LOWER_INC_UPPER.matcher(text);
        if (matcher.matches()) {
          return make(matcher, false, false);
        }
      }

      {
        final var matcher = PATTERN_INC_LOWER_EXC_UPPER.matcher(text);
        if (matcher.matches()) {
          return make(matcher, false, true);
        }
      }

      {
        final var matcher = PATTERN_EXC_LOWER_INC_UPPER.matcher(text);
        if (matcher.matches()) {
          return make(matcher, true, false);
        }
      }

      {
        final var matcher = PATTERN_EXC_LOWER_EXC_UPPER.matcher(text);
        if (matcher.matches()) {
          return make(matcher, true, true);
        }
      }

      final var version = Runtime.Version.parse(text);
      return RuntimeVersionRange.builder()
        .setLowerExclusive(false)
        .setLower(version)
        .setUpperExclusive(false)
        .setUpper(version)
        .build();
    } catch (final Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static RuntimeVersionRange make(
    final Matcher matcher,
    final boolean lower_ex,
    final boolean upper_ex)
  {
    return RuntimeVersionRange.builder()
      .setLowerExclusive(lower_ex)
      .setLower(Runtime.Version.parse(matcher.group(1)))
      .setUpperExclusive(upper_ex)
      .setUpper(Runtime.Version.parse(matcher.group(2)))
      .build();
  }
}

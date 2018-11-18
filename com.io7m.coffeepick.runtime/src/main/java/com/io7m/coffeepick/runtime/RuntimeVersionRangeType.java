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

package com.io7m.coffeepick.runtime;

import com.io7m.immutables.styles.ImmutablesStyleType;
import org.immutables.value.Value;

import java.util.Objects;

/**
 * A range of runtime versions.
 */

@ImmutablesStyleType
@Value.Immutable
public interface RuntimeVersionRangeType
{
  /**
   * @return The lower bound of the range
   */

  @Value.Parameter
  Runtime.Version lower();

  /**
   * @return {@code true} iff the lower bound is exclusive
   */

  @Value.Parameter
  boolean lowerExclusive();

  /**
   * @return The upper bound of the range
   */

  @Value.Parameter
  Runtime.Version upper();

  /**
   * @return {@code true} iff the upper bound is exclusive
   */

  @Value.Parameter
  boolean upperExclusive();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    if (this.lower().compareTo(this.upper()) > 0) {
      final var separator = System.lineSeparator();
      throw new IllegalArgumentException(
        new StringBuilder(128)
          .append("Invalid version range.")
          .append(separator)
          .append("  Problem: Lower version must be less than or equal to the upper version")
          .append(separator)
          .append("  Lower:   ")
          .append(this.lower())
          .append(separator)
          .append("  Upper:   ")
          .append(this.upper())
          .append(separator)
          .toString());
    }
  }

  /**
   * @param version The runtime version
   *
   * @return {@code true} if the given version is within the current range
   */

  default boolean includes(
    final Runtime.Version version)
  {
    Objects.requireNonNull(version, "version");

    final var lower = this.lower();
    final var upper = this.upper();

    if (this.lowerExclusive()) {
      if (this.upperExclusive()) {
        return version.compareTo(lower) > 0 && version.compareTo(upper) < 0;
      }
      return version.compareTo(lower) > 0 && version.compareTo(upper) <= 0;
    }
    if (this.upperExclusive()) {
      return version.compareTo(lower) >= 0 && version.compareTo(upper) < 0;
    }
    return version.compareTo(lower) >= 0 && version.compareTo(upper) <= 0;
  }
}

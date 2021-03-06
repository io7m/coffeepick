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

import java.util.regex.Pattern;

/**
 * A hash value.
 */

@ImmutablesStyleType
@Value.Immutable
public interface RuntimeHashType
{
  /**
   * A pattern that describes a valid hash value.
   */

  Pattern HASH_PATTERN = Pattern.compile("[a-f0-9]{1,256}");

  /**
   * A pattern that describes a valid algorithm value.
   */

  Pattern ALGORITHM_PATTERN = Pattern.compile("[A-Z0-9\\-]{1,32}");

  /**
   * @return The algorithm name (eg. "SHA-256")
   */

  @Value.Parameter
  String algorithm();

  /**
   * @return The hexadecimal, ASCII-encoded hash value
   */

  @Value.Parameter
  String value();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    final var separator = System.lineSeparator();
    final var value = this.value();
    if (!HASH_PATTERN.matcher(value).matches()) {
      throw new IllegalArgumentException(
        new StringBuilder(128)
          .append("Invalid hash value.")
          .append(separator)
          .append("  Expected: A value matching ")
          .append(HASH_PATTERN.pattern())
          .append(separator)
          .append("  Received: ")
          .append(value)
          .append(separator)
          .toString());
    }

    final var algo = this.algorithm();
    if (!ALGORITHM_PATTERN.matcher(algo).matches()) {
      throw new IllegalArgumentException(
        new StringBuilder(128)
          .append("Invalid hash algorithm.")
          .append(separator)
          .append("  Expected: A value matching ")
          .append(ALGORITHM_PATTERN.pattern())
          .append(separator)
          .append("  Received: ")
          .append(value)
          .append(separator)
          .toString());
    }
  }
}

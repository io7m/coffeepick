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

import com.io7m.immutables.styles.ImmutablesStyleType;
import org.immutables.value.Value;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Optional;

/**
 * A (slightly reduced) version number for Java runtimes. This version
 * represents a subset of the possible data present in Java runtime versions.
 * Specifically, this version number represents just the semantic versioning
 * information, plus a build number.
 *
 * @see "https://openjdk.java.net/jeps/223"
 */

@ImmutablesStyleType
@Value.Immutable
public interface RuntimeVersionType extends Comparable<RuntimeVersionType>
{
  /**
   * @return The major version
   */

  BigInteger major();

  /**
   * @return The minor version
   */

  BigInteger minor();

  /**
   * @return The patch version
   */

  BigInteger patch();

  /**
   * @return The build number, if any
   */

  Optional<BigInteger> build();

  @Override
  default int compareTo(final RuntimeVersionType other)
  {
    return Comparator
      .comparing(RuntimeVersionType::major)
      .thenComparing(RuntimeVersionType::minor)
      .thenComparing(RuntimeVersionType::patch)
      .thenComparing(o -> o.build().orElse(BigInteger.ZERO))
      .compare(this, other);
  }

  /**
   * @return The version as an external string (such as "3.2.1+200")
   */

  default String toExternalString()
  {
    final var builder = new StringBuilder(32);
    builder.append(this.major());
    builder.append('.');
    builder.append(this.minor());
    builder.append('.');
    builder.append(this.patch());
    this.build().ifPresent(b -> {
      builder.append('+');
      builder.append(b);
    });
    return builder.toString();
  }

  /**
   * @return The version as an external string (such as "3.2.1+200")
   */

  default String toExternalMinimalString()
  {
    final var builder = new StringBuilder(32);

    if (this.isBuildNonZero()) {
      return this.toExternalString();
    }

    if (this.isPatchNonZero()) {
      builder.append(this.major());
      builder.append('.');
      builder.append(this.minor());
      builder.append('.');
      builder.append(this.patch());
      return builder.toString();
    }

    if (this.isMinorNonZero()) {
      builder.append(this.major());
      builder.append('.');
      builder.append(this.minor());
      return builder.toString();
    }

    builder.append(this.major());
    return builder.toString();
  }

  private boolean isBuildNonZero()
  {
    return this.build().orElse(BigInteger.ZERO).compareTo(BigInteger.ZERO) > 0;
  }

  private boolean isMinorNonZero()
  {
    return this.minor().compareTo(BigInteger.ZERO) > 0;
  }

  private boolean isPatchNonZero()
  {
    return this.patch().compareTo(BigInteger.ZERO) > 0;
  }
}

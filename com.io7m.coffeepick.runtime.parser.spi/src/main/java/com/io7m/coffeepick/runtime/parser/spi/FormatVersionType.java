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

package com.io7m.coffeepick.runtime.parser.spi;

import com.io7m.immutables.styles.ImmutablesStyleType;
import org.immutables.value.Value;
import org.osgi.annotation.versioning.ProviderType;

import java.util.Objects;

/**
 * A format version.
 */

@ProviderType
@ImmutablesStyleType
@Value.Immutable
public interface FormatVersionType extends Comparable<FormatVersionType>
{
  /**
   * @return The major number for a format
   */

  @Value.Parameter
  int major();

  /**
   * @return The minor number for a format
   */

  @Value.Parameter
  int minor();

  /**
   * @return The humanly-readable version string
   */

  default String toHumanString()
  {
    return new StringBuilder(64)
      .append(Integer.toUnsignedString(this.major()))
      .append(".")
      .append(Integer.toUnsignedString(this.minor()))
      .toString();
  }

  @Override
  default int compareTo(final FormatVersionType o)
  {
    Objects.requireNonNull(o, "Other");

    final var r = Integer.compareUnsigned(this.major(), o.major());
    if (r == 0) {
      return Integer.compareUnsigned(this.minor(), o.minor());
    }
    return r;
  }
}

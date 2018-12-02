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

import java.util.Optional;

/**
 * The result of probing.
 */

public interface SPIProbeResultType
{
  /**
   * @return The kind of result
   */

  Kind kind();

  /**
   * The kind of result.
   */

  enum Kind
  {
    /**
     * Probing succeeded; the provider supports the probed format.
     */

    PROBE_SUCCESS,

    /**
     * Probing failed; the provider does not support the probed format (or the format could not be
     * determined at all).
     */

    PROBE_FAILURE
  }

  /**
   * Probing succeeded.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface SPIProbeSuccessType extends SPIProbeResultType
  {
    @Override
    default Kind kind()
    {
      return Kind.PROBE_SUCCESS;
    }

    /**
     * @return The format description
     */

    FormatDescription format();

    /**
     * @return The format version
     */

    FormatVersion version();
  }

  /**
   * Probing failed.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface SPIProbeFailureType extends SPIProbeResultType
  {
    @Override
    default Kind kind()
    {
      return Kind.PROBE_FAILURE;
    }

    /**
     * @return The exception encountered, if any
     */

    Optional<Exception> error();
  }
}

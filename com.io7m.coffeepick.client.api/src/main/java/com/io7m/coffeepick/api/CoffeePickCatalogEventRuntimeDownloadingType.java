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

package com.io7m.coffeepick.api;

import com.io7m.immutables.styles.ImmutablesStyleType;
import org.immutables.value.Value;

/**
 * A download has started.
 */

@ImmutablesStyleType
@Value.Immutable
public interface CoffeePickCatalogEventRuntimeDownloadingType extends CoffeePickCatalogEventType
{
  @Override
  default Severity severity()
  {
    return Severity.INFO;
  }

  /**
   * @return The ID of the runtime
   */

  @Value.Parameter
  String id();

  /**
   * @return The current progress in the range [0.0, 1.0]
   */

  default double progress()
  {
    final var received = (double) this.received();
    final var expected = (double) this.expected();
    return received / expected;
  }

  /**
   * @return The number of octets being received per second
   */

  @Value.Parameter
  double octetsPerSecond();

  /**
   * @return The number of octets expected
   */

  @Value.Parameter
  long expected();

  /**
   * @return The number of octets that have been received
   */

  @Value.Parameter
  long received();
}

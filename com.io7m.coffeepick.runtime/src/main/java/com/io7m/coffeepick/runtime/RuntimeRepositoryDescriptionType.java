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

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A repository of runtimes.
 */

@ImmutablesStyleType
@Value.Immutable
public interface RuntimeRepositoryDescriptionType
{
  /**
   * @return The unique ID of the repository
   */

  URI id();

  /**
   * @return The time of the last update of the repository
   */

  Optional<ZonedDateTime> updated();

  /**
   * @return The runtimes available in the repository
   */

  Map<String, RuntimeDescription> descriptions();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    this.descriptions()
      .values()
      .forEach(description -> {
        if (!Objects.equals(description.repository(), this.id())) {
          final var separator = System.lineSeparator();
          throw new IllegalArgumentException(
            new StringBuilder(128)
              .append("All runtimes must have the correct repository ID")
              .append(separator)
              .append("  Expected: ")
              .append(this.id())
              .append(separator)
              .append("  Received: ")
              .append(description.repository())
              .append(separator)
              .toString());
        }
      });
  }
}

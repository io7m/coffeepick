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
import java.util.Set;

/**
 * A description of a runtime.
 */

@ImmutablesStyleType
@Value.Immutable
public interface RuntimeDescriptionType
{
  /**
   * @return The repository to which this runtime belongs
   */

  URI repository();

  /**
   * @return The runtime version
   */

  Runtime.Version version();

  /**
   * @return The name of the platform upon which this runtime will run
   *
   * @see RuntimePlatforms
   */

  String platform();

  /**
   * @return The name of the architecture upon which this runtime will run
   *
   * @see RuntimeArchitectures
   */

  String architecture();

  /**
   * @return A URI that can be used to fetch an archive of the runtime
   */

  URI archiveURI();

  /**
   * @return The size in octets of the archive
   */

  long archiveSize();

  /**
   * @return The hash of the runtime
   */

  RuntimeHash archiveHash();

  /**
   * @return The name of the VM used in the runtime
   *
   * @see RuntimeVMs
   */

  String vm();

  /**
   * @return A set of arbitrary tags added to the runtime
   */

  @Value.Default
  default Set<String> tags()
  {
    return Set.of();
  }

  /**
   * @return A unique identifier for this runtime
   */

  default String id()
  {
    return this.archiveHash().value();
  }
}

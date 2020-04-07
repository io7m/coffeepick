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

import com.io7m.coffeepick.runtime.RuntimeConfiguration;
import com.io7m.coffeepick.runtime.RuntimeHash;
import com.io7m.coffeepick.runtime.RuntimeVersionRange;
import com.io7m.immutables.styles.ImmutablesStyleType;
import org.immutables.value.Value;

import java.net.URI;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Inventory search parameters.
 */

@ImmutablesStyleType
@Value.Immutable
public interface CoffeePickSearchType
{
  /**
   * @return The repository URI
   */

  Optional<String> repository();

  /**
   * @return The range of acceptable runtime versions
   */

  Optional<RuntimeVersionRange> versionRange();

  /**
   * @return The name of the platform upon which this runtime will run
   */

  Optional<String> platform();

  /**
   * @return The name of the architecture upon which this runtime will run
   */

  Optional<String> architecture();

  /**
   * @return The name of the VM used for this runtime
   */

  Optional<String> vm();

  /**
   * @return The configuration of the runtime
   */

  Optional<RuntimeConfiguration> configuration();

  /**
   * @return A URI that can be used to fetch an archive of the runtime
   */

  Optional<URI> archiveURI();

  /**
   * @return The size in octets of the archive
   */

  OptionalLong archiveSize();

  /**
   * @return The hash of the runtime
   */

  Optional<RuntimeHash> archiveHash();

  /**
   * @return The unique identifier of the runtime
   */

  Optional<String> id();

  /**
   * @return A set of tags that must be present on a given runtime
   */

  @Value.Default
  default Set<String> requiredTags()
  {
    return Set.of();
  }
}

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

import java.net.URI;

/**
 * A descriptions of a supported format.
 */

@ProviderType
@ImmutablesStyleType
@Value.Immutable
public interface FormatDescriptionType
{
  /**
   * @return A human-readable descriptions of the format
   */

  String description();

  /**
   * @return The mime type for the format
   */

  String mimeType();

  /**
   * @return The unique name for the format
   */

  URI name();
}

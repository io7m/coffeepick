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

import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimeRepositoryDescription;
import com.io7m.immutables.styles.ImmutablesStyleType;
import org.immutables.value.Value;

/**
 * The type of parser results.
 */

public interface ParserResultType
{
  /**
   * @return The kind of parsed value
   */

  Kind kind();

  /**
   * The kind of parsed value.
   */

  enum Kind
  {

    /**
     * A repository was parsed.
     */

    REPOSITORY,

    /**
     * A runtime was parsed.
     */

    RUNTIME
  }

  /**
   * A parsed repository
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface ParsedRepositoryType extends ParserResultType
  {
    @Override
    default Kind kind()
    {
      return Kind.REPOSITORY;
    }

    /**
     * @return The actual repository value
     */

    @Value.Parameter
    RuntimeRepositoryDescription repository();
  }

  /**
   * A parsed runtime
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface ParsedRuntimeType extends ParserResultType
  {
    @Override
    default Kind kind()
    {
      return Kind.RUNTIME;
    }

    /**
     * @return The actual runtime value
     */

    @Value.Parameter
    RuntimeDescription runtime();
  }
}

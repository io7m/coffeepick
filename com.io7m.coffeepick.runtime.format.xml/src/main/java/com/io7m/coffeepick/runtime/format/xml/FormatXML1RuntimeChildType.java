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

package com.io7m.coffeepick.runtime.format.xml;

import com.io7m.coffeepick.runtime.RuntimeBuild;
import com.io7m.coffeepick.runtime.RuntimeHash;
import com.io7m.immutables.styles.ImmutablesStyleType;
import org.immutables.value.Value;

import java.util.Set;

/**
 * The type of elements that can appear as children of runtimes.
 */

public interface FormatXML1RuntimeChildType
{
  /**
   * @return The kind of child
   */

  Kind kind();

  /**
   * The kind of children
   */

  enum Kind
  {
    /**
     * A tags element
     */

    TAGS,

    /**
     * A hash element
     */

    HASH,

    /**
     * A build element.
     */

    BUILD
  }

  /**
   * The tags element.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface FormatXML1RuntimeChildTagsType extends FormatXML1RuntimeChildType
  {
    @Override
    default Kind kind()
    {
      return Kind.TAGS;
    }

    /**
     * @return The set of tags
     */

    @Value.Parameter
    Set<String> tags();
  }

  /**
   * The hash element.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface FormatXML1RuntimeChildHashType extends FormatXML1RuntimeChildType
  {
    @Override
    default Kind kind()
    {
      return Kind.HASH;
    }

    /**
     * @return The hash value
     */

    @Value.Parameter
    RuntimeHash hash();
  }

  /**
   * The build element.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface FormatXML1RuntimeChildBuildType extends FormatXML1RuntimeChildType
  {
    @Override
    default Kind kind()
    {
      return Kind.BUILD;
    }

    /**
     * @return The build information
     */

    @Value.Parameter
    RuntimeBuild build();
  }
}

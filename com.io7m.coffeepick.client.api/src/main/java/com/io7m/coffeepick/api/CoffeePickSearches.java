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

import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimeVersion;
import com.io7m.coffeepick.runtime.RuntimeVersionRange;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Functions to implement searches over runtimes.
 */

public final class CoffeePickSearches
{
  private CoffeePickSearches()
  {

  }

  private static <T> boolean matchesField(
    final T field,
    final Optional<T> matcher)
  {
    return matcher.map(match -> Boolean.valueOf(Objects.equals(field, match)))
      .orElse(Boolean.TRUE)
      .booleanValue();
  }

  private static <T> boolean matchesFieldInexact(
    final String field,
    final Optional<String> matcher)
  {
    return matcher.map(match -> Boolean.valueOf(
      field.toUpperCase(Locale.ROOT).contains(match.toUpperCase(Locale.ROOT))))
      .orElse(Boolean.TRUE)
      .booleanValue();
  }

  /**
   * @param runtime    The runtime runtimes
   * @param parameters The search parameters
   *
   * @return {@code true} if the given runtime is matched by the given search parameters
   */

  public static boolean matchesExact(
    final RuntimeDescription runtime,
    final CoffeePickSearch parameters)
  {
    Objects.requireNonNull(runtime, "runtime");
    Objects.requireNonNull(parameters, "parameters");

    return matchesArchive(runtime, parameters)
      && matchesExactRuntime(runtime, parameters);
  }

  /**
   * @param runtime    The runtime runtimes
   * @param parameters The search parameters
   *
   * @return {@code true} if the given runtime is matched by the given search parameters
   */

  public static boolean matchesInexact(
    final RuntimeDescription runtime,
    final CoffeePickSearch parameters)
  {
    Objects.requireNonNull(runtime, "runtime");
    Objects.requireNonNull(parameters, "parameters");

    return matchesArchive(runtime, parameters)
      && matchesInexactRuntime(runtime, parameters);
  }

  // CHECKSTYLE:OFF
  private static boolean matchesInexactRuntime(
    final RuntimeDescription runtime,
    final CoffeePickSearch parameters)
  {
    return matchesFieldInexact(runtime.repository().toString(), parameters.repository())
      && matchesField(runtime.configuration(), parameters.configuration())
      && matchesFieldInexact(runtime.id(), parameters.id())
      && matchesFieldInexact(runtime.platform(), parameters.platform())
      && matchesFieldInexact(runtime.architecture(), parameters.architecture())
      && matchesFieldInexact(runtime.vm(), parameters.vm())
      && matchesTags(runtime.tags(), parameters.requiredTags())
      && matchesVersionRange(runtime.version(), parameters.versionRange());
  }
  // CHECKSTYLE:ON

  // CHECKSTYLE:OFF
  private static boolean matchesExactRuntime(
    final RuntimeDescription runtime,
    final CoffeePickSearch parameters)
  {
    return matchesField(runtime.repository().toString(), parameters.repository())
      && matchesField(runtime.configuration(), parameters.configuration())
      && matchesField(runtime.id(), parameters.id())
      && matchesField(runtime.platform(), parameters.platform())
      && matchesField(runtime.architecture(), parameters.architecture())
      && matchesField(runtime.vm(), parameters.vm())
      && matchesTags(runtime.tags(), parameters.requiredTags())
      && matchesVersionRange(runtime.version(), parameters.versionRange());
  }
  // CHECKSTYLE:ON

  private static boolean matchesArchive(
    final RuntimeDescription runtime,
    final CoffeePickSearch parameters)
  {
    return matchesField(runtime.archiveHash(), parameters.archiveHash())
      && matchesField(runtime.archiveURI(), parameters.archiveURI())
      && matchesArchiveSize(runtime.archiveSize(), parameters.archiveSize());
  }

  private static boolean matchesTags(
    final Set<String> receivedTags,
    final Set<String> requiredTags)
  {
    return receivedTags.containsAll(requiredTags);
  }

  private static boolean matchesArchiveSize(
    final long size,
    final OptionalLong match)
  {
    if (match.isPresent()) {
      return size == match.getAsLong();
    }
    return true;
  }

  private static boolean matchesVersionRange(
    final RuntimeVersion version,
    final Optional<RuntimeVersionRange> range)
  {
    return range.map(vRange -> Boolean.valueOf(vRange.includes(version)))
      .orElse(Boolean.TRUE).booleanValue();
  }
}

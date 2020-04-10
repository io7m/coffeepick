/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> http://io7m.com
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

package com.io7m.coffeepick.tests.runtime;

import com.io7m.coffeepick.runtime.RuntimeVersions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

public final class RuntimeVersionTest
{
  private static void badString(final String input)
  {
    final var ex = Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> {
        RuntimeVersions.parse(input);
      });

    Assertions.assertTrue(ex.getMessage().contains(input));
  }

  @Test
  public void testVersionMinimalString0()
    throws IOException
  {
    final var version = RuntimeVersions.parse("3.0.0+0");
    Assertions.assertEquals("3", version.toExternalMinimalString());
  }

  @Test
  public void testVersionMinimalString1()
    throws IOException
  {
    final var version = RuntimeVersions.parse("3.1.0+0");
    Assertions.assertEquals("3.1", version.toExternalMinimalString());
  }

  @Test
  public void testVersionMinimalString2()
    throws IOException
  {
    final var version = RuntimeVersions.parse("3.0.1+0");
    Assertions.assertEquals("3.0.1", version.toExternalMinimalString());
  }

  @Test
  public void testVersionMinimalString3()
    throws IOException
  {
    final var version = RuntimeVersions.parse("3.0.1+1");
    Assertions.assertEquals("3.0.1+1", version.toExternalMinimalString());
  }

  @Test
  public void testVersion0()
    throws IOException
  {
    final var version = RuntimeVersions.parse("3.2.1+20");
    Assertions.assertEquals(3, version.major().intValue());
    Assertions.assertEquals(2, version.minor().intValue());
    Assertions.assertEquals(1, version.patch().intValue());
    Assertions.assertEquals(20, version.build().get().intValue());
    Assertions.assertEquals("3.2.1+20", version.toExternalString());
  }

  @Test
  public void testVersion1()
    throws IOException
  {
    final var version = RuntimeVersions.parse("3.2.1");
    Assertions.assertEquals(3, version.major().intValue());
    Assertions.assertEquals(2, version.minor().intValue());
    Assertions.assertEquals(1, version.patch().intValue());
    Assertions.assertEquals(Optional.empty(), version.build());
    Assertions.assertEquals("3.2.1", version.toExternalString());
  }

  @TestFactory
  public Stream<DynamicTest> testVersionsBad()
    throws IOException
  {
    return Stream.of(
      "bad",
      "1.2.3.4")
      .map(input ->
             DynamicTest.dynamicTest(
               String.format("testVersionsBad%s", input),
               () -> badString(input))
      );
  }
}

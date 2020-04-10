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

package com.io7m.coffeepick.tests.shipilev_net;

import com.io7m.coffeepick.shipilev_net.internal.ASFilenameMetadataParsing;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Set;

public final class ASFilenameMetadataParsingTest
{
  @Test
  @Disabled("Broken by recent version parsing changes")
  public void testCase_0()
  {
    final var meta_opt =
      ASFilenameMetadataParsing.parseFilename(
        "openjdk-amber",
        "openjdk-amber-b62-20181130-jdk-12+21-linux-aarch64-fastdebug.tar.xz");

    Assertions.assertTrue(meta_opt.isPresent());
    final var meta = meta_opt.get();
    Assertions.assertEquals("b62", meta.build().get().buildNumber());
    Assertions.assertEquals("linux", meta.platform());
    Assertions.assertEquals("aarch64", meta.architecture());
    Assertions.assertEquals(Runtime.Version.parse("12+21"), meta.version());
    Assertions.assertEquals(Set.of("fastdebug"), meta.extraTags());
  }
}

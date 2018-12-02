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

import com.io7m.coffeepick.runtime.RuntimeArchitectures;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimePlatforms;
import com.io7m.coffeepick.shipilev_net.ASArchiveResolver;
import com.io7m.coffeepick.shipilev_net.ASFileList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.util.Objects;

public final class ASArchiveResolverTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(ASArchiveResolverTest.class);

  private static void isRecognizedArchitecture(
    final RuntimeDescription description)
  {
    for (final var arch : RuntimeArchitectures.values()) {
      if (Objects.equals(arch.architectureName(), description.architecture())) {
        return;
      }
    }
    throw new IllegalArgumentException("Unrecognized arch: " + description.architecture());
  }

  private static void isRecognizedPlatform(
    final RuntimeDescription description)
  {
    for (final var arch : RuntimePlatforms.values()) {
      if (Objects.equals(arch.platformName(), description.platform())) {
        return;
      }
    }
    throw new IllegalArgumentException("Unrecognized platform: " + description.platform());
  }

  @Test
  public void testResolve()
    throws Exception
  {
    final var client =
      HttpClient.newBuilder().build();
    final var names =
      ASFileList.fetch(client);
    final var runtimes =
      ASArchiveResolver.create(client)
        .resolve(names);

    Assertions.assertTrue(runtimes.size() > 250);
    Assertions.assertAll(
      runtimes.stream()
        .map(description -> () -> {
          isRecognizedArchitecture(description);
          isRecognizedPlatform(description);
          return;
        }));
  }
}

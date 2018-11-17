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

package com.io7m.coffeepick.tests;

import com.io7m.coffeepick.jdk_java_net.OJNRepositoryProvider;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryContextType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;

import static com.io7m.coffeepick.runtime.RuntimePlatforms.PLATFORM_LINUX;
import static com.io7m.coffeepick.runtime.RuntimePlatforms.PLATFORM_MACOS;
import static com.io7m.coffeepick.runtime.RuntimePlatforms.PLATFORM_WINDOWS;

/**
 * Tests for the OpenJDK repository provider.
 */

public final class OJNRepositoryTests
{
  private static final Logger LOG = LoggerFactory.getLogger(OJNRepositoryTests.class);

  @Test
  public void testRuntimes()
  {
    final var context = Mockito.mock(RuntimeRepositoryContextType.class);
    final var provider = new OJNRepositoryProvider();

    Assertions.assertTimeout(Duration.ofSeconds(60L), () -> {
      final var repository = provider.openRepository(context);
      final var runtimes = repository.runtimes();
      runtimes.values().forEach(description -> LOG.debug("runtime: {}", description));

      Assertions.assertEquals(8, runtimes.size());
      Assertions.assertTrue(
        runtimes.values().stream()
          .anyMatch(run -> Objects.equals(run.platform(), PLATFORM_LINUX.platformName())),
        "Linux present");
      Assertions.assertTrue(
        runtimes.values().stream()
          .anyMatch(run -> Objects.equals(run.platform(), PLATFORM_MACOS.platformName())),
        "MacOS present");
      Assertions.assertTrue(
        runtimes.values().stream()
          .anyMatch(run -> Objects.equals(run.platform(), PLATFORM_WINDOWS.platformName())),
        "Windows present");
    });
  }
}

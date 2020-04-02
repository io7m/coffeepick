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

package com.io7m.coffeepick.tests.adoptopenjdk_v3;

import com.io7m.coffeepick.adoptopenjdk_v3.AOJRepositoryProvider;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryContextType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventType;
import com.io7m.coffeepick.tests.TestDirectories;
import io.reactivex.disposables.Disposable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

public final class AOJRepositoryProviderTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(AOJRepositoryProviderTest.class);

  private RuntimeRepositoryContextType context;
  private Path directory;
  private ArrayList<RuntimeRepositoryEventType> events;
  private Disposable subscription;

  @BeforeEach
  public void testSetup()
    throws IOException
  {
    this.directory =
      TestDirectories.createTempDirectory();
    this.events =
      new ArrayList<>();

    this.context =
      Mockito.mock(RuntimeRepositoryContextType.class);
    Mockito.when(this.context.cacheDirectory())
      .thenReturn(this.directory.resolve("cache"));
  }

  @AfterEach
  public void testTeardown()
  {
    this.subscription.dispose();
  }

  @Test
  public void testOpen()
    throws Exception
  {
    final var provider =
      new AOJRepositoryProvider();
    final var repository =
      provider.openRepository(this.context);

    this.subscription = repository.events().subscribe(this::logEvent);
    repository.update(() -> false);

    Assertions.assertTrue(repository.runtimes().size() > 60);
  }

  private void logEvent(
    final RuntimeRepositoryEventType e)
  {
    LOG.debug("event: {}", e);
    this.events.add(e);
  }
}

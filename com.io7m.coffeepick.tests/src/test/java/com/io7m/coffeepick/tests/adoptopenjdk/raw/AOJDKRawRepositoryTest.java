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

package com.io7m.coffeepick.tests.adoptopenjdk.raw;

import com.io7m.coffeepick.adoptopenjdk.raw.AOJDKRawRepositoryProvider;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryContextType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;

public final class AOJDKRawRepositoryTest
{
  private Path directory;

  @BeforeEach
  public final void setup()
    throws IOException
  {
    this.directory = Files.createTempDirectory("aojdk-");
  }

  @Test
  public void testUpdate()
    throws Exception
  {
    final var context = Mockito.mock(RuntimeRepositoryContextType.class);
    Mockito.when(context.httpClient())
      .thenReturn(HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build());
    Mockito.when(context.cacheDirectory())
      .thenReturn(this.directory);

    final var provider = new AOJDKRawRepositoryProvider();
    final var repository = provider.openRepository(context);
    repository.update(() -> false);
  }

  @Test
  public void testUpdateCancelled()
    throws Exception
  {
    final var context = Mockito.mock(RuntimeRepositoryContextType.class);
    Mockito.when(context.httpClient())
      .thenReturn(HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build());
    Mockito.when(context.cacheDirectory())
      .thenReturn(this.directory);

    final var provider = new AOJDKRawRepositoryProvider();
    final var repository = provider.openRepository(context);

    Assertions.assertThrows(CancellationException.class, () -> {
      repository.update(() -> true);
    });
  }
}

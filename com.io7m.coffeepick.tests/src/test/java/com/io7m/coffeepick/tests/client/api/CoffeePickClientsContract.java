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

package com.io7m.coffeepick.tests.client.api;

import com.io7m.coffeepick.api.CoffeePickClientProviderType;
import com.io7m.coffeepick.api.CoffeePickSearch;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderRegistryEventType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderRegistryType;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

public abstract class CoffeePickClientsContract
{
  private PublishSubject<RuntimeRepositoryProviderRegistryEventType> repos_events;

  protected abstract Logger log();

  protected abstract CoffeePickClientProviderType provider(
    RuntimeRepositoryProviderRegistryType repositories);

  @BeforeEach
  public void setup()
  {
    this.repos_events = PublishSubject.create();
  }

  @Test
  public final void testCreateNotInventoryDirectory()
    throws Exception
  {
    final var registry = Mockito.mock(RuntimeRepositoryProviderRegistryType.class);

    Mockito.when(registry.events()).thenReturn(this.repos_events);

    final var clients = this.provider(registry);
    final var tmp = Files.createTempDirectory("coffeepick-");

    Files.writeString(
      tmp.resolve("inventory"),
      "",
      StandardCharsets.US_ASCII,
      StandardOpenOption.CREATE_NEW);

    Assertions.assertThrows(IOException.class, () -> clients.newClient(tmp));
  }

  @Test
  public final void testCreateEmpty()
    throws Exception
  {
    final var registry = Mockito.mock(RuntimeRepositoryProviderRegistryType.class);

    Mockito.when(registry.events()).thenReturn(this.repos_events);

    final var clients = this.provider(registry);
    final var tmp = Files.createTempDirectory("coffeepick-");
    final var client = clients.newClient(tmp);

    final var op =
      client.inventorySearch(
        CoffeePickSearch.builder()
          .build());

    final var runtimes = op.get(60L, TimeUnit.SECONDS);
    Assertions.assertEquals(0L, runtimes.size());
  }

  @Test
  public final void testDeleteEmpty()
    throws Exception
  {
    final var registry = Mockito.mock(RuntimeRepositoryProviderRegistryType.class);

    Mockito.when(registry.events()).thenReturn(this.repos_events);

    final var clients = this.provider(registry);
    final var tmp = Files.createTempDirectory("coffeepick-");
    final var client = clients.newClient(tmp);

    final var op =
      client.inventoryDelete("abcd");

    final var runtimes = op.get(60L, TimeUnit.SECONDS);
  }
}

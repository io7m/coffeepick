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

import com.io7m.coffeepick.api.CoffeePickCatalogEventType;
import com.io7m.coffeepick.api.CoffeePickCatalogType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryContextType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderRegistryEvent;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderRegistryEventType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderRegistryType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryType;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimeHash;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import static com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderRegistryEventType.Change.ADDED;
import static com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderRegistryEventType.Change.REMOVED;

public abstract class CoffeePickCatalogContract
{
  private ArrayList<CoffeePickCatalogEventType> event_log;
  private PublishSubject<CoffeePickCatalogEventType> events;

  protected abstract Logger logger();

  protected abstract CoffeePickCatalogType catalog(
    Subject<CoffeePickCatalogEventType> events,
    RuntimeRepositoryContextType context,
    RuntimeRepositoryProviderRegistryType repositories);

  @BeforeEach
  public final void setup()
  {
    this.event_log = new ArrayList<>();
    this.events = PublishSubject.create();
    this.events.subscribe(e -> {
      this.logger().trace("event: {}", e);
      this.event_log.add(e);
    });
  }

  @Test
  public final void testEmpty()
  {
    final var repo_events =
      PublishSubject.<RuntimeRepositoryProviderRegistryEventType>create();

    final var context = Mockito.mock(RuntimeRepositoryContextType.class);
    final var repositories = Mockito.mock(RuntimeRepositoryProviderRegistryType.class);
    Mockito.when(repositories.events()).thenReturn(repo_events);

    final var catalog = this.catalog(this.events, context, repositories);
    Assertions.assertEquals(0L, (long) catalog.searchAll().size());
  }

  @Test
  public final void testRepositoryAdded()
    throws IOException
  {
    final var repo_events =
      PublishSubject.<RuntimeRepositoryProviderRegistryEventType>create();

    final var context = Mockito.mock(RuntimeRepositoryContextType.class);
    final var repositories = Mockito.mock(RuntimeRepositoryProviderRegistryType.class);
    Mockito.when(repositories.events()).thenReturn(repo_events);

    final var description =
      RuntimeDescription.builder()
        .setRepository(URI.create("urn:example"))
        .setArchitecture("x64")
        .setArchiveHash(RuntimeHash.of("SHA-256", "abcd"))
        .setArchiveSize(100L)
        .setArchiveURI(URI.create("https://www.example.com"))
        .setPlatform("linux")
        .setVersion(Runtime.Version.parse("11.0.1"))
        .setVm("hotspot")
        .build();

    final var provider = Mockito.mock(RuntimeRepositoryProviderType.class);
    final var repository = Mockito.mock(RuntimeRepositoryType.class);

    Mockito.when(provider.uri()).thenReturn(URI.create("urn:example:0.0"));
    Mockito.when(provider.openRepository(context)).thenReturn(repository);

    Mockito.when(repository.provider()).thenReturn(provider);
    Mockito.when(repository.runtimes()).thenReturn(Map.of(description.id(), description));

    final var catalog = this.catalog(this.events, context, repositories);
    repo_events.onNext(RuntimeRepositoryProviderRegistryEvent.of(ADDED, provider));

    Assertions.assertEquals(1L, (long) catalog.searchAll().size());
    Assertions.assertEquals(description, catalog.searchExact(description.id()).get());
  }

  @Test
  public final void testRepositoryAddedRemoved()
    throws IOException
  {
    final var repo_events = PublishSubject.<RuntimeRepositoryProviderRegistryEventType>create();

    final var context = Mockito.mock(RuntimeRepositoryContextType.class);
    final var repositories = Mockito.mock(RuntimeRepositoryProviderRegistryType.class);
    Mockito.when(repositories.events()).thenReturn(repo_events);

    final var description =
      RuntimeDescription.builder()
        .setRepository(URI.create("urn:example"))
        .setArchitecture("x64")
        .setArchiveHash(RuntimeHash.of("SHA-256", "abcd"))
        .setArchiveSize(100L)
        .setArchiveURI(URI.create("https://www.example.com"))
        .setPlatform("linux")
        .setVersion(Runtime.Version.parse("11.0.1"))
        .setVm("hotspot")
        .build();

    final var provider = Mockito.mock(RuntimeRepositoryProviderType.class);
    Mockito.when(provider.uri()).thenReturn(URI.create("urn:example:0.0"));

    final var repository = Mockito.mock(RuntimeRepositoryType.class);
    Mockito.when(repository.provider()).thenReturn(provider);
    Mockito.when(repository.runtimes()).thenReturn(Map.of(description.id(), description));

    final var catalog = this.catalog(this.events, context, repositories);
    repo_events.onNext(RuntimeRepositoryProviderRegistryEvent.of(ADDED, provider));
    repo_events.onNext(RuntimeRepositoryProviderRegistryEvent.of(REMOVED, provider));
    Assertions.assertEquals(0L, (long) catalog.searchAll().size());
  }
}

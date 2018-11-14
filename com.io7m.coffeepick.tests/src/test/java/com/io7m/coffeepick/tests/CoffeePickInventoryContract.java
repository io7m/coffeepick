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

import com.io7m.coffeepick.api.CoffeePickInventoryEventRuntimeDeleted;
import com.io7m.coffeepick.api.CoffeePickInventoryEventRuntimeLoadFailed;
import com.io7m.coffeepick.api.CoffeePickInventoryEventRuntimeLoaded;
import com.io7m.coffeepick.api.CoffeePickInventoryEventType;
import com.io7m.coffeepick.api.CoffeePickInventoryType;
import com.io7m.coffeepick.api.CoffeePickSearch;
import com.io7m.coffeepick.runtime.RuntimeConfiguration;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimeHash;
import com.io7m.coffeepick.runtime.RuntimeVersionRange;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Inventory contract.
 */

public abstract class CoffeePickInventoryContract
{
  public static final String HASH_VALUE =
    "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

  private Path directory;
  private ArrayList<CoffeePickInventoryEventType> event_log;
  private PublishSubject<CoffeePickInventoryEventType> events;

  private static String hashOf(final String text)
  {
    try {
      final var digest = MessageDigest.getInstance("SHA-256");
      return Hex.encodeHexString(digest.digest(text.getBytes(UTF_8)), true);
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  protected abstract Logger logger();

  protected abstract CoffeePickInventoryType inventory(
    Subject<CoffeePickInventoryEventType> events,
    Path path)
    throws IOException;

  @BeforeEach
  public final void setup()
    throws IOException
  {
    this.directory = Files.createTempDirectory("coffee-pick-inventory-");
    this.event_log = new ArrayList<>();
    this.events = PublishSubject.create();
    this.events.subscribe(e -> {
      this.logger().trace("event: {}", e);
      this.event_log.add(e);
    });
  }

  @Test
  public final void testEmpty()
    throws Exception
  {
    final var inventory = this.inventory(this.events, this.directory);
    final var results = inventory.search(CoffeePickSearch.builder().build());
    Assertions.assertEquals(0L, (long) results.size());
    Assertions.assertEquals(0L, (long) this.event_log.size());
  }

  @Test
  public final void testWrite()
    throws Exception
  {
    final var inventory = this.inventory(this.events, this.directory);

    final var description =
      RuntimeDescription.builder()
        .setRepository(URI.create("urn:example"))
        .setArchitecture("x64")
        .setArchiveHash(RuntimeHash.of("SHA-256", HASH_VALUE))
        .setArchiveSize(100L)
        .setArchiveURI(URI.create("https://www.example.com"))
        .setConfiguration(RuntimeConfiguration.JDK)
        .setPlatform("linux")
        .setVersion(Runtime.Version.parse("11.0.1"))
        .setVm("hotspot")
        .build();

    inventory.write(description, stream -> stream.write("hello".getBytes(UTF_8)));

    final var results = inventory.search(CoffeePickSearch.builder().build());
    Assertions.assertEquals(1L, (long) results.size());
    Assertions.assertTrue(results.containsKey(HASH_VALUE));

    Assertions.assertEquals(1L, (long) this.event_log.size());
    final var event = this.eventFor(CoffeePickInventoryEventRuntimeLoaded.class, 0);
    Assertions.assertEquals(HASH_VALUE, event.id());
  }

  @Test
  public final void testWriteSearchMatches()
    throws Exception
  {
    final var inventory = this.inventory(this.events, this.directory);

    final var descriptions =
      IntStream.rangeClosed(8, 11)
        .mapToObj(major -> IntStream.rangeClosed(0, 9)
          .mapToObj(minor -> {
            var version_name =
              String.format("%d+%d", Integer.valueOf(major), Integer.valueOf(minor));

            return RuntimeDescription.builder()
              .setRepository(URI.create("urn:example"))
              .setArchitecture("x64")
              .setArchiveHash(RuntimeHash.of("SHA-256",  hashOf(version_name)))
              .setArchiveSize(100L)
              .setArchiveURI(URI.create("https://www.example.com"))
              .setConfiguration(RuntimeConfiguration.JDK)
              .setPlatform("linux")
              .setVersion(Runtime.Version.parse(version_name))
              .setVm("hotspot")
              .build();
          })
          .collect(Collectors.toList()))
        .flatMap(List::stream)
        .collect(Collectors.toList());

    for (final var description : descriptions) {
      inventory.write(
        description,
        stream -> stream.write(description.version().toString().getBytes(UTF_8)));
    }

    final var results =
      inventory.search(
        CoffeePickSearch.builder()
          .setArchitecture("x64")
          .setArchiveSize(100L)
          .setArchiveURI(URI.create("https://www.example.com"))
          .setPlatform("linux")
          .setVersionRange(
            RuntimeVersionRange.builder()
              .setLower(Runtime.Version.parse("8+0"))
              .setLowerExclusive(false)
              .setUpper(Runtime.Version.parse("12+0"))
              .setUpperExclusive(true)
              .build())
          .setVm("hotspot")
          .build());

    Assertions.assertEquals(descriptions.size(), results.size());

    for (final var description : descriptions) {
      Assertions.assertTrue(results.containsKey(description.id()));
    }

    for (final var description : descriptions) {
      Assertions.assertEquals(description, inventory.searchExact(description.id()).get());
    }

    Assertions.assertEquals(40L, (long) this.event_log.size());

    final var remaining = new HashSet<>(results.keySet());
    for (var index = 0; index < 40; ++index) {
      final var event = this.eventFor(CoffeePickInventoryEventRuntimeLoaded.class, index);
      remaining.remove(event.id());
    }

    Assertions.assertEquals(0L, (long) remaining.size());
  }

  @Test
  public final void testWriteDelete()
    throws Exception
  {
    final var inventory = this.inventory(this.events, this.directory);

    final var descriptions =
      IntStream.rangeClosed(8, 11)
        .mapToObj(major -> IntStream.rangeClosed(0, 9)
          .mapToObj(minor -> {
            var version_name =
              String.format("%d+%d", Integer.valueOf(major), Integer.valueOf(minor));

            return RuntimeDescription.builder()
              .setRepository(URI.create("urn:example"))
              .setArchitecture("x64")
              .setArchiveHash(RuntimeHash.of("SHA-256",  hashOf(version_name)))
              .setArchiveSize(100L)
              .setArchiveURI(URI.create("https://www.example.com"))
              .setConfiguration(RuntimeConfiguration.JDK)
              .setPlatform("linux")
              .setVersion(Runtime.Version.parse(version_name))
              .setVm("hotspot")
              .build();
          })
          .collect(Collectors.toList()))
        .flatMap(List::stream)
        .collect(Collectors.toList());

    for (final var description : descriptions) {
      inventory.write(description, stream -> {
        stream.write(description.version().toString().getBytes(UTF_8));
      });
    }

    for (final var description : descriptions) {
      inventory.delete(description.id());
    }

    final var results =
      inventory.search(CoffeePickSearch.builder().build());

    Assertions.assertEquals(0, results.size());
    Assertions.assertEquals(80L, (long) this.event_log.size());

    {
      final var remaining = new HashSet<>(results.keySet());
      for (var index = 0; index < 40; ++index) {
        final var event = this.eventFor(CoffeePickInventoryEventRuntimeLoaded.class, index);
        remaining.remove(event.id());
      }
      Assertions.assertEquals(0L, (long) remaining.size());
    }

    {
      final var remaining = new HashSet<>(results.keySet());
      for (var index = 40; index < 80; ++index) {
        final var event = this.eventFor(CoffeePickInventoryEventRuntimeDeleted.class, index);
        remaining.remove(event.id());
      }
      Assertions.assertEquals(0L, (long) remaining.size());
    }
  }

  @Test
  public final void testWriteSearchDoesNotMatchVersion()
    throws Exception
  {
    final var inventory = this.inventory(this.events, this.directory);

    final var description =
      RuntimeDescription.builder()
        .setRepository(URI.create("urn:example"))
        .setArchitecture("x64")
        .setArchiveHash(RuntimeHash.of("SHA-256", HASH_VALUE))
        .setArchiveSize(100L)
        .setArchiveURI(URI.create("https://www.example.com"))
        .setConfiguration(RuntimeConfiguration.JDK)
        .setPlatform("linux")
        .setVersion(Runtime.Version.parse("11.0.1"))
        .setVm("hotspot")
        .build();

    inventory.write(description, stream -> stream.write("hello".getBytes(UTF_8)));

    final var results =
      inventory.search(
        CoffeePickSearch.builder()
          .setArchitecture("x64")
          .setArchiveHash(RuntimeHash.of("SHA-256", HASH_VALUE))
          .setArchiveSize(100L)
          .setArchiveURI(URI.create("https://www.example.com"))
          .setPlatform("linux")
          .setVersionRange(
            RuntimeVersionRange.builder()
              .setLower(Runtime.Version.parse("11.0.2"))
              .setLowerExclusive(false)
              .setUpper(Runtime.Version.parse("11.0.3"))
              .setUpperExclusive(false)
              .build())
          .setVm("hotspot")
          .build());

    Assertions.assertEquals(0L, (long) results.size());

    Assertions.assertEquals(1L, (long) this.event_log.size());
    final var event = this.eventFor(CoffeePickInventoryEventRuntimeLoaded.class, 0);
    Assertions.assertEquals(HASH_VALUE, event.id());
  }

  @Test
  public final void testOpenCorrupted()
    throws Exception
  {
    final var item = this.directory.resolve(
      "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    Files.createDirectories(item);
    Files.writeString(item.resolve("meta.properties"), "INVALID!");

    final var inventory = this.inventory(this.events, this.directory);

    Assertions.assertEquals(1L, (long) this.event_log.size());
    final var event = this.eventFor(CoffeePickInventoryEventRuntimeLoadFailed.class, 0);
    Assertions.assertEquals(
      "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
      event.id());
  }

  @SuppressWarnings("unchecked")
  private <T> T eventFor(
    final Class<T> clazz,
    final int index)
  {
    Assertions.assertEquals(clazz, this.event_log.get(index).getClass());
    return (T) this.event_log.get(index);
  }
}

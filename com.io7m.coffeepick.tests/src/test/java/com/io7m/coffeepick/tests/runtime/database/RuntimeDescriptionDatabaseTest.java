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

package com.io7m.coffeepick.tests.runtime.database;

import com.io7m.coffeepick.runtime.RuntimeConfiguration;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimeHash;
import com.io7m.coffeepick.runtime.database.RuntimeDescriptionDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class RuntimeDescriptionDatabaseTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RuntimeDescriptionDatabaseTest.class);

  private Path directory;

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.directory = Files.createTempDirectory("coffeepick--runtime-");
  }

  @Test
  public void testEmpty()
    throws IOException
  {
    final var database = RuntimeDescriptionDatabase.open(this.directory);
    Assertions.assertEquals(0L, (long) database.descriptions().size());
  }

  @Test
  public void testAddReopen()
    throws IOException
  {
    final var description =
      RuntimeDescription.builder()
        .setConfiguration(RuntimeConfiguration.JDK)
        .setRepository(URI.create("urn:repository"))
        .setVersion(Runtime.Version.parse("11"))
        .setArchiveHash(RuntimeHash.of("SHA-256", "abcd"))
        .setArchitecture("x64")
        .setPlatform("linux")
        .setArchiveURI(URI.create("http://example.com"))
        .setArchiveSize(100L)
        .setVm("hotspot")
        .build();

    final var database0 = RuntimeDescriptionDatabase.open(this.directory);
    Assertions.assertEquals(0L, (long) database0.descriptions().size());

    database0.add(description);
    Assertions.assertEquals(1L, (long) database0.descriptions().size());
    Assertions.assertTrue(database0.descriptions().values().contains(description));

    final var database1 = RuntimeDescriptionDatabase.open(this.directory);
    Assertions.assertEquals(1L, (long) database1.descriptions().size());
    Assertions.assertTrue(database1.descriptions().values().contains(description));
  }

  @Test
  public void testReopenCorrupted()
    throws IOException
  {
    final var description =
      RuntimeDescription.builder()
        .setConfiguration(RuntimeConfiguration.JDK)
        .setRepository(URI.create("urn:repository"))
        .setVersion(Runtime.Version.parse("11"))
        .setArchiveHash(RuntimeHash.of("SHA-256", "abcd"))
        .setArchitecture("x64")
        .setPlatform("linux")
        .setArchiveURI(URI.create("http://example.com"))
        .setArchiveSize(100L)
        .setVm("hotspot")
        .build();

    final var database0 = RuntimeDescriptionDatabase.open(this.directory);
    Assertions.assertEquals(0L, (long) database0.descriptions().size());

    database0.add(description);
    Assertions.assertEquals(1L, (long) database0.descriptions().size());
    Assertions.assertTrue(database0.descriptions().values().contains(description));

    Files.walk(this.directory)
      .filter(p -> Files.isRegularFile(p))
      .flatMap(p -> {
        try {
          Files.writeString(p, "!");
          return Stream.of(p);
        } catch (IOException e) {
          LOG.error("error: ", e);
          return Stream.empty();
        }
      })
      .forEach(p -> LOG.debug("path: {}", p));

    final var database1 = RuntimeDescriptionDatabase.open(this.directory);
    Assertions.assertEquals(0L, (long) database1.descriptions().size());
  }
}

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

package com.io7m.coffeepick.tests.runtime;

import com.io7m.coffeepick.runtime.RuntimeDescriptions;
import com.io7m.coffeepick.tests.client.api.CoffeePickInventoryContract;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Properties;

public final class RuntimeDescriptionsTest
{
  private static InputStream resource(
    final String name)
    throws IOException
  {
    final var file = "/com/io7m/coffeepick/tests/" + name;
    final var url = RuntimeDescriptionsTest.class.getResource(file);
    if (url == null) {
      throw new FileNotFoundException(file);
    }
    return url.openStream();
  }

  @Test
  public void testRoundTrip()
    throws Exception
  {
    try (var stream = resource("trivial.properties")) {
      final var properties = new Properties();
      properties.load(stream);

      final var description_0 =
        RuntimeDescriptions.parseFromProperties(properties);

      Assertions.assertEquals(
        "x64", description_0.architecture());
      Assertions.assertEquals(
        "linux", description_0.platform());
      Assertions.assertEquals(
        "11.0.1", description_0.version().toExternalString());
      Assertions.assertEquals(
        CoffeePickInventoryContract.HASH_VALUE,
        description_0.archiveHash().value());
      Assertions.assertEquals(
        "SHA-256", description_0.archiveHash().algorithm());
      Assertions.assertEquals(
        "https://www.io7m.com/", description_0.archiveURI().toString());
      Assertions.assertEquals(
        "urn:example", description_0.repository().toString());
      Assertions.assertEquals(
        "b12", description_0.build().get().buildNumber());
      Assertions.assertEquals(
        OffsetDateTime.parse("2018-01-01T00:00:00+00:00"),
        description_0.build().get().time());

      final var output =
        RuntimeDescriptions.serializeToProperties(description_0);
      final var description_1 =
        RuntimeDescriptions.parseFromProperties(output);

      Assertions.assertEquals(description_0, description_1);
    }
  }

  @Test
  public void testMissingRuntimeVersion()
    throws Exception
  {
    try (var stream = resource("trivial.properties")) {
      final var properties = new Properties();
      properties.load(stream);

      properties.remove("coffeepick.runtimeVersion");

      final var ex =
        Assertions.assertThrows(
          IOException.class,
          () -> RuntimeDescriptions.parseFromProperties(properties));

      Assertions.assertTrue(ex.getMessage().contains("Version"));
    }
  }

  @Test
  public void testMissingArchiveURI()
    throws Exception
  {
    try (var stream = resource("trivial.properties")) {
      final var properties = new Properties();
      properties.load(stream);

      properties.remove("coffeepick.runtimeArchiveURI");

      final var ex =
        Assertions.assertThrows(
          IOException.class,
          () -> RuntimeDescriptions.parseFromProperties(properties));

      Assertions.assertTrue(ex.getMessage().contains("URI"));
    }
  }

  @Test
  public void testBrokenArchiveURI()
    throws Exception
  {
    try (var stream = resource("trivial.properties")) {
      final var properties = new Properties();
      properties.load(stream);

      properties.setProperty("coffeepick.runtimeArchiveURI", " ");

      final var ex =
        Assertions.assertThrows(
          IOException.class,
          () -> RuntimeDescriptions.parseFromProperties(properties));

      Assertions.assertTrue(ex.getMessage().contains("URI"));
    }
  }

  @Test
  public void testBrokenBuildTime()
    throws Exception
  {
    try (var stream = resource("trivial.properties")) {
      final var properties = new Properties();
      properties.load(stream);

      properties.setProperty("coffeepick.runtimeBuildTime", " ");

      final var ex =
        Assertions.assertThrows(
          IOException.class,
          () -> RuntimeDescriptions.parseFromProperties(properties));

      Assertions.assertTrue(ex.getMessage().contains("Time"));
    }
  }

  @Test
  public void testMissingRepositoryURI()
    throws Exception
  {
    try (var stream = resource("trivial.properties")) {
      final var properties = new Properties();
      properties.load(stream);

      properties.remove("coffeepick.runtimeRepository");

      final var ex =
        Assertions.assertThrows(
          IOException.class,
          () -> RuntimeDescriptions.parseFromProperties(properties));

      Assertions.assertTrue(ex.getMessage().contains("Repository"));
    }
  }

  @Test
  public void testBrokenRepositoryURI()
    throws Exception
  {
    try (var stream = resource("trivial.properties")) {
      final var properties = new Properties();
      properties.load(stream);

      properties.setProperty("coffeepick.runtimeRepository", " ");

      final var ex =
        Assertions.assertThrows(
          IOException.class,
          () -> RuntimeDescriptions.parseFromProperties(properties));

      Assertions.assertTrue(ex.getMessage().contains("Repository"));
    }
  }

  @Test
  public void testMissingArchiveSize()
    throws Exception
  {
    try (var stream = resource("trivial.properties")) {
      final var properties = new Properties();
      properties.load(stream);

      properties.remove("coffeepick.runtimeArchiveSize");

      final var ex =
        Assertions.assertThrows(
          IOException.class,
          () -> RuntimeDescriptions.parseFromProperties(properties));

      Assertions.assertTrue(ex.getMessage().contains("Size"));
    }
  }

  @Test
  public void testMissingArchivePlatform()
    throws Exception
  {
    try (var stream = resource("trivial.properties")) {
      final var properties = new Properties();
      properties.load(stream);

      properties.remove("coffeepick.runtimePlatform");

      final var ex =
        Assertions.assertThrows(
          IOException.class,
          () -> RuntimeDescriptions.parseFromProperties(properties));

      Assertions.assertTrue(ex.getMessage().contains("Platform"));
    }
  }

  @Test
  public void testMissingArchiveArchitecture()
    throws Exception
  {
    try (var stream = resource("trivial.properties")) {
      final var properties = new Properties();
      properties.load(stream);

      properties.remove("coffeepick.runtimeArchitecture");

      final var ex =
        Assertions.assertThrows(
          IOException.class,
          () -> RuntimeDescriptions.parseFromProperties(properties));

      Assertions.assertTrue(ex.getMessage().contains("Architecture"));
    }
  }

  @Test
  public void testMissingArchiveVM()
    throws Exception
  {
    try (var stream = resource("trivial.properties")) {
      final var properties = new Properties();
      properties.load(stream);

      properties.remove("coffeepick.runtimeVM");

      final var ex =
        Assertions.assertThrows(
          IOException.class,
          () -> RuntimeDescriptions.parseFromProperties(properties));

      Assertions.assertTrue(ex.getMessage().contains("VM"));
    }
  }

  @Test
  public void testMissingHashValue()
    throws Exception
  {
    try (var stream = resource("trivial.properties")) {
      final var properties = new Properties();
      properties.load(stream);

      properties.remove("coffeepick.runtimeArchiveHashValue");

      final var ex =
        Assertions.assertThrows(
          IOException.class,
          () -> RuntimeDescriptions.parseFromProperties(properties));

      Assertions.assertTrue(ex.getMessage().contains("Value"));
    }
  }

  @Test
  public void testMissingHashAlgorithm()
    throws Exception
  {
    try (var stream = resource("trivial.properties")) {
      final var properties = new Properties();
      properties.load(stream);

      properties.remove("coffeepick.runtimeArchiveHashAlgorithm");

      final var ex =
        Assertions.assertThrows(
          IOException.class,
          () -> RuntimeDescriptions.parseFromProperties(properties));

      Assertions.assertTrue(ex.getMessage().contains("Algorithm"));
    }
  }

  @Test
  public void testUnsupportedFormat()
    throws Exception
  {
    try (var stream = resource("trivial.properties")) {
      final var properties = new Properties();
      properties.load(stream);

      properties.setProperty("coffeepick.formatVersion", Integer.toString(Integer.MAX_VALUE));

      final var ex =
        Assertions.assertThrows(
          IOException.class,
          () -> RuntimeDescriptions.parseFromProperties(properties));

      Assertions.assertTrue(ex.getMessage().contains("Unsupported"));
    }
  }

  @Test
  public void testBrokenFormat()
    throws Exception
  {
    try (var stream = resource("trivial.properties")) {
      final var properties = new Properties();
      properties.load(stream);

      properties.setProperty("coffeepick.formatVersion", "z");

      final var ex =
        Assertions.assertThrows(
          IOException.class,
          () -> RuntimeDescriptions.parseFromProperties(properties));
    }
  }

  @Test
  public void testMissingFormat()
    throws Exception
  {
    try (var stream = resource("trivial.properties")) {
      final var properties = new Properties();
      properties.load(stream);

      properties.remove("coffeepick.formatVersion");

      final var ex =
        Assertions.assertThrows(
          IOException.class,
          () -> RuntimeDescriptions.parseFromProperties(properties));

      Assertions.assertTrue(ex.getMessage().contains("Missing"));
    }
  }

  @Test
  public void testBrokenConfiguration()
    throws Exception
  {
    try (var stream = resource("trivial.properties")) {
      final var properties = new Properties();
      properties.load(stream);

      properties.setProperty("coffeepick.runtimeConfiguration", "z");

      final var ex =
        Assertions.assertThrows(
          IOException.class,
          () -> RuntimeDescriptions.parseFromProperties(properties));
    }
  }

  @Test
  public void testMissingConfiguration()
    throws Exception
  {
    try (var stream = resource("trivial.properties")) {
      final var properties = new Properties();
      properties.load(stream);

      properties.remove("coffeepick.runtimeConfiguration");

      final var ex =
        Assertions.assertThrows(
          IOException.class,
          () -> RuntimeDescriptions.parseFromProperties(properties));

      Assertions.assertTrue(ex.getMessage().contains("Missing"));
    }
  }

  @Test
  public void testMissingBuildTime()
    throws Exception
  {
    try (var stream = resource("trivial.properties")) {
      final var properties = new Properties();
      properties.load(stream);

      properties.remove("coffeepick.runtimeBuildTime");

      final var ex =
        Assertions.assertThrows(
          IOException.class,
          () -> RuntimeDescriptions.parseFromProperties(properties));

      Assertions.assertTrue(ex.getMessage().contains("Time"));
    }
  }
}

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

package com.io7m.coffeepick.runtime;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

/**
 * Functions to parse inventory items.
 */

public final class RuntimeDescriptions
{
  private static final String COFFEEPICK_FORMAT_VERSION =
    "coffeepick.formatVersion";
  private static final String COFFEEPICK_RUNTIME_REPOSITORY =
    "coffeepick.runtimeRepository";
  private static final String COFFEEPICK_RUNTIME_VERSION =
    "coffeepick.runtimeVersion";
  private static final String COFFEEPICK_RUNTIME_CONFIGURATION =
    "coffeepick.runtimeConfiguration";
  private static final String COFFEEPICK_RUNTIME_PLATFORM =
    "coffeepick.runtimePlatform";
  private static final String COFFEEPICK_RUNTIME_VM =
    "coffeepick.runtimeVM";
  private static final String COFFEEPICK_RUNTIME_ARCHITECTURE =
    "coffeepick.runtimeArchitecture";
  private static final String COFFEEPICK_RUNTIME_ARCHIVE_SIZE =
    "coffeepick.runtimeArchiveSize";
  private static final String COFFEEPICK_RUNTIME_ARCHIVE_URI =
    "coffeepick.runtimeArchiveURI";
  private static final String COFFEEPICK_RUNTIME_ARCHIVE_HASH_ALGORITHM =
    "coffeepick.runtimeArchiveHashAlgorithm";
  private static final String COFFEEPICK_RUNTIME_ARCHIVE_HASH_VALUE =
    "coffeepick.runtimeArchiveHashValue";
  private static final String COFFEEPICK_RUNTIME_TAGS =
    "coffeepick.runtimeTags";
  private static final String COFFEEPICK_RUNTIME_BUILD_NUMBER =
    "coffeepick.runtimeBuildNumber";
  private static final String COFFEEPICK_RUNTIME_BUILD_TIME =
    "coffeepick.runtimeBuildTime";

  private RuntimeDescriptions()
  {

  }

  /**
   * Parse a runtime runtimes from the given properties.
   *
   * @param properties The properties
   *
   * @return A runtimes
   *
   * @throws IOException On errors
   */

  public static RuntimeDescription parseFromProperties(
    final Properties properties)
    throws IOException
  {
    return parseFromPropertiesVersioned(Objects.requireNonNull(properties, "properties"));
  }

  /**
   * Serialize a runtime runtimes.
   *
   * @param description The runtime runtimes
   *
   * @return Properties
   */

  public static Properties serializeToProperties(
    final RuntimeDescription description)
  {
    Objects.requireNonNull(description, "runtimes");

    final var properties = new Properties();

    properties.setProperty(
      COFFEEPICK_FORMAT_VERSION, "1");

    properties.setProperty(
      COFFEEPICK_RUNTIME_ARCHITECTURE, description.architecture());
    properties.setProperty(
      COFFEEPICK_RUNTIME_ARCHIVE_HASH_ALGORITHM, description.archiveHash().algorithm());
    properties.setProperty(
      COFFEEPICK_RUNTIME_ARCHIVE_HASH_VALUE, description.archiveHash().value());

    description.build().ifPresent(build -> {
      properties.setProperty(
        COFFEEPICK_RUNTIME_BUILD_NUMBER, build.buildNumber());
      properties.setProperty(
        COFFEEPICK_RUNTIME_BUILD_TIME, ISO_OFFSET_DATE_TIME.format(build.time()));
    });

    properties.setProperty(
      COFFEEPICK_RUNTIME_CONFIGURATION, description.configuration().configurationName());
    properties.setProperty(
      COFFEEPICK_RUNTIME_PLATFORM, description.platform());
    properties.setProperty(
      COFFEEPICK_RUNTIME_VM, description.vm());
    properties.setProperty(
      COFFEEPICK_RUNTIME_ARCHIVE_SIZE, Long.toUnsignedString(description.archiveSize()));
    properties.setProperty(
      COFFEEPICK_RUNTIME_ARCHIVE_URI, description.archiveURI().toString());
    properties.setProperty(
      COFFEEPICK_RUNTIME_REPOSITORY, description.repository().toString());
    properties.setProperty(
      COFFEEPICK_RUNTIME_TAGS, String.join(" ", description.tags()));
    properties.setProperty(
      COFFEEPICK_RUNTIME_VERSION, description.version().toExternalString());

    return properties;
  }

  private static RuntimeDescription parseFromPropertiesVersioned(
    final Properties properties)
    throws IOException
  {
    final var separator = System.lineSeparator();

    if (properties.containsKey(COFFEEPICK_FORMAT_VERSION)) {
      try {
        final var version =
          Integer.parseUnsignedInt(properties.getProperty(COFFEEPICK_FORMAT_VERSION));

        if (version == 1) {
          return parseFromPropertiesV1(properties);
        }

        throw new IOException(
          new StringBuilder(64)
            .append("Unsupported properties version")
            .append(separator)
            .append("  Version: ")
            .append(Integer.toUnsignedString(version))
            .append(separator)
            .toString());
      } catch (final NumberFormatException e) {
        throw new IOException(e);
      }
    }

    throw new IOException(
      new StringBuilder(64)
        .append("Missing property version")
        .append(separator)
        .append("  Expected: A field '")
        .append(COFFEEPICK_FORMAT_VERSION)
        .append('\'')
        .append(separator)
        .append("  Received: Nothing")
        .append(separator)
        .toString());
  }

  // CHECKSTYLE:OFF
  private static RuntimeDescription parseFromPropertiesV1(
    final Properties properties)
    throws IOException
  {
    IOException exception = null;

    String architecture = null;
    try {
      architecture = parseArchitecture(properties);
    } catch (final IOException e) {
      exception = accumulateException(exception, e);
    }

    RuntimeHash archive_hash = null;
    try {
      archive_hash = parseArchiveHash(properties);
    } catch (final IOException e) {
      exception = accumulateException(exception, e);
    }

    var archive_size = 0L;
    try {
      archive_size = parseArchiveSize(properties);
    } catch (final IOException e) {
      exception = accumulateException(exception, e);
    }

    URI archive_uri = null;
    try {
      archive_uri = parseArchiveURI(properties);
    } catch (final IOException e) {
      exception = accumulateException(exception, e);
    }

    Optional<RuntimeBuild> build = Optional.empty();
    try {
      build = parseBuild(properties);
    } catch (final IOException e) {
      exception = accumulateException(exception, e);
    }

    URI repository = null;
    try {
      repository = parseRepositoryURI(properties);
    } catch (final IOException e) {
      exception = accumulateException(exception, e);
    }

    String platform = null;
    try {
      platform = parsePlatform(properties);
    } catch (final IOException e) {
      exception = accumulateException(exception, e);
    }

    String vm = null;
    try {
      vm = parseVM(properties);
    } catch (final IOException e) {
      exception = accumulateException(exception, e);
    }

    RuntimeVersion version = null;
    try {
      version = parseVersion(properties);
    } catch (final IOException e) {
      exception = accumulateException(exception, e);
    }

    RuntimeConfiguration configuration = null;
    try {
      configuration = parseConfiguration(properties);
    } catch (final IOException e) {
      exception = accumulateException(exception, e);
    }

    final var tags = parseTags(properties);

    if (exception != null) {
      throw exception;
    }

    return RuntimeDescription.builder()
      .setArchitecture(architecture)
      .setArchiveHash(archive_hash)
      .setArchiveSize(archive_size)
      .setArchiveURI(archive_uri)
      .setBuild(build)
      .setConfiguration(configuration)
      .setPlatform(platform)
      .setRepository(repository)
      .setTags(tags)
      .setVm(vm)
      .setVersion(version)
      .build();
  }
  // CHECKSTYLE:ON

  private static Optional<RuntimeBuild> parseBuild(
    final Properties properties)
    throws IOException
  {
    if (properties.containsKey(COFFEEPICK_RUNTIME_BUILD_NUMBER)) {
      final var number =
        requireField(properties, COFFEEPICK_RUNTIME_BUILD_NUMBER).trim();
      final var time_text =
        requireField(properties, COFFEEPICK_RUNTIME_BUILD_TIME).trim();

      try {
        final var time = OffsetDateTime.from(ISO_OFFSET_DATE_TIME.parse(time_text));
        return Optional.of(
          RuntimeBuild.builder()
            .setBuildNumber(number)
            .setTime(time)
            .build());
      } catch (final DateTimeException e) {
        final var separator = System.lineSeparator();
        throw new IOException(
          new StringBuilder(64)
            .append("Unparseable configuration")
            .append(separator)
            .append("  Field: ")
            .append(COFFEEPICK_RUNTIME_BUILD_TIME)
            .append(separator)
            .append("  Expected: A date/time value")
            .append(separator)
            .append("  Received: ")
            .append(time_text)
            .append(separator)
            .toString(),
          e);
      }
    }

    return Optional.empty();
  }

  private static Set<String> parseTags(
    final Properties properties)
  {
    if (properties.containsKey(COFFEEPICK_RUNTIME_TAGS)) {
      final var raw = properties.getProperty(COFFEEPICK_RUNTIME_TAGS).trim();
      if (!raw.isEmpty()) {
        return Set.of(raw.split(" "));
      }
    }
    return Set.of();
  }

  private static IOException accumulateException(
    final IOException exception,
    final IOException next)
  {
    if (exception == null) {
      return next;
    }
    exception.addSuppressed(next);
    return exception;
  }

  private static RuntimeConfiguration parseConfiguration(
    final Properties properties)
    throws IOException
  {
    final var text = requireField(properties, COFFEEPICK_RUNTIME_CONFIGURATION);

    try {
      return RuntimeConfiguration.ofName(text);
    } catch (final Exception e) {
      final var separator = System.lineSeparator();
      throw new IOException(
        new StringBuilder(64)
          .append("Unparseable configuration")
          .append(separator)
          .append("  Field: ")
          .append(COFFEEPICK_RUNTIME_CONFIGURATION)
          .append(separator)
          .append("  Expected: A configuration value")
          .append(separator)
          .append("  Received: ")
          .append(text)
          .append(separator)
          .toString(),
        e);
    }
  }

  private static RuntimeVersion parseVersion(
    final Properties properties)
    throws IOException
  {
    final var text = requireField(properties, COFFEEPICK_RUNTIME_VERSION);

    try {
      return RuntimeVersions.parse(text);
    } catch (final Exception e) {
      final var separator = System.lineSeparator();
      throw new IOException(
        new StringBuilder(64)
          .append("Unparseable runtime version")
          .append(separator)
          .append("  Field: ")
          .append(COFFEEPICK_RUNTIME_VERSION)
          .append(separator)
          .append("  Expected: A runtime version")
          .append(separator)
          .append("  Received: ")
          .append(text)
          .append(separator)
          .toString(),
        e);
    }
  }

  private static String requireField(
    final Properties properties,
    final String name)
    throws IOException
  {
    final var text = properties.getProperty(name);
    if (text == null) {
      final var separator = System.lineSeparator();
      throw new IOException(
        new StringBuilder(64)
          .append("Missing required field.")
          .append(separator)
          .append("  Field: ")
          .append(name)
          .append(separator)
          .toString());
    }
    return text;
  }

  private static String parsePlatform(
    final Properties properties)
    throws IOException
  {
    return requireField(properties, COFFEEPICK_RUNTIME_PLATFORM);
  }

  private static String parseVM(
    final Properties properties)
    throws IOException
  {
    return requireField(properties, COFFEEPICK_RUNTIME_VM);
  }

  private static URI parseRepositoryURI(
    final Properties properties)
    throws IOException
  {
    final var text = requireField(properties, COFFEEPICK_RUNTIME_REPOSITORY);

    try {
      return new URI(text);
    } catch (final URISyntaxException e) {
      final var separator = System.lineSeparator();
      throw new IOException(
        new StringBuilder(64)
          .append("Unparseable URI")
          .append(separator)
          .append("  Field: ")
          .append(COFFEEPICK_RUNTIME_REPOSITORY)
          .append(separator)
          .append("  Expected: A URI")
          .append(separator)
          .append("  Received: ")
          .append(text)
          .append(separator)
          .toString(),
        e);
    }
  }

  private static URI parseArchiveURI(
    final Properties properties)
    throws IOException
  {
    final var text = requireField(properties, COFFEEPICK_RUNTIME_ARCHIVE_URI);

    try {
      return new URI(text);
    } catch (final URISyntaxException e) {
      final var separator = System.lineSeparator();
      throw new IOException(
        new StringBuilder(64)
          .append("Unparseable URI")
          .append(separator)
          .append("  Field: ")
          .append(COFFEEPICK_RUNTIME_ARCHIVE_URI)
          .append(separator)
          .append("  Expected: A URI")
          .append(separator)
          .append("  Received: ")
          .append(text)
          .append(separator)
          .toString(),
        e);
    }
  }

  private static long parseArchiveSize(
    final Properties properties)
    throws IOException
  {
    final var text = requireField(properties, COFFEEPICK_RUNTIME_ARCHIVE_SIZE);

    try {
      return Long.parseUnsignedLong(text);
    } catch (final NumberFormatException e) {
      final var separator = System.lineSeparator();
      throw new IOException(
        new StringBuilder(64)
          .append("Unparseable size")
          .append(separator)
          .append("  Field: ")
          .append(COFFEEPICK_RUNTIME_ARCHIVE_SIZE)
          .append(separator)
          .append("  Expected: An unsigned integer")
          .append(separator)
          .append("  Received: ")
          .append(text)
          .append(separator)
          .toString(),
        e);
    }
  }

  private static RuntimeHash parseArchiveHash(
    final Properties properties)
    throws IOException
  {
    return RuntimeHash.of(
      requireField(properties, COFFEEPICK_RUNTIME_ARCHIVE_HASH_ALGORITHM),
      requireField(properties, COFFEEPICK_RUNTIME_ARCHIVE_HASH_VALUE));
  }

  private static String parseArchitecture(
    final Properties properties)
    throws IOException
  {
    return requireField(properties, COFFEEPICK_RUNTIME_ARCHITECTURE);
  }
}

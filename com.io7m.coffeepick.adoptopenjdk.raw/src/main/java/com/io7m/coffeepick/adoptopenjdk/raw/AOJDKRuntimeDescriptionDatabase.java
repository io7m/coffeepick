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

package com.io7m.coffeepick.adoptopenjdk.raw;

import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimeDescriptionType;
import com.io7m.coffeepick.runtime.RuntimeDescriptions;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * A persistent database of runtime descriptions.
 */

@ThreadSafe
public final class AOJDKRuntimeDescriptionDatabase
{
  private static final Logger LOG = LoggerFactory.getLogger(AOJDKRuntimeDescriptionDatabase.class);
  private final Path path;
  private final ConcurrentHashMap<String, RuntimeDescription> descriptions;
  private final Map<String, RuntimeDescription> descriptions_read;

  private AOJDKRuntimeDescriptionDatabase(
    final Path in_path,
    final Map<String, RuntimeDescription> in_descriptions)
  {
    this.path =
      Objects.requireNonNull(in_path, "path");
    this.descriptions =
      new ConcurrentHashMap<>(Objects.requireNonNull(in_descriptions, "descriptions"));
    this.descriptions_read =
      Collections.unmodifiableMap(this.descriptions);
  }

  /**
   * Open an existing, or create a new, on-disk cache. The cache will be populated from the contents
   * of the given directory if it exists and contains runtime descriptions.
   *
   * @param path The directory
   *
   * @return A new cache
   *
   * @throws IOException On I/O errors
   */

  public static AOJDKRuntimeDescriptionDatabase open(
    final Path path)
    throws IOException
  {
    Files.createDirectories(path);

    final var descriptions =
      Files.list(path)
        .filter(file -> Files.isRegularFile(file))
        .flatMap(AOJDKRuntimeDescriptionDatabase::parseAsStream)
        .collect(Collectors.toMap(RuntimeDescriptionType::id, Function.identity()));

    return new AOJDKRuntimeDescriptionDatabase(path, descriptions);
  }

  private static Stream<RuntimeDescription> parseAsStream(
    final Path file)
  {
    try {
      return Stream.of(parse(file));
    } catch (final IOException e) {
      LOG.debug("could not parse {}: ", file, e);
      return Stream.empty();
    }
  }

  private static RuntimeDescription parse(
    final Path file)
    throws IOException
  {
    try (var stream = Files.newInputStream(file)) {
      final var props = new Properties();
      props.load(stream);
      return RuntimeDescriptions.parseFromProperties(props);
    } catch (final IOException e) {
      Files.deleteIfExists(file);
      throw e;
    }
  }

  /**
   * Add a description to the cache. The description will be persisted to disk, but no exception
   * will be raised if persisting the file fails.
   *
   * @param description The description to be added
   */

  public void add(
    final RuntimeDescription description)
  {
    this.descriptions.put(description.id(), description);

    try {
      this.write(description);
    } catch (final IOException e) {
      LOG.debug("could not cache {}: ", description.id(), e);
    }
  }

  private void write(
    final RuntimeDescription description)
    throws IOException
  {
    final var file =
      this.path.resolve(description.id() + ".properties");
    final var file_tmp =
      this.path.resolve(description.id() + ".properties.tmp");

    try (var output = Files.newOutputStream(file_tmp, TRUNCATE_EXISTING, CREATE, WRITE)) {
      RuntimeDescriptions.serializeToProperties(description)
        .store(output, "");
      Files.move(file_tmp, file, ATOMIC_MOVE, REPLACE_EXISTING);
    }
  }

  /**
   * @return A read-only map of the current descriptions
   */

  public Map<String, RuntimeDescription> descriptions()
  {
    return this.descriptions_read;
  }
}

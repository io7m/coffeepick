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

package com.io7m.coffeepick.client.vanilla;

import com.io7m.coffeepick.api.CoffeePickInventoryEventRuntimeDeleted;
import com.io7m.coffeepick.api.CoffeePickInventoryEventRuntimeLoadFailed;
import com.io7m.coffeepick.api.CoffeePickInventoryEventRuntimeLoaded;
import com.io7m.coffeepick.api.CoffeePickInventoryEventType;
import com.io7m.coffeepick.api.CoffeePickInventoryType;
import com.io7m.coffeepick.api.CoffeePickSearch;
import com.io7m.coffeepick.api.CoffeePickSearches;
import com.io7m.coffeepick.api.CoffeePickVerification;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimeDescriptionType;
import com.io7m.coffeepick.runtime.RuntimeDescriptions;
import com.io7m.coffeepick.runtime.RuntimeHash;
import io.reactivex.Observable;
import io.reactivex.subjects.Subject;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * An inventory of downloaded runtimes.
 */

public final class CoffeePickInventory implements CoffeePickInventoryType
{
  private static final Logger LOG = LoggerFactory.getLogger(CoffeePickInventory.class);

  private static final String LOCK =
    "lock";
  private static final String META_PROPERTIES =
    "meta.properties";
  private static final String META_PROPERTIES_TMP =
    "meta.properties.tmp";
  private static final String ARCHIVE =
    "archive";
  private static final String ARCHIVE_TMP =
    "archive.tmp";

  private final Map<String, RuntimeDescription> runtimes;
  private final Subject<CoffeePickInventoryEventType> events;
  private final Path path;

  private CoffeePickInventory(
    final Subject<CoffeePickInventoryEventType> in_events,
    final Map<String, RuntimeDescription> in_runtimes,
    final Path in_path)
  {
    this.runtimes =
      new TreeMap<>(Objects.requireNonNull(in_runtimes, "runtimes"));
    this.events =
      Objects.requireNonNull(in_events, "events");
    this.path =
      Objects.requireNonNull(in_path, "path");
  }

  /**
   * Open an inventory.
   *
   * @param events The receiver of events
   * @param path   The path of the inventory
   *
   * @return An inventory
   *
   * @throws IOException On I/O errors
   */

  public static CoffeePickInventoryType open(
    final Subject<CoffeePickInventoryEventType> events,
    final Path path)
    throws IOException
  {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(events, "events");

    Files.createDirectories(path);

    final var runtimes =
      Files.list(path)
        .map(Path::toAbsolutePath)
        .filter(Files::isDirectory)
        .map(directory -> load(events, directory))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toMap(o -> o.archiveHash().value(), o -> o));

    return new CoffeePickInventory(events, runtimes, path);
  }

  private static Optional<RuntimeDescription> load(
    final Subject<CoffeePickInventoryEventType> events,
    final Path path)
  {
    final var meta = path.resolve(META_PROPERTIES);

    try (var stream = Files.newInputStream(meta)) {
      final var properties = new Properties();
      properties.load(stream);
      final var item = RuntimeDescriptions.parseFromProperties(properties);
      events.onNext(CoffeePickInventoryEventRuntimeLoaded.of(item.id()));
      return Optional.of(item);
    } catch (final IOException e) {
      events.onNext(CoffeePickInventoryEventRuntimeLoadFailed.of(path.getFileName().toString(), e));
      return Optional.empty();
    }
  }

  private static void writeLockedMeta(
    final RuntimeDescription description,
    final Path meta_tmp,
    final Path meta)
    throws IOException
  {
    LOG.debug("write meta {}", meta_tmp);
    try (var stream = Files.newOutputStream(meta_tmp, TRUNCATE_EXISTING, CREATE, WRITE)) {
      final var properties = RuntimeDescriptions.serializeToProperties(description);
      properties.store(stream, "");
      stream.flush();
      LOG.debug("rename {} -> {}", meta_tmp, meta);
      Files.move(meta_tmp, meta, REPLACE_EXISTING, ATOMIC_MOVE);
    } finally {
      LOG.debug("unlink {}", meta_tmp);
      Files.deleteIfExists(meta_tmp);
    }
  }

  private static void writeLockedArchive(
    final RuntimeDescription description,
    final RuntimeArchiveWriterType writer,
    final Path archive_tmp,
    final Path archive)
    throws IOException
  {
    LOG.debug("write archive {}", archive_tmp);
    try (var stream = Files.newOutputStream(archive_tmp, TRUNCATE_EXISTING, CREATE, WRITE)) {
      final var expected_hash = description.archiveHash();
      final var digest = MessageDigest.getInstance(expected_hash.algorithm());
      try (var digest_stream = new DigestOutputStream(stream, digest)) {
        writer.write(digest_stream);
        digest_stream.flush();

        final var received_hash =
          RuntimeHash.of(expected_hash.algorithm(), Hex.encodeHexString(digest.digest(), true));

        if (!Objects.equals(expected_hash, received_hash)) {
          final var separator = System.lineSeparator();
          throw new IOException(
            new StringBuilder(64)
              .append("Final archive hash does not match expected hash.")
              .append(separator)
              .append("  Expected: ")
              .append(expected_hash.algorithm())
              .append(" ")
              .append(expected_hash.value())
              .append(separator)
              .append("  Received: ")
              .append(received_hash.algorithm())
              .append(" ")
              .append(received_hash.value())
              .append(separator)
              .toString());
        }

        LOG.debug("rename {} -> {}", archive_tmp, archive);
        Files.move(archive_tmp, archive, REPLACE_EXISTING, ATOMIC_MOVE);
      }
    } catch (final NoSuchAlgorithmException e) {
      throw new IOException(e);
    } finally {
      LOG.debug("unlink {}", archive_tmp);
      Files.deleteIfExists(archive_tmp);
    }
  }

  private static CoffeePickVerification verifyLocked(
    final RuntimeDescription description,
    final Path archive)
    throws IOException
  {
    try {
      final var expected_hash = description.archiveHash();
      final var algorithm = expected_hash.algorithm();
      final var digest = MessageDigest.getInstance(algorithm);

      try (var input = Files.newInputStream(archive)) {
        try (var input_digest = new DigestInputStream(input, digest)) {
          input_digest.transferTo(new NullOutputStream());
          return CoffeePickVerification.builder()
            .setExpectedHash(description.archiveHash())
            .setReceivedHash(RuntimeHash.of(algorithm, Hex.encodeHexString(digest.digest(), true)))
            .build();
        }
      }
    } catch (final NoSuchAlgorithmException e) {
      throw new IOException(e);
    }
  }

  private static void deleteLocked(
    final Path directory,
    final Path lock)
    throws IOException
  {
    Files.list(directory)
      .map(Path::toAbsolutePath)
      .filter(p -> !Objects.equals(p, lock))
      .forEach(p -> {
        try {
          LOG.debug("delete {}", p);
          Files.deleteIfExists(p);
        } catch (final IOException e) {
          LOG.error("i/o error: ", e);
        }
      });
  }

  @Override
  public Optional<RuntimeDescription> searchExact(
    final String id)
  {
    Objects.requireNonNull(id, "id");
    return Optional.ofNullable(this.runtimes.get(id));
  }

  @Override
  public Observable<CoffeePickInventoryEventType> events()
  {
    return this.events;
  }

  @Override
  public Map<String, RuntimeDescription> search(
    final CoffeePickSearch parameters)
  {
    Objects.requireNonNull(parameters, "parameters");

    return this.runtimes.values()
      .stream()
      .filter(runtime -> CoffeePickSearches.matches(runtime, parameters))
      .collect(Collectors.toMap(RuntimeDescriptionType::id, d -> d));
  }

  @Override
  public Path write(
    final RuntimeDescription description,
    final RuntimeArchiveWriterType writer)
    throws IOException
  {
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(writer, "writer");

    final var runtime_id = description.id();

    final var directory =
      this.path.resolve(runtime_id)
        .toAbsolutePath();

    Files.createDirectories(directory);

    final var lock =
      directory.resolve(LOCK).toAbsolutePath();
    final var archive_tmp =
      directory.resolve(ARCHIVE_TMP).toAbsolutePath();
    final var archive =
      directory.resolve(ARCHIVE).toAbsolutePath();
    final var meta_tmp =
      directory.resolve(META_PROPERTIES_TMP).toAbsolutePath();
    final var meta =
      directory.resolve(META_PROPERTIES).toAbsolutePath();

    LOG.debug("lock {}", lock);
    try (var channel = FileChannel.open(lock, CREATE, WRITE)) {
      try (var ignored = channel.lock()) {
        writeLockedArchive(description, writer, archive_tmp, archive);
        writeLockedMeta(description, meta_tmp, meta);
      }
    }

    this.runtimes.put(runtime_id, description);
    this.events.onNext(CoffeePickInventoryEventRuntimeLoaded.of(runtime_id));
    return archive;
  }

  @Override
  public Optional<Path> pathOf(
    final String id)
    throws IOException
  {
    Objects.requireNonNull(id, "ID");

    final var directory =
      this.path.resolve(id)
        .toAbsolutePath();

    if (Files.isDirectory(directory)) {
      final var lock =
        directory.resolve(LOCK)
          .toAbsolutePath();

      LOG.debug("lock {}", lock);
      try (var channel = FileChannel.open(lock, CREATE, WRITE)) {
        try (var ignored = channel.lock()) {
          return Optional.of(directory.resolve(ARCHIVE).toAbsolutePath());
        }
      }
    }

    return Optional.empty();
  }

  @Override
  public void delete(final String id)
    throws IOException
  {
    Objects.requireNonNull(id, "ID");

    final var directory =
      this.path.resolve(id)
        .toAbsolutePath();

    if (Files.isDirectory(directory)) {
      final var lock =
        directory.resolve(LOCK)
          .toAbsolutePath();

      LOG.debug("lock {}", lock);
      try (var channel = FileChannel.open(lock, CREATE, WRITE)) {
        try (var ignored = channel.lock()) {
          deleteLocked(directory, lock);
        }
      }

      LOG.debug("delete {}", lock);
      Files.deleteIfExists(lock);
      LOG.debug("delete {}", directory);
      Files.delete(directory);

      this.runtimes.remove(id);
      this.events.onNext(CoffeePickInventoryEventRuntimeDeleted.of(id));
    }
  }

  @Override
  public CoffeePickVerification verify(final String id)
    throws IOException
  {
    Objects.requireNonNull(id, "id");

    final var directory =
      this.path.resolve(id)
        .toAbsolutePath();

    final var lock =
      directory.resolve(LOCK).toAbsolutePath();
    final var archive =
      directory.resolve(ARCHIVE).toAbsolutePath();
    final var meta =
      directory.resolve(META_PROPERTIES).toAbsolutePath();

    LOG.debug("lock {}", lock);
    try (var channel = FileChannel.open(lock, CREATE, WRITE)) {
      try (var ignored = channel.lock()) {
        try (var stream = Files.newInputStream(meta)) {
          final var properties = new Properties();
          properties.load(stream);
          final var item = RuntimeDescriptions.parseFromProperties(properties);
          return verifyLocked(item, archive);
        }
      }
    }
  }

  private static final class NullOutputStream extends OutputStream
  {
    NullOutputStream()
    {

    }

    @Override
    public void write(final int b)
    {

    }
  }
}

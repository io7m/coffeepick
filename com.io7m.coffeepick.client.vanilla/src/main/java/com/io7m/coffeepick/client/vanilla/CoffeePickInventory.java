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
import com.io7m.coffeepick.api.CoffeePickIsCancelledType;
import com.io7m.coffeepick.api.CoffeePickSearch;
import com.io7m.coffeepick.api.CoffeePickSearches;
import com.io7m.coffeepick.api.CoffeePickVerification;
import com.io7m.coffeepick.client.vanilla.internal.CoffeePickArchiveEntries;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimeDescriptionType;
import com.io7m.coffeepick.runtime.RuntimeDescriptions;
import com.io7m.coffeepick.runtime.RuntimeHash;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.Subject;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
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
  private final CompressorStreamFactory compressors;
  private final Path path;
  private final ArchiveStreamFactory archives;

  private CoffeePickInventory(
    final Subject<CoffeePickInventoryEventType> in_events,
    final Map<String, RuntimeDescription> in_runtimes,
    final ArchiveStreamFactory in_archives,
    final CompressorStreamFactory in_compressors,
    final Path in_path)
  {
    this.runtimes =
      new TreeMap<>(Objects.requireNonNull(in_runtimes, "runtimes"));
    this.events =
      Objects.requireNonNull(in_events, "events");
    this.compressors =
      Objects.requireNonNull(in_compressors, "compressors");
    this.path =
      Objects.requireNonNull(in_path, "path");
    this.archives =
      Objects.requireNonNull(in_archives, "archives");
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
    return open(events, new ArchiveStreamFactory(), new CompressorStreamFactory(), path);
  }

  /**
   * Open an inventory.
   *
   * @param events      The receiver of events
   * @param archives    A factory for archive streams
   * @param compressors A factory for compressors
   * @param path        The path of the inventory
   *
   * @return An inventory
   *
   * @throws IOException On I/O errors
   */

  public static CoffeePickInventoryType open(
    final Subject<CoffeePickInventoryEventType> events,
    final ArchiveStreamFactory archives,
    final CompressorStreamFactory compressors,
    final Path path)
    throws IOException
  {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(archives, "archives");
    Objects.requireNonNull(compressors, "compressors");
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

    return new CoffeePickInventory(events, runtimes, archives, compressors, path);
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
    final RuntimeCancellableArchiveWriterType writer,
    final Path archiveTmp,
    final Path archive)
    throws IOException
  {
    LOG.debug("write archive {}", archiveTmp);
    try (var stream = Files.newOutputStream(archiveTmp, TRUNCATE_EXISTING, CREATE, WRITE)) {
      final var expectedHash = description.archiveHash();
      final var digest = MessageDigest.getInstance(expectedHash.algorithm());
      try (var digestStream = new DigestOutputStream(stream, digest)) {
        writer.write(digestStream);
        digestStream.flush();

        final var encodedDigest =
          Hex.encodeHexString(digest.digest(), true);
        final var receivedHash =
          RuntimeHash.of(expectedHash.algorithm(), encodedDigest);

        if (!Objects.equals(expectedHash, receivedHash)) {
          final var separator = System.lineSeparator();
          throw new IOException(
            new StringBuilder(64)
              .append("Final archive hash does not match expected hash.")
              .append(separator)
              .append("  Expected: ")
              .append(expectedHash.algorithm())
              .append(" ")
              .append(expectedHash.value())
              .append(separator)
              .append("  Received: ")
              .append(receivedHash.algorithm())
              .append(" ")
              .append(receivedHash.value())
              .append(separator)
              .toString());
        }

        LOG.debug("rename {} -> {}", archiveTmp, archive);
        Files.move(archiveTmp, archive, REPLACE_EXISTING, ATOMIC_MOVE);
      }
    } catch (final NoSuchAlgorithmException e) {
      throw new IOException(e);
    } finally {
      LOG.debug("unlink {}", archiveTmp);
      Files.deleteIfExists(archiveTmp);
    }
  }

  private static CoffeePickVerification verifyLocked(
    final RuntimeDescription description,
    final CoffeePickIsCancelledType cancelled,
    final Path archive)
    throws IOException
  {
    try {
      final var expected_hash = description.archiveHash();
      final var algorithm = expected_hash.algorithm();
      final var digest = MessageDigest.getInstance(algorithm);

      try (var input = Files.newInputStream(archive)) {
        try (var input_digest = new DigestInputStream(input, digest)) {
          try (var output = new NullOutputStream()) {
            writeCancellable(cancelled, input_digest, output);
          }

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

  private static void writeCancellable(
    final CoffeePickIsCancelledType cancelled,
    final InputStream input_digest,
    final OutputStream output)
    throws IOException
  {
    final var buffer = new byte[4096];
    while (true) {
      if (cancelled.isCancelled()) {
        throw new CancellationException();
      }
      final var r = input_digest.read(buffer);
      if (r == -1) {
        break;
      }
      output.write(buffer, 0, r);
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

  private static BufferedInputStream open(final Path file)
    throws IOException
  {
    return new BufferedInputStream(Files.newInputStream(file, READ));
  }

  private static void unpackArchiveLocked(
    final Path archive,
    final Path target_path,
    final ArchiveInputStream archive_stream,
    final CoffeePickIsCancelledType cancelled,
    final Set<UnpackOption> options)
    throws IOException
  {
    LOG.debug("unpacking {}", archive_stream.getClass().getCanonicalName());

    while (true) {
      if (cancelled.isCancelled()) {
        throw new CancellationException();
      }

      final var entry = archive_stream.getNextEntry();
      if (entry == null) {
        break;
      }

      final var entry_name =
        stripLeadingDirectoryIfRequested(options, entry, Paths.get(entry.getName()).normalize());
      if (entry_name.isEmpty()) {
        continue;
      }

      final var name = entry_name.get();
      final var output = target_path.resolve(name).toAbsolutePath();

      LOG.debug("unpack {} -> {}", name, output);
      if (!output.startsWith(target_path)) {
        throw pathTraversalException(archive, name, output);
      }

      if (entry.isDirectory()) {
        Files.createDirectories(output);
      } else {
        final var parent = output.getParent();
        if (parent != null) {
          Files.createDirectories(parent);
        }
        try (var output_stream = Files.newOutputStream(output, CREATE, WRITE, TRUNCATE_EXISTING)) {
          archive_stream.transferTo(output_stream);
        }
      }

      final var mode_opt =
        CoffeePickArchiveEntries.posixFilePermissionsFor(entry)
          .map(perms -> stripPermsIfNecessary(perms, options));

      if (mode_opt.isPresent()) {
        final var mode = mode_opt.get();

        try {
          Files.setPosixFilePermissions(output, mode);
        } catch (final UnsupportedOperationException e) {
          // Not a POSIX filesystem
        }
      }
    }
  }

  private static Set<PosixFilePermission> stripPermsIfNecessary(
    final Set<PosixFilePermission> perms,
    final Collection<UnpackOption> options)
  {
    if (options.contains(UnpackOption.STRIP_NON_OWNER_WRITABLE)) {
      final var results = new HashSet<>(perms);
      results.remove(PosixFilePermission.GROUP_WRITE);
      results.remove(PosixFilePermission.OTHERS_WRITE);
      return results;
    }
    return perms;
  }

  private static IOException pathTraversalException(
    final Path archive,
    final Path name,
    final Path output)
  {
    final var separator = System.lineSeparator();
    return new IOException(
      new StringBuilder(128)
        .append("Refusing to unpack files above target directory.")
        .append(separator)
        .append("  Archive: ")
        .append(archive)
        .append(separator)
        .append("  Entry:   ")
        .append(name)
        .append(separator)
        .append("  Output:  ")
        .append(output)
        .append(separator)
        .toString());
  }

  private static Optional<Path> stripLeadingDirectoryIfRequested(
    final Collection<UnpackOption> options,
    final ArchiveEntry entry,
    final Path path)
  {
    if (options.contains(UnpackOption.STRIP_LEADING_DIRECTORY)) {
      if (path.getNameCount() > 1) {
        return Optional.of(path.subpath(1, path.getNameCount()));
      }
      if (entry.isDirectory()) {
        return Optional.empty();
      }
    }
    return Optional.of(path);
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
      .filter(runtime -> CoffeePickSearches.matchesExact(runtime, parameters))
      .collect(Collectors.toMap(RuntimeDescriptionType::id, d -> d));
  }

  @Override
  public Path write(
    final RuntimeDescription description,
    final RuntimeCancellableArchiveWriterType writer)
    throws IOException
  {
    Objects.requireNonNull(description, "runtimes");
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
        try {
          writeLockedArchive(description, writer, archive_tmp, archive);
          writeLockedMeta(description, meta_tmp, meta);
        } catch (final CancellationException e) {
          try {
            Files.deleteIfExists(meta);
          } catch (final IOException ex) {
            LOG.error("could not delete {}: ", meta, ex);
          }
          try {
            Files.deleteIfExists(archive);
          } catch (final IOException ex) {
            LOG.error("could not delete {}: ", meta, ex);
          }
          throw e;
        }
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
  public Path unpack(
    final String id,
    final Path target_path,
    final CoffeePickIsCancelledType cancelled,
    final Set<UnpackOption> options)
    throws IOException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(target_path, "path");
    Objects.requireNonNull(cancelled, "cancelled");
    Objects.requireNonNull(options, "options");

    final var target_abs =
      target_path.toAbsolutePath();

    final var directory =
      this.path.resolve(id)
        .toAbsolutePath();

    Files.createDirectories(directory);

    final var lock =
      directory.resolve(LOCK).toAbsolutePath();
    final var archive =
      directory.resolve(ARCHIVE).toAbsolutePath();

    LOG.debug("lock {}", lock);
    try (var channel = FileChannel.open(lock, CREATE, WRITE)) {
      try (var ignored = channel.lock()) {
        try {
          this.unpackLocked(target_abs, archive, cancelled, options);
        } catch (final CancellationException e) {
          Files.walk(target_abs)
            .sorted(Comparator.reverseOrder())
            .forEach(p -> {
              try {
                Files.delete(p);
              } catch (final IOException ex) {
                LOG.error("could not delete {}: ", p, ex);
              }
            });
        }
      }
    }

    return target_path;
  }

  private BufferedInputStream openCompressed(final Path file)
    throws IOException, CompressorException
  {
    return new BufferedInputStream(this.compressors.createCompressorInputStream(open(file)));
  }

  private void unpackLocked(
    final Path target_path,
    final Path archive,
    final CoffeePickIsCancelledType cancelled,
    final Set<UnpackOption> options)
    throws IOException
  {
    final CompressorException compressor_exception;

    /*
     * Try to treat the file as if it was a compressed archive. If a compressor exception is
     * raised, then the archive probably isn't compressed. Retry using a plain archive stream.
     */

    try (var uncompressed = this.openCompressed(archive)) {
      try (var archive_stream = this.archives.createArchiveInputStream(uncompressed)) {
        unpackArchiveLocked(archive, target_path, archive_stream, cancelled, options);
        return;
      }
    } catch (final ArchiveException e) {
      throw new IOException(e);
    } catch (final CompressorException e) {
      compressor_exception = e;
    }

    try (var archive_stream = this.archives.createArchiveInputStream(open(archive))) {
      unpackArchiveLocked(archive, target_path, archive_stream, cancelled, options);
    } catch (final ArchiveException e) {
      final var ex = new IOException(e);
      ex.addSuppressed(compressor_exception);
      throw ex;
    }
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
  public CoffeePickVerification verify(
    final String id,
    final CoffeePickIsCancelledType cancelled)
    throws IOException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(cancelled, "cancelled");

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
          return verifyLocked(item, cancelled, archive);
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

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

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.arj.ArjArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Functions for dealing with archive entries.
 */

public final class CoffeePickArchiveEntries
{
  private CoffeePickArchiveEntries()
  {

  }

  /**
   * Try to extract POSIX mode information from the given entry.
   *
   * @param entry The entry
   *
   * @return POSIX mode information if any is present
   */

  public static Optional<Set<PosixFilePermission>> posixFilePermissionsFor(
    final ArchiveEntry entry)
  {
    Objects.requireNonNull(entry, "entry");

    return posixModeFor(entry)
      .stream()
      .mapToObj(CoffeePickArchiveEntries::posixFilePermissionsForValue)
      .findAny();
  }

  // CHECKSTYLE:OFF
  private static Set<PosixFilePermission> posixFilePermissionsForValue(final long mode)
  {
    final Set<PosixFilePermission> perms = EnumSet.noneOf(PosixFilePermission.class);
    if ((mode & 0b000_000_001L) == 0b000_000_001L) {
      perms.add(PosixFilePermission.OTHERS_EXECUTE);
    }
    if ((mode & 0b000_000_010L) == 0b000_000_010L) {
      perms.add(PosixFilePermission.OTHERS_WRITE);
    }
    if ((mode & 0b000_000_100L) == 0b000_000_100L) {
      perms.add(PosixFilePermission.OTHERS_READ);
    }

    if ((mode & 0b000_001_000L) == 0b000_001_000L) {
      perms.add(PosixFilePermission.GROUP_EXECUTE);
    }
    if ((mode & 0b000_010_000L) == 0b000_010_000L) {
      perms.add(PosixFilePermission.GROUP_WRITE);
    }
    if ((mode & 0b000_100_000L) == 0b000_100_000L) {
      perms.add(PosixFilePermission.GROUP_READ);
    }

    if ((mode & 0b001_000_000L) == 0b001_000_000L) {
      perms.add(PosixFilePermission.OWNER_EXECUTE);
    }
    if ((mode & 0b010_000_000L) == 0b010_000_000L) {
      perms.add(PosixFilePermission.OWNER_WRITE);
    }
    if ((mode & 0b100_000_000L) == 0b100_000_000L) {
      perms.add(PosixFilePermission.OWNER_READ);
    }
    return perms;
  }
  // CHECKSTYLE:ON

  /**
   * Try to extract POSIX mode information from the given entry.
   *
   * @param entry The entry
   *
   * @return POSIX mode information if any is present
   */

  // CHECKSTYLE:OFF
  public static OptionalLong posixModeFor(
    final ArchiveEntry entry)
  {
    Objects.requireNonNull(entry, "entry");

    if (entry instanceof ArArchiveEntry) {
      return modeForAr((ArArchiveEntry) entry);
    }
    if (entry instanceof ArjArchiveEntry) {
      return modeForArj((ArjArchiveEntry) entry);
    }
    if (entry instanceof CpioArchiveEntry) {
      return modeForCpio((CpioArchiveEntry) entry);
    }
    if (entry instanceof JarArchiveEntry) {
      return modeForJar((JarArchiveEntry) entry);
    }
    if (entry instanceof SevenZArchiveEntry) {
      return modeFor7z((SevenZArchiveEntry) entry);
    }
    if (entry instanceof TarArchiveEntry) {
      return modeForTar((TarArchiveEntry) entry);
    }
    if (entry instanceof ZipArchiveEntry) {
      return modeForZip((ZipArchiveEntry) entry);
    }
    return OptionalLong.empty();
  }
  // CHECKSTYLE:ON

  private static OptionalLong modeForZip(
    final ZipArchiveEntry entry)
  {
    return OptionalLong.empty();
  }

  private static OptionalLong modeForTar(
    final TarArchiveEntry entry)
  {
    return OptionalLong.of(Integer.toUnsignedLong(entry.getMode()));
  }

  private static OptionalLong modeFor7z(
    final SevenZArchiveEntry entry)
  {
    return OptionalLong.empty();
  }

  private static OptionalLong modeForJar(
    final JarArchiveEntry entry)
  {
    return OptionalLong.of(Integer.toUnsignedLong(entry.getUnixMode()));
  }

  private static OptionalLong modeForCpio(
    final CpioArchiveEntry entry)
  {
    return OptionalLong.of(entry.getMode());
  }

  private static OptionalLong modeForArj(
    final ArjArchiveEntry entry)
  {
    return OptionalLong.of(Integer.toUnsignedLong(entry.getUnixMode()));
  }

  private static OptionalLong modeForAr(
    final ArArchiveEntry entry)
  {
    return OptionalLong.of(Integer.toUnsignedLong(entry.getMode()));
  }
}

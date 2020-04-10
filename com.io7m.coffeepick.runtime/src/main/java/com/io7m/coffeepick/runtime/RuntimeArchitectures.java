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

import java.util.Objects;

/**
 * Standard names for architectures.
 */

public enum RuntimeArchitectures
{
  /**
   * 32-bit x86
   */

  X32("x32"),

  /**
   * 64-bit x86
   */

  X64("x64"),

  /**
   * S390X
   */

  S390X("s390x"),

  /**
   * PowerPC 64-bit Little-Endian
   */

  PPC64_LE("ppc64le"),

  /**
   * PowerPC 64-bit Big-Endian
   */

  PPC64_BE("ppc64"),

  /**
   * 64-bit ARM
   */

  AARCH_64("aarch64"),

  /**
   * 32-bit ARM with hardware floating point.
   */

  ARM32_HFLT("arm32-hflt");

  private final String name;

  RuntimeArchitectures(
    final String in_name)
  {
    this.name = Objects.requireNonNull(in_name, "name");
  }

  /**
   * @return The name of the architecture
   */

  public String architectureName()
  {
    return this.name;
  }
}

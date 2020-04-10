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
 * Standard names for VMs.
 */

public enum RuntimeVMs
{
  /**
   * Hotspot
   */

  HOTSPOT("hotspot"),

  /**
   * GraalVM
   */

  GRAAL("graal"),

  /**
   * Eclipse OpenJ9.
   */

  OPENJ9("openj9");

  private final String name;

  RuntimeVMs(
    final String in_name)
  {
    this.name = Objects.requireNonNull(in_name, "name");
  }

  /**
   * @return The name of the VM
   */

  public String vmName()
  {
    return this.name;
  }
}

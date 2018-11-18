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

package com.io7m.coffeepick.shell;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import org.slf4j.event.Level;

import java.util.Objects;

/**
 * A converter for log level values.
 */

public final class CoffeePickShellLogLevelConverter implements IStringConverter<Level>
{
  /**
   * Construct a converter.
   */

  public CoffeePickShellLogLevelConverter()
  {

  }

  @Override
  public Level convert(final String value)
  {
    Objects.requireNonNull(value, "value");

    for (final var level : Level.values()) {
      if (level.name().equalsIgnoreCase(value)) {
        return level;
      }
    }

    throw new ParameterException(
      "Unrecognized log level: " + value);
  }
}

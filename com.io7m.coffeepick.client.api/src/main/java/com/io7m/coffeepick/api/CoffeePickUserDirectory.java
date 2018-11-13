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

package com.io7m.coffeepick.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Functions to pick a sensible user directory across platforms.
 */

public final class CoffeePickUserDirectory
{
  private static final Logger LOG = LoggerFactory.getLogger(CoffeePickUserDirectory.class);

  private CoffeePickUserDirectory()
  {

  }

  /**
   * @return A sensible user directory
   */

  public static Path detectUserDirectory()
  {
    final var coffee = System.getProperty("coffeepick.directory");
    if (coffee != null) {
      LOG.debug("used property coffeepick.directory");
      return Paths.get(coffee)
        .toAbsolutePath();
    }

    final var xdg = System.getenv("XDG_DATA_HOME");
    if (xdg != null) {
      LOG.debug("used environment XDG_DATA_HOME");
      return Paths.get(xdg)
        .resolve("coffeepick")
        .toAbsolutePath();
    }

    final var home = System.getProperty("user.home");
    if (home != null) {
      LOG.debug("used property user.home");
      return Paths.get(home)
        .resolve(".coffeepick")
        .toAbsolutePath();
    }

    final var userdir = System.getProperty("user.dir");
    if (userdir != null) {
      LOG.debug("used property user.dir");
      return Paths.get(userdir)
        .resolve("coffeepick")
        .toAbsolutePath();
    }

    LOG.debug("current directory");
    return Paths.get("")
      .resolve("coffeepick")
      .toAbsolutePath();
  }
}

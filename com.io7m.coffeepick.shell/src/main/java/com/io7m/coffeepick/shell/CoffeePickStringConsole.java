/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> http://io7m.com
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

import com.beust.jcommander.internal.Console;

final class CoffeePickStringConsole implements Console
{
  private final StringBuilder stringBuilder;

  CoffeePickStringConsole()
  {
    this.stringBuilder = new StringBuilder();
  }

  public StringBuilder stringBuilder()
  {
    return this.stringBuilder;
  }

  @Override
  public void print(final String s)
  {
    this.stringBuilder.append(s);
  }

  @Override
  public void println(final String s)
  {
    this.stringBuilder.append(s);
    this.stringBuilder.append(System.lineSeparator());
  }

  @Override
  public char[] readPassword(final boolean b)
  {
    return new char[0];
  }
}

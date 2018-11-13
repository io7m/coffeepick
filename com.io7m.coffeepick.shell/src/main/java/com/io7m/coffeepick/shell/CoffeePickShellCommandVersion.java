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

import com.io7m.coffeepick.api.CoffeePickClientType;
import org.jline.builtins.Completers;

import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static org.jline.builtins.Completers.TreeCompleter.node;

/**
 * Version displays.
 */

public final class CoffeePickShellCommandVersion implements CoffeePickShellCommandType
{
  private final CoffeePickClientType client;
  private final PrintWriter writer;

  /**
   * Construct a command.
   *
   * @param in_client The client
   * @param in_writer The output terminal writer
   */

  public CoffeePickShellCommandVersion(
    final CoffeePickClientType in_client,
    final PrintWriter in_writer)
  {
    this.client = Objects.requireNonNull(in_client, "client");
    this.writer = Objects.requireNonNull(in_writer, "writer");
  }

  @Override
  public String name()
  {
    return "version";
  }

  @Override
  public CompletableFuture<?> execute(
    final List<String> arguments)
  {
    final var pack = this.client.getClass().getPackage();
    this.writer.append(pack.getImplementationTitle());
    this.writer.append(" ");
    this.writer.append(pack.getImplementationVersion());
    this.writer.println();
    return CompletableFuture.completedFuture(new Object());
  }

  @Override
  public Completers.TreeCompleter.Node completer()
  {
    return node(this.name());
  }
}

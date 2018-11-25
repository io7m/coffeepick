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
import com.io7m.coffeepick.runtime.RuntimeDescription;
import org.jline.builtins.Completers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static org.jline.builtins.Completers.TreeCompleter.node;

/**
 * Show a runtime.
 */

public final class CoffeePickShellCommandRuntimeShow implements CoffeePickShellCommandType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoffeePickShellCommandRuntimeShow.class);

  private final CoffeePickClientType client;
  private final PrintWriter writer;

  /**
   * Construct a command.
   *
   * @param in_client The client
   * @param in_writer The output terminal writer
   */

  public CoffeePickShellCommandRuntimeShow(
    final CoffeePickClientType in_client,
    final PrintWriter in_writer)
  {
    this.client = Objects.requireNonNull(in_client, "client");
    this.writer = Objects.requireNonNull(in_writer, "writer");
  }

  @Override
  public String name()
  {
    return "runtime-show";
  }

  @Override
  public CompletableFuture<?> execute(
    final List<String> arguments)
  {
    if (arguments.size() != 2) {
      this.writer.println("usage: runtime-show <id>");
      this.writer.flush();
      return CompletableFuture.completedFuture(null);
    }

    final var op0 = this.client.inventorySearchExact(arguments.get(1));
    final var op1 = this.client.catalogSearchExact(arguments.get(1));

    return op0.thenAcceptBoth(op1, (inventory_runtime, catalog_runtime) -> {
      if (inventory_runtime.isPresent()) {
        this.showRuntime(inventory_runtime.get());
      } else if (catalog_runtime.isPresent()) {
        this.showRuntime(catalog_runtime.get());
      } else {
        LOG.error("No runtime available with the given ID");
      }
    });
  }

  private void showRuntime(
    final RuntimeDescription description)
  {
    this.writer.printf("%-16s: %-32s\n", "ID", description.id());

    this.writer.printf("%-16s: %-32s\n", "Architecture", description.architecture());
    this.writer.printf("%-16s: %s:%-32s\n", "Archive Hash", description.archiveHash().algorithm(), description.archiveHash().value());
    this.writer.printf("%-16s: %-32s\n", "Archive Size", Long.toUnsignedString(description.archiveSize()));
    this.writer.printf("%-16s: %-32s\n", "Archive URI", description.archiveURI());
    this.writer.printf("%-16s: %-32s\n", "Configuration", description.configuration());
    this.writer.printf("%-16s: %-32s\n", "Platform", description.platform());
    this.writer.printf("%-16s: %-32s\n", "Repository", description.repository());
    this.writer.printf("%-16s: %-32s\n", "Tags", String.join(" ", description.tags()));
    this.writer.printf("%-16s: %-32s\n", "Version", description.version());
    this.writer.printf("%-16s: %-32s\n", "VM", description.vm());
  }

  @Override
  public Completers.TreeCompleter.Node completer()
  {
    return node(this.name());
  }
}

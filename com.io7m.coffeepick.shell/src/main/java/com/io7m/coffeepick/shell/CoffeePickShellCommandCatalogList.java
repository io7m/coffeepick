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
import com.io7m.coffeepick.api.CoffeePickSearch;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import org.jline.builtins.Completers;

import java.io.PrintWriter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.jline.builtins.Completers.TreeCompleter.node;

/**
 * Catalog listing.
 */

public final class CoffeePickShellCommandCatalogList implements CoffeePickShellCommandType
{
  private final CoffeePickClientType client;
  private final PrintWriter writer;

  /**
   * Construct a command.
   *
   * @param in_client The client
   * @param in_writer The output terminal writer
   */

  public CoffeePickShellCommandCatalogList(
    final CoffeePickClientType in_client,
    final PrintWriter in_writer)
  {
    this.client = Objects.requireNonNull(in_client, "client");
    this.writer = Objects.requireNonNull(in_writer, "writer");
  }

  static void showRuntimes(
    final PrintWriter writer,
    final Map<String, RuntimeDescription> runtimes)
  {
    writer.printf(
      "%-70s | %-8s | %-8s | %-8s | %-8s | %-10s | %s\n",
      "ID",
      "Arch",
      "Platform",
      "Version",
      "VM",
      "Size",
      "Tags");

    runtimes.values()
      .stream()
      .sorted(Comparator.comparing(RuntimeDescription::version)
                .thenComparing(RuntimeDescription::architecture)
                .thenComparing(RuntimeDescription::platform))
      .forEach(description -> {
        writer.printf(
          "%-70s | %-8s | %-8s | %-8s | %-8s | %-8.2fMB | %s\n",
          description.id(),
          description.architecture(),
          description.platform(),
          description.version(),
          description.vm(),
          Double.valueOf((double) description.archiveSize() / 1_000_000.0),
          description.tags().stream().sorted().collect(Collectors.joining(" ")));
      });
  }

  @Override
  public String name()
  {
    return "catalog";
  }

  @Override
  public CompletableFuture<?> execute(
    final List<String> arguments)
  {
    try {
      final CoffeePickSearch params;
      if (arguments.size() > 1) {
        final var rest = arguments.subList(1, arguments.size());
        params = CoffeePickShellSearchParameters.parseSearchParameters(rest);
      } else {
        params = CoffeePickSearch.builder().build();
      }

      return this.client.catalogSearch(params)
        .future()
        .thenAccept(runtimes -> showRuntimes(this.writer, runtimes));
    } catch (final Exception e) {
      return CompletableFuture.failedFuture(e);
    }
  }

  @Override
  public Completers.TreeCompleter.Node completer()
  {
    return node(this.name());
  }
}

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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.io7m.coffeepick.api.CoffeePickClientType;
import com.io7m.coffeepick.api.CoffeePickInventoryType.UnpackOption;
import org.jline.builtins.Completers;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.io7m.coffeepick.api.CoffeePickInventoryType.UnpackOption.STRIP_LEADING_DIRECTORY;
import static com.io7m.coffeepick.api.CoffeePickInventoryType.UnpackOption.STRIP_NON_OWNER_WRITABLE;
import static org.jline.builtins.Completers.TreeCompleter.node;

/**
 * Inventory archive unpacking.
 */

public final class CoffeePickShellCommandInventoryUnpack implements CoffeePickShellCommandType
{
  private final CoffeePickClientType client;
  private final PrintWriter writer;

  /**
   * Construct a command.
   *
   * @param in_client The client
   * @param in_writer The output terminal writer
   */

  public CoffeePickShellCommandInventoryUnpack(
    final CoffeePickClientType in_client,
    final PrintWriter in_writer)
  {
    this.client = Objects.requireNonNull(in_client, "client");
    this.writer = Objects.requireNonNull(in_writer, "writer");
  }

  @Override
  public String name()
  {
    return "unpack";
  }

  @Override
  public CompletableFuture<?> execute(
    final List<String> arguments)
  {
    final var args = new String[arguments.size() - 1];
    arguments.subList(1, arguments.size()).toArray(args);

    final var parameters = new Parameters();
    final var console = new CoffeePickStringConsole();
    final var commander =
      JCommander.newBuilder()
        .addObject(parameters)
        .console(console)
        .programName("unpack")
        .build();

    try {
      commander.parse(args);

      if (parameters.rest.size() != 1) {
        throw new ParameterException("Must specify exactly one ID");
      }

      final Set<UnpackOption> options = EnumSet.noneOf(UnpackOption.class);
      if (parameters.strip_leading_directory) {
        options.add(STRIP_LEADING_DIRECTORY);
      }
      if (parameters.strip_non_owner_write) {
        options.add(STRIP_NON_OWNER_WRITABLE);
      }

      return this.client.inventoryUnpack(
        parameters.rest.get(0),
        parameters.output_path,
        options);

    } catch (final ParameterException e) {
      commander.usage();
      this.writer.println(console.stringBuilder().toString());
      return CompletableFuture.completedFuture(null);
    }
  }

  @Override
  public Completers.TreeCompleter.Node completer()
  {
    return node(this.name());
  }

  // CHECKSTYLE:OFF
  private static final class Parameters
  {
    @Parameter(
      description = "The output directory",
      required = true,
      names = "--output")
    Path output_path;

    @Parameter(
      description = "Strip the leading directory from archives",
      required = false,
      names = "--strip-leading-directory")
    boolean strip_leading_directory = false;

    @Parameter(
      description = "Remove write permissions for group/others on POSIX filesystems",
      required = false,
      names = "--strip-group-other-writes")
    boolean strip_non_owner_write = true;

    @Parameter(description = "<id>")
    private List<String> rest = new ArrayList<>(1);

    // CHECKSTYLE:ON
    Parameters()
    {

    }
  }
}

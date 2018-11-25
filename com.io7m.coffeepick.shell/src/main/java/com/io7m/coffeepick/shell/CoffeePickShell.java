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
import com.io7m.coffeepick.api.CoffeePickUserDirectory;
import com.io7m.coffeepick.client.vanilla.CoffeePickClients;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoriesServiceLoaderProvider;
import org.jline.builtins.Completers.TreeCompleter;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The CoffeePick shell.
 */

public final class CoffeePickShell
{
  private static final Logger LOG = LoggerFactory.getLogger(CoffeePickShell.class);

  private CoffeePickShell()
  {

  }

  /**
   * Command line entry point.
   *
   * @param args The command-line arguments
   *
   * @throws IOException On I/O errors
   */

  public static void main(
    final String[] args)
    throws IOException
  {
    final var parameters = new Parameters();
    final var commander =
      JCommander.newBuilder()
        .addObject(parameters)
        .programName("coffeepick")
        .build();

    try {
      commander.parse(args);
    } catch (final ParameterException e) {
      LOG.error("parameter error: {}", e.getMessage());
      final var sb = new StringBuilder(128);
      commander.usage(sb);
      LOG.info("usage: {}", sb.toString());
      System.exit(1);
    }

    final var root =
      (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    root.setLevel(mapLevel(parameters.log_level));

    try (var terminal = createTerminal()) {
      try (var writer = terminal.writer()) {

        final var directory = CoffeePickUserDirectory.detectUserDirectory();
        writer.printf("User directory: %s\n", directory);
        writer.flush();
        Files.createDirectories(directory);

        writer.printf("Loading repositories…\n");
        writer.flush();

        final var repositories = RuntimeRepositoriesServiceLoaderProvider.create();
        final var clients = CoffeePickClients.createWith(repositories);

        try (var client = clients.newClient(directory)) {

          final var commands =
            List.of(
              new CoffeePickShellCommandRuntimeShow(client, writer),
              new CoffeePickShellCommandRepositoryUpdate(client, writer),
              new CoffeePickShellCommandInventoryList(client, writer),
              new CoffeePickShellCommandInventoryPathOf(client, writer),
              new CoffeePickShellCommandInventoryUnpack(client, writer),
              new CoffeePickShellCommandDelete(client, writer),
              new CoffeePickShellCommandVerify(client, writer),
              new CoffeePickShellCommandCatalogList(client, writer),
              new CoffeePickShellCommandVersion(client, writer),
              new CoffeePickShellCommandDownload(client, writer));

          final var commands_named =
            commands.stream()
              .collect(Collectors.toMap(CoffeePickShellCommandType::name, Function.identity()));

          final var completer =
            new TreeCompleter(
              commands.stream()
                .map(CoffeePickShellCommandType::completer)
                .collect(Collectors.toList()));

          final var history = new DefaultHistory();
          final var parser = new DefaultParser();

          final var reader =
            LineReaderBuilder.builder()
              .appName("coffeepick")
              .terminal(terminal)
              .completer(completer)
              .parser(parser)
              .history(history)
              .build();

          while (true) {
            String line = null;

            try {
              line = reader.readLine("[coffeepick]$ ", null, (MaskingCallback) null, null);
            } catch (final UserInterruptException e) {
              // Ignore
            } catch (final EndOfFileException e) {
              return;
            }

            if (line == null) {
              continue;
            }

            line = line.trim();
            if (line.isEmpty()) {
              continue;
            }

            final var parsed = reader.getParser().parse(line, 0);
            final var command_name = parsed.word();
            if (!commands_named.containsKey(command_name)) {
              writer.append("Unrecognized command: ");
              writer.append(command_name);
              writer.println();
              writer.flush();
              continue;
            }

            final var command = commands_named.get(command_name);
            final var future = command.execute(parsed.words());

            try {
              future.get();
            } catch (final InterruptedException e) {
              Thread.currentThread().interrupt();
            } catch (final ExecutionException e) {
              writer.append("Error during command execution: ");
              e.getCause().printStackTrace(writer);
              writer.println();
              writer.flush();
            }
          }
        }
      }
    }
  }

  private static ch.qos.logback.classic.Level mapLevel(
    final Level level)
  {
    switch (level) {
      case ERROR:
        return ch.qos.logback.classic.Level.ERROR;
      case WARN:
        return ch.qos.logback.classic.Level.WARN;
      case INFO:
        return ch.qos.logback.classic.Level.INFO;
      case DEBUG:
        return ch.qos.logback.classic.Level.DEBUG;
      case TRACE:
        return ch.qos.logback.classic.Level.TRACE;
    }

    throw new AssertionError("Unreachable code");
  }

  private static Terminal createTerminal()
    throws IOException
  {
    return TerminalBuilder.builder()
      .build();
  }

  private static final class Parameters
  {
    Parameters()
    {

    }

    @Parameter(
      description = "The logging level",
      required = false,
      converter = CoffeePickShellLogLevelConverter.class,
      names = "--verbose")
    Level log_level = Level.INFO;
  }
}

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

import com.io7m.coffeepick.api.CoffeePickCatalogEventRepositoryUpdateType;
import com.io7m.coffeepick.api.CoffeePickClientType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventUpdateFailed;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventUpdateFinished;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventUpdateRunning;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventUpdateStarted;
import io.reactivex.disposables.Disposable;
import org.jline.builtins.Completers;

import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.io7m.coffeepick.shell.CoffeePickShellCommandDownload.progressBar;
import static org.jline.builtins.Completers.TreeCompleter.node;

/**
 * Update a repository.
 */

public final class CoffeePickShellCommandRepositoryUpdate implements CoffeePickShellCommandType
{
  private final CoffeePickClientType client;
  private final PrintWriter writer;
  private final Disposable subscription;

  /**
   * Construct a command.
   *
   * @param in_client The client
   * @param in_writer The output terminal writer
   */

  public CoffeePickShellCommandRepositoryUpdate(
    final CoffeePickClientType in_client,
    final PrintWriter in_writer)
  {
    this.client = Objects.requireNonNull(in_client, "client");
    this.writer = Objects.requireNonNull(in_writer, "writer");

    this.subscription =
      this.client.events()
        .filter(e -> e instanceof CoffeePickCatalogEventRepositoryUpdateType)
        .cast(CoffeePickCatalogEventRepositoryUpdateType.class)
        .subscribe(this::onCatalogEventUpdate);
  }

  private void onCatalogEventUpdate(
    final CoffeePickCatalogEventRepositoryUpdateType event)
  {
    switch (event.event().kind()) {
      case STARTED: {
        final var ee = (RuntimeRepositoryEventUpdateStarted) event.event();
        this.writer.printf("Update of %s started…\n", ee.repository());
        break;
      }
      case RUNNING: {
        final var ee = (RuntimeRepositoryEventUpdateRunning) event.event();
        this.writer.printf(
          "[ %-6.2f %% ] |%-60s|\n",
          Double.valueOf(ee.progress() * 100.0),
          progressBar(60, ee.progress()));
        break;
      }
      case FAILED: {
        final var ee = (RuntimeRepositoryEventUpdateFailed) event.event();
        this.writer.printf("Update of %s failed:\n", ee.repository());
        ee.exception().printStackTrace(this.writer);
        break;
      }
      case FINISHED: {
        final var ee = (RuntimeRepositoryEventUpdateFinished) event.event();
        this.writer.printf("Update of %s finished.\n", ee.repository());
        break;
      }
    }

    this.writer.flush();
  }

  @Override
  public String name()
  {
    return "repository-update";
  }

  @Override
  public CompletableFuture<?> execute(
    final List<String> arguments)
  {
    if (arguments.size() != 2) {
      this.writer.println("usage: repository-update <uri>");
      this.writer.flush();
      return CompletableFuture.completedFuture(null);
    }

    try {
      return this.client.repositoryUpdate(new URI(arguments.get(1)));
    } catch (final URISyntaxException e) {
      return CompletableFuture.failedFuture(e);
    }
  }

  @Override
  public Completers.TreeCompleter.Node completer()
  {
    return node(this.name());
  }
}

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

import com.io7m.coffeepick.api.CoffeePickCatalogEventRuntimeDownloadFailed;
import com.io7m.coffeepick.api.CoffeePickCatalogEventRuntimeDownloadFinished;
import com.io7m.coffeepick.api.CoffeePickCatalogEventRuntimeDownloading;
import com.io7m.coffeepick.api.CoffeePickCatalogEventType;
import com.io7m.coffeepick.api.CoffeePickClientType;
import io.reactivex.disposables.Disposable;
import org.jline.builtins.Completers;

import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static org.jline.builtins.Completers.TreeCompleter.node;

/**
 * Download a runtime.
 */

public final class CoffeePickShellCommandDownload implements CoffeePickShellCommandType
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

  public CoffeePickShellCommandDownload(
    final CoffeePickClientType in_client,
    final PrintWriter in_writer)
  {
    this.client = Objects.requireNonNull(in_client, "client");
    this.writer = Objects.requireNonNull(in_writer, "writer");

    this.subscription =
      this.client.events()
        .filter(e -> e instanceof CoffeePickCatalogEventType)
        .cast(CoffeePickCatalogEventType.class)
        .subscribe(this::onCatalogEvent);
  }

  private static String megabytesPerSecond(final double octetsPerSecond)
  {
    return String.format("%.2fMB/s", Double.valueOf(octetsPerSecond / 1_000_000.0));
  }

  static String progressBar(
    final int length,
    final double progress)
  {
    final var builder = new StringBuilder(length);
    for (var index = 0; index < length * progress; ++index) {
      builder.append('+');
    }
    return builder.toString();
  }

  private void onCatalogEvent(
    final CoffeePickCatalogEventType event)
  {
    if (event instanceof CoffeePickCatalogEventRuntimeDownloadFinished) {
      this.writer.println("Download finished");
      this.writer.flush();
    } else if (event instanceof CoffeePickCatalogEventRuntimeDownloadFailed) {
      final var failed = (CoffeePickCatalogEventRuntimeDownloadFailed) event;
      this.writer.println("Download failed: ");
      failed.exception().printStackTrace(this.writer);
      this.writer.flush();
    } else if (event instanceof CoffeePickCatalogEventRuntimeDownloading) {
      final var downloading = (CoffeePickCatalogEventRuntimeDownloading) event;
      final var received_mb = (double) downloading.received() / 1_000_000.0;
      final var expected_mb = (double) downloading.expected() / 1_000_000.0;

      this.writer.printf(
        "[%8.2f MB / %8.2f MB] |%-60s| %-8s\n",
        Double.valueOf(received_mb),
        Double.valueOf(expected_mb),
        progressBar(60, downloading.progress()),
        megabytesPerSecond(downloading.octetsPerSecond()));
      this.writer.flush();
    }
  }

  @Override
  public String name()
  {
    return "download";
  }

  @Override
  public CompletableFuture<?> execute(
    final List<String> arguments)
  {
    if (arguments.size() != 2) {
      this.writer.println("usage: download <id>");
      this.writer.flush();
      return CompletableFuture.completedFuture(null);
    }

    return this.client.catalogDownload(arguments.get(1)).future();
  }

  @Override
  public Completers.TreeCompleter.Node completer()
  {
    return node(this.name());
  }
}

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

package com.io7m.coffeepick.client.vanilla.internal;

import java.net.URI;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The default string provider.
 */

public final class CoffeePickStrings
{
  private final ResourceBundle resourceBundle;

  private CoffeePickStrings(
    final ResourceBundle inResourceBundle)
  {
    this.resourceBundle = inResourceBundle;
  }

  /**
   * Retrieve the resource bundle for the given locale.
   *
   * @param locale The locale
   *
   * @return The resource bundle
   */

  public static ResourceBundle getResourceBundle(
    final Locale locale)
  {
    return ResourceBundle.getBundle(
      "com.io7m.coffeepick.client.vanilla.Strings",
      locale);
  }

  /**
   * Retrieve the resource bundle for the current locale.
   *
   * @return The resource bundle
   */

  public static ResourceBundle getResourceBundle()
  {
    return getResourceBundle(Locale.getDefault());
  }

  /**
   * Create a new string provider from the given bundle.
   *
   * @param bundle The resource bundle
   *
   * @return A string provider
   */

  public static CoffeePickStrings of(
    final ResourceBundle bundle)
  {
    return new CoffeePickStrings(bundle);
  }

  public ResourceBundle resourceBundle()
  {
    return this.resourceBundle;
  }

  public String format(
    final String id,
    final Object... args)
  {
    return MessageFormat.format(this.resourceBundle.getString(id), args);
  }

  @Override
  public String toString()
  {
    return String.format(
      "[CoffeePickStrings 0x%s]",
      Integer.toUnsignedString(System.identityHashCode(this), 16)
    );
  }

  public String inventoryDelete(
    final String id)
  {
    return this.format("task.inventoryDelete", id);
  }

  public String inventoryVerify(
    final String id)
  {
    return this.format("task.inventoryVerify", id);
  }

  public String inventorySearch()
  {
    return this.format("task.inventorySearch");
  }

  public String catalogSearch()
  {
    return this.format("task.catalogSearch");
  }

  public String catalogDownload(
    final String id)
  {
    return this.format("task.catalogDownload", id);
  }

  public String inventoryPathOf(
    final String id)
  {
    return this.format("task.inventoryPath", id);
  }

  public String inventoryUnpack(
    final String id,
    final Path path)
  {
    return this.format("task.inventoryUnpack", id, path);
  }

  public String repositoryUpdate(final URI uri)
  {
    return this.format("task.repositoryUpdate", uri);
  }

  public String repositoryList()
  {
    return this.format("task.repositoryList");
  }

  public String repositoryExport(
    final URI repository,
    final URI format,
    final Path output_path)
  {
    return this.format("task.repositoryExport", repository, format, output_path);
  }
}

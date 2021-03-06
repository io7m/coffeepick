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

import com.io7m.coffeepick.runtime.parser.api.CoffeePickParsers;
import com.io7m.coffeepick.runtime.parser.api.CoffeePickParsersType;
import com.io7m.coffeepick.runtime.parser.api.CoffeePickSerializers;
import com.io7m.coffeepick.runtime.parser.api.CoffeePickSerializersType;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;

/**
 * A provider of CoffeePick clients.
 */

public interface CoffeePickClientProviderType
{
  /**
   * Create a new client. The client will use the given directory for configuration data and
   * inventory.
   *
   * @param base_directory The base directory
   *
   * @return A new client
   *
   * @throws IOException On errors
   */

  default CoffeePickClientType newClient(
    final Path base_directory)
    throws IOException
  {
    return this.newClient(
      CoffeePickParsers.createFromServiceLoader(),
      CoffeePickSerializers.createFromServiceLoader(),
      base_directory,
      HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build());
  }

  /**
   * Create a new client. The client will use the given directory for configuration data and
   * inventory.
   *
   * @param parsers        The parser provider
   * @param serializers    The serializer provider
   * @param base_directory The base directory
   * @param http           The HTTP client that will be used
   *
   * @return A new client
   *
   * @throws IOException On errors
   */

  CoffeePickClientType newClient(
    CoffeePickParsersType parsers,
    CoffeePickSerializersType serializers,
    Path base_directory,
    HttpClient http)
    throws IOException;
}

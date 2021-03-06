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

package com.io7m.coffeepick.repository.spi;

import org.osgi.annotation.versioning.ConsumerType;

import java.net.http.HttpClient;
import java.nio.file.Path;

/**
 * A context interface passed to repositories.
 *
 * This is used to provide configuration and other information to repositories at runtime, and is
 * typically filled in by the client implementation. This interface essentially serves as a means to
 * avoid a circular dependency between the client API and repository SPI.
 */

@ConsumerType
public interface RuntimeRepositoryContextType
{
  /**
   * @return The base cache directory
   */

  Path cacheDirectory();

  /**
   * @return An HTTP client to be used for requests
   */

  HttpClient httpClient();
}

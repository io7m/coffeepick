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

import com.io7m.coffeepick.adoptopenjdk_v3.AOJRepositoryProvider;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderType;

/**
 * Java runtime retrieval (AdoptOpenJDK v3 provider)
 */

module com.io7m.coffeepick.adoptopenjdk_v3
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;
  requires static org.osgi.service.component.annotations;

  requires com.io7m.coffeepick.repository.spi;
  requires com.io7m.coffeepick.runtime.database;
  requires com.io7m.coffeepick.runtime;
  requires com.io7m.junreachable.core;
  requires io.reactivex.rxjava3;
  requires net.adoptopenjdk.v3.api;
  requires net.adoptopenjdk.v3.vanilla;
  requires org.slf4j;

  provides RuntimeRepositoryProviderType with AOJRepositoryProvider;

  exports com.io7m.coffeepick.adoptopenjdk_v3;
}
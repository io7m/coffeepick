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

/**
 * Java runtime retrieval (API)
 */

module com.io7m.coffeepick.client.api
{
  requires static com.io7m.immutables.style;
  requires static org.immutables.value;
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;
  requires static org.osgi.service.component.annotations;

  requires com.io7m.coffeepick.repository.spi;
  requires com.io7m.coffeepick.runtime.parser.api;
  requires com.io7m.coffeepick.runtime.parser.spi;
  requires com.io7m.coffeepick.runtime;
  requires io.reactivex.rxjava2;
  requires java.net.http;
  requires org.slf4j;

  exports com.io7m.coffeepick.api;
}

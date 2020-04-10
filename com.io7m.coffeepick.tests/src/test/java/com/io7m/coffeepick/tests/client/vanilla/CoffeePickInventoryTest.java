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

package com.io7m.coffeepick.tests.client.vanilla;

import com.io7m.coffeepick.api.CoffeePickInventoryEventType;
import com.io7m.coffeepick.api.CoffeePickInventoryType;
import com.io7m.coffeepick.client.vanilla.CoffeePickInventory;
import com.io7m.coffeepick.tests.client.api.CoffeePickInventoryContract;
import io.reactivex.rxjava3.subjects.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public final class CoffeePickInventoryTest extends CoffeePickInventoryContract
{
  @Override
  protected Logger logger()
  {
    return LoggerFactory.getLogger(CoffeePickInventoryTest.class);
  }

  @Override
  protected CoffeePickInventoryType inventory(
    final Subject<CoffeePickInventoryEventType> events,
    final Path path)
    throws IOException
  {
    return CoffeePickInventory.open(events, path);
  }
}

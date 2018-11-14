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

package com.io7m.coffeepick.tests;

import com.io7m.coffeepick.adoptopenjdk.raw.AOJDKDataParser;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

public final class AOJDKDataParserTest
{
  private static final Logger LOG = LoggerFactory.getLogger(AOJDKDataParserTest.class);

  @Test
  public void testParsingOpenJDK8()
    throws IOException
  {
    final var parser =
      AOJDKDataParser.create(new URL("https://raw.githubusercontent.com/AdoptOpenJDK/openjdk8-binaries/master/releases.json").openStream());

    final var descriptions = parser.parse();
    for (final var description : descriptions) {
      LOG.debug("description: {}", description);
    }
  }

  @Test
  public void testParsingOpenJDK9()
    throws IOException
  {
    final var parser =
      AOJDKDataParser.create(new URL("https://raw.githubusercontent.com/AdoptOpenJDK/openjdk9-binaries/master/releases.json").openStream());

    final var descriptions = parser.parse();
    for (final var description : descriptions) {
      LOG.debug("description: {}", description);
    }
  }

  @Test
  public void testParsingOpenJDK11()
    throws IOException
  {
    final var parser =
      AOJDKDataParser.create(new URL("https://raw.githubusercontent.com/AdoptOpenJDK/openjdk11-binaries/master/releases.json").openStream());

    final var descriptions = parser.parse();
    for (final var description : descriptions) {
      LOG.debug("description: {}", description);
    }
  }
}

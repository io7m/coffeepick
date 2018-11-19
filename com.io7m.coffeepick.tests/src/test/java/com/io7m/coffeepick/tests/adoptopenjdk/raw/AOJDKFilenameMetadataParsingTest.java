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

package com.io7m.coffeepick.tests.adoptopenjdk.raw;

import com.io7m.coffeepick.adoptopenjdk.raw.AOJDKFilenameMetadataParsing;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class AOJDKFilenameMetadataParsingTest
{
  private static final Logger LOG = LoggerFactory.getLogger(AOJDKFilenameMetadataParsingTest.class);

  private static void tryLine(
    final String line)
  {
    final var result = AOJDKFilenameMetadataParsing.parseFilename(line);
    LOG.debug("result: {} → {}", line, result);
    Assertions.assertTrue(
      result.isPresent(),
      new StringBuilder(128)
        .append("Must have parsed ")
        .append(line)
        .append(" correctly")
        .toString());
  }

  @Test
  public void testNames()
    throws Exception
  {
    final var file = "/com/io7m/coffeepick/tests/adoptopenjdk_names.txt";
    try (var stream = AOJDKFilenameMetadataParsingTest.class.getResourceAsStream(file)) {
      try (var reader = new BufferedReader(new InputStreamReader(stream, UTF_8))) {
        Assertions.assertAll(
          reader.lines()
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .filter(line -> !line.startsWith("#"))
            .map(line -> (Executable) () -> tryLine(line)));
      }
    }
  }
}

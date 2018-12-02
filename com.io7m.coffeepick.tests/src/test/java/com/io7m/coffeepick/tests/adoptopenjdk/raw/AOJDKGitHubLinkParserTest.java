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

import com.io7m.coffeepick.adoptopenjdk.raw.AOJDKGitHubLinkParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class AOJDKGitHubLinkParserTest
{
  @Test
  public void testLinkParserNone_0()
  {
    final var uris = AOJDKGitHubLinkParser.linkURIs(
      "<https://api.github.com/repositories/140418865/releases?page=3>; rel=\"prev\", <https://api.github.com/repositories/140418865/releases?page=1>; rel=\"first\"");

    Assertions.assertEquals(0, uris.size());
  }

  @Test
  public void testLinkParserSome_0()
  {
    final var uris = AOJDKGitHubLinkParser.linkURIs(
      "<https://api.github.com/repositories/140419044/releases?page=2>; rel=\"next\", <https://api.github.com/repositories/140419044/releases?page=4>; rel=\"last\"");

    Assertions.assertEquals(1, uris.size());
    Assertions.assertEquals("https://api.github.com/repositories/140419044/releases?page=2", uris.get(0).toString());
  }
}

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

package com.io7m.coffeepick.adoptopenjdk.raw;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A parser for Link: headers delivered by GitHub.
 */

public final class AOJDKGitHubLinkParser
{
  private static final Pattern URI_MATCHER = Pattern.compile(".*<([0-9a-zA-Z/.:?=_&]+)>.*");

  private AOJDKGitHubLinkParser()
  {

  }

  /**
   * Extract all "next" links from GitHub headers.
   *
   * @param text The input header
   *
   * @return A list of URIs
   */

  public static List<URI> linkURIs(
    final String text)
  {
    Objects.requireNonNull(text, "text");

    final ArrayList<URI> uris = new ArrayList<>();
    final var sections = List.of(text.split(","));
    for (final var section : sections) {
      if (section.contains("rel=\"next\"")) {
        final var matcher = URI_MATCHER.matcher(section);
        if (matcher.matches()) {
          final var uri = URI.create(matcher.group(1));
          uris.add(uri);
        }
      }
    }
    return uris;
  }
}

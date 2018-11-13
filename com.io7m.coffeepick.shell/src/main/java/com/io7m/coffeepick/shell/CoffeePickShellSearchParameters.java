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

package com.io7m.coffeepick.shell;

import com.io7m.coffeepick.api.CoffeePickSearch;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Functions to parse search parameters.
 */

public final class CoffeePickShellSearchParameters
{
  private static final List<AttributeParserType> PARSERS =
    List.of(
      new AttributeArchitecture(),
      new AttributeArchiveSize(),
      new AttributeArchiveURI(),
      new AttributeID(),
      new AttributePlatform(),
      new AttributeRequireTag(),
      new AttributeVersion(),
      new AttributeVM());

  private static final Map<String, AttributeParserType> PARSERS_BY_NAME =
    PARSERS.stream().collect(Collectors.toMap(AttributeParserType::name, Function.identity()));

  private CoffeePickShellSearchParameters()
  {

  }

  /**
   * Parse a set of search parameters.
   *
   * @param parameters The individual search parameters
   *
   * @return A parsed set of parameters
   */

  public static CoffeePickSearch parseSearchParameters(
    final List<String> parameters)
  {
    Objects.requireNonNull(parameters, "parameters");

    final var separator = System.lineSeparator();
    final var builder = CoffeePickSearch.builder();
    for (final var parameter : parameters) {
      final var pieces = List.of(parameter.split(":"));
      if (pieces.size() != 2) {
        throw new IllegalArgumentException(
          new StringBuilder(64)
            .append("Unparseable search parameter.")
            .append(separator)
            .append("  Expected: <attribute>:<value>")
            .append(separator)
            .append("  Received: ")
            .append(parameter)
            .append(separator)
            .toString());
      }

      final var attribute = pieces.get(0);
      final var value = pieces.get(1);

      if (PARSERS_BY_NAME.containsKey(attribute)) {
        PARSERS_BY_NAME.get(attribute).parse(builder, value);
      } else {
        throw new IllegalArgumentException(
          new StringBuilder(64)
            .append("Unrecognized search parameter.")
            .append(separator)
            .append("  Expected: One of ")
            .append(PARSERS_BY_NAME.keySet().stream().collect(Collectors.joining("|")))
            .append(separator)
            .append("  Received: ")
            .append(parameter)
            .append(separator)
            .toString());
      }
    }

    return builder.build();
  }

  private interface AttributeParserType
  {
    String name();

    void parse(
      CoffeePickSearch.Builder builder,
      String value);
  }

  private static final class AttributeRequireTag implements AttributeParserType
  {
    AttributeRequireTag()
    {

    }

    @Override
    public String name()
    {
      return "require-tag";
    }

    @Override
    public void parse(
      final CoffeePickSearch.Builder builder,
      final String value)
    {
      builder.addRequiredTags(value);
    }
  }

  private static final class AttributeArchitecture implements AttributeParserType
  {
    AttributeArchitecture()
    {

    }

    @Override
    public String name()
    {
      return "arch";
    }

    @Override
    public void parse(
      final CoffeePickSearch.Builder builder,
      final String value)
    {
      builder.setArchitecture(value);
    }
  }

  private static final class AttributeVM implements AttributeParserType
  {
    AttributeVM()
    {

    }

    @Override
    public String name()
    {
      return "vm";
    }

    @Override
    public void parse(
      final CoffeePickSearch.Builder builder,
      final String value)
    {
      builder.setVm(value);
    }
  }

  private static final class AttributeArchiveURI implements AttributeParserType
  {
    AttributeArchiveURI()
    {

    }

    @Override
    public String name()
    {
      return "uri";
    }

    @Override
    public void parse(
      final CoffeePickSearch.Builder builder,
      final String value)
    {
      builder.setArchiveURI(URI.create(value));
    }
  }

  private static final class AttributeID implements AttributeParserType
  {
    AttributeID()
    {

    }

    @Override
    public String name()
    {
      return "id";
    }

    @Override
    public void parse(
      final CoffeePickSearch.Builder builder,
      final String value)
    {
      builder.setId(value);
    }
  }

  private static final class AttributeArchiveSize implements AttributeParserType
  {
    AttributeArchiveSize()
    {

    }

    @Override
    public String name()
    {
      return "size";
    }

    @Override
    public void parse(
      final CoffeePickSearch.Builder builder,
      final String value)
    {
      builder.setArchiveSize(Long.parseUnsignedLong(value));
    }
  }

  private static final class AttributePlatform implements AttributeParserType
  {
    AttributePlatform()
    {

    }

    @Override
    public String name()
    {
      return "platform";
    }

    @Override
    public void parse(
      final CoffeePickSearch.Builder builder,
      final String value)
    {
      builder.setPlatform(value);
    }
  }

  private static final class AttributeVersion implements AttributeParserType
  {
    AttributeVersion()
    {

    }

    @Override
    public String name()
    {
      return "version";
    }

    @Override
    public void parse(
      final CoffeePickSearch.Builder builder,
      final String value)
    {
      builder.setVersionRange(CoffeePickShellVersionRanges.parse(value));
    }
  }
}

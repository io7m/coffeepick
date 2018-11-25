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

package com.io7m.coffeepick.runtime.format.xml;

import com.io7m.coffeepick.runtime.RuntimeConfiguration;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.Locator2;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A content handler for parsing runtimes.
 */

public final class FormatXML1RuntimeHandler
  extends FormatXMLAbstractContentHandler<FormatXML1RuntimeChildType, RuntimeDescription>
{
  private final RuntimeDescription.Builder runtime_builder;
  private final Optional<URI> repository;

  /**
   * Construct a handler.
   *
   * @param in_repository The repository URI
   * @param in_locator    The XML locator
   */

  public FormatXML1RuntimeHandler(
    final Optional<URI> in_repository,
    final Locator2 in_locator)
  {
    super(in_locator, Optional.of("runtime"));

    this.repository =
      Objects.requireNonNull(in_repository, "repository");
    this.runtime_builder =
      RuntimeDescription.builder();
  }

  private static FormatXMLContentHandlerType<FormatXML1RuntimeChildType> hashHandler(
    final Locator2 locator)
  {
    return new FormatXML1HashHandler(locator).map(FormatXML1RuntimeChildHash::of);
  }

  private static FormatXMLContentHandlerType<FormatXML1RuntimeChildType> tagsHandler(
    final Locator2 locator)
  {
    return new FormatXML1TagsHandler(locator).map(FormatXML1RuntimeChildTags::of);
  }

  @Override
  protected Map<String, Supplier<FormatXMLContentHandlerType<FormatXML1RuntimeChildType>>> onWantChildHandlers()
  {
    return Map.of(
      "tags", () -> tagsHandler(super.locator()),
      "hash", () -> hashHandler(super.locator())
    );
  }

  @Override
  protected String onWantHandlerName()
  {
    return FormatXML1RuntimeHandler.class.getSimpleName();
  }

  @Override
  protected Optional<RuntimeDescription> onElementFinishDirectly(
    final String namespace,
    final String name,
    final String qname)
  {
    return Optional.of(this.runtime_builder.build());
  }

  @Override
  protected void onElementStartDirectly(
    final String namespace,
    final String name,
    final String qname,
    final Attributes attributes)
    throws SAXParseException
  {
    try {
      this.parseRepositoryURI(attributes);

      this.runtime_builder.setArchiveSize(
        Long.parseUnsignedLong(attributes.getValue("archiveSize")));
      this.runtime_builder.setArchiveURI(
        new URI(attributes.getValue("archive")));
      this.runtime_builder.setConfiguration(
        RuntimeConfiguration.ofName(attributes.getValue("configuration")));
      this.runtime_builder.setPlatform(
        attributes.getValue("platform"));
      this.runtime_builder.setArchitecture(
        attributes.getValue("architecture"));
      this.runtime_builder.setVersion(
        Runtime.Version.parse(attributes.getValue("version")));
      this.runtime_builder.setVm(
        attributes.getValue("vm"));

    } catch (final URISyntaxException | IllegalArgumentException e) {
      throw new SAXParseException(e.getMessage(), this.locator(), e);
    }
  }

  @Override
  protected void onChildResultReceived(
    final FormatXML1RuntimeChildType value)
  {
    switch (value.kind()) {
      case TAGS: {
        final var tags = (FormatXML1RuntimeChildTags) value;
        this.runtime_builder.addAllTags(tags.tags());
        break;
      }
      case HASH: {
        final var hash = (FormatXML1RuntimeChildHash) value;
        this.runtime_builder.setArchiveHash(hash.hash());
        break;
      }
    }
  }

  private void parseRepositoryURI(
    final Attributes attributes)
    throws URISyntaxException
  {
    final var value = attributes.getValue("repository");
    if (value != null) {
      this.runtime_builder.setRepository(new URI(value));
      return;
    }

    if (this.repository.isEmpty()) {
      final var separator = System.lineSeparator();
      throw new IllegalArgumentException(
        new StringBuilder(128)
          .append(
            "A runtime description declared outside of a repository must have an explicit 'repository' attribute.")
          .append(separator)
          .append("  Expected: An XML 'repository' attribute")
          .append(separator)
          .append("  Received: Nothing")
          .toString());
    }

    this.runtime_builder.setRepository(this.repository.get());
  }
}

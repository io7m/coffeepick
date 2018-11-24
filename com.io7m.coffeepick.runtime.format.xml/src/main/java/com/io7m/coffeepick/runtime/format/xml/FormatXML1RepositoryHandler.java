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

import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimeRepositoryDescription;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.Locator2;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

/**
 * A content handler for parsing runtime repositories.
 */

public final class FormatXML1RepositoryHandler
  extends FormatXMLAbstractContentHandler<List<RuntimeDescription>, RuntimeRepositoryDescription>
{
  private final RuntimeRepositoryDescription.Builder repository_builder;
  private URI repository_uri;

  /**
   * Construct a handler.
   *
   * @param in_locator The XML locator
   */

  public FormatXML1RepositoryHandler(
    final Locator2 in_locator)
  {
    super(in_locator, Optional.of("runtime-repository"));
    this.repository_builder = RuntimeRepositoryDescription.builder();
  }

  @Override
  protected Map<String, Supplier<FormatXMLContentHandlerType<List<RuntimeDescription>>>> onWantChildHandlers()
  {
    return Map.of(
      "runtimes", () -> new FormatXML1RuntimesHandler(this.repository_uri, super.locator())
    );
  }

  @Override
  protected String onWantHandlerName()
  {
    return FormatXML1RepositoryHandler.class.getSimpleName();
  }

  @Override
  protected Optional<RuntimeRepositoryDescription> onElementFinishDirectly(
    final String namespace,
    final String name,
    final String qname)
  {
    return Optional.of(this.repository_builder.build());
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
      this.repository_uri = new URI(attributes.getValue("id"));
      this.repository_builder.setId(this.repository_uri);

      {
        final var updated = attributes.getValue("updated");
        if (updated != null) {
          this.repository_builder.setUpdated(OffsetDateTime.parse(updated, ISO_OFFSET_DATE_TIME));
        }
      }

    } catch (final URISyntaxException | IllegalArgumentException e) {
      throw new SAXParseException(e.getMessage(), this.locator(), e);
    }
  }

  @Override
  protected void onChildResultReceived(final List<RuntimeDescription> runtimes)
  {
    for (final var runtime : runtimes) {
      this.repository_builder.putRuntimes(runtime.id(), runtime);
    }
  }
}

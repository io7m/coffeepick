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
import com.io7m.coffeepick.runtime.RuntimeHash;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * A content handler for parsing runtimes.
 */

public final class FormatXML1RuntimeHandler
  implements FormatXMLContentHandlerType<RuntimeDescription>
{
  private final RuntimeDescription.Builder runtime_builder;
  private final URI repository;
  private FormatXMLContentHandlerType<?> handler;

  /**
   * Construct a handler.
   *
   * @param in_repository The repository URI
   */

  public FormatXML1RuntimeHandler(
    final URI in_repository)
  {
    this.repository =
      Objects.requireNonNull(in_repository, "repository");
    this.runtime_builder =
      RuntimeDescription.builder();
  }

  @Override
  public void onElementStarted(
    final String namespace_uri,
    final String local_name,
    final String qualified_name,
    final Attributes attributes)
    throws SAXException
  {
    switch (local_name) {
      case "runtime": {
        try {
          this.runtime_builder.setRepository(
            this.repository);
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
        } catch (final URISyntaxException e) {
          throw new SAXException(e);
        }
        break;
      }
      case "tags": {
        this.handler = new FormatXML1TagsHandler();
        this.handler.onElementStarted(namespace_uri, local_name, qualified_name, attributes);
        break;
      }
      case "hash": {
        this.handler = new FormatXML1HashHandler();
        this.handler.onElementStarted(namespace_uri, local_name, qualified_name, attributes);
        break;
      }
      default: {
        this.handler.onElementStarted(namespace_uri, local_name, qualified_name, attributes);
        break;
      }
    }
  }

  @Override
  public void onElementFinished(
    final String namespace_uri,
    final String local_name,
    final String qualified_name)
    throws SAXException
  {
    switch (local_name) {
      case "runtime": {
        break;
      }
      case "tags": {
        this.handler.onElementFinished(namespace_uri, local_name, qualified_name);
        this.runtime_builder.setTags((Iterable<String>) this.handler.get());
        this.handler = null;
        break;
      }
      case "hash": {
        this.handler.onElementFinished(namespace_uri, local_name, qualified_name);
        this.runtime_builder.setArchiveHash((RuntimeHash) this.handler.get());
        break;
      }
      default: {
        this.handler.onElementFinished(namespace_uri, local_name, qualified_name);
        break;
      }
    }
  }

  @Override
  public void onCharacters(
    final char[] ch,
    final int start,
    final int length)
    throws SAXException
  {
    this.handler.onCharacters(ch, length, start);
  }

  @Override
  public RuntimeDescription get()
  {
    return this.runtime_builder.build();
  }
}

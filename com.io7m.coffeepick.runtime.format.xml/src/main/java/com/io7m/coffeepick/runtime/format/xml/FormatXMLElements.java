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

import com.io7m.coffeepick.runtime.RuntimeBuild;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimeHash;
import com.io7m.coffeepick.runtime.RuntimeRepositoryDescription;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Objects;
import java.util.Set;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

/**
 * Functions to transform values into XML elements.
 */

public final class FormatXMLElements
{
  private static final String NAMESPACE =
    FormatXMLConstants.SCHEMA_1_0_NAMESPACE.toString();

  private final DocumentBuilderFactory document_builders;

  /**
   * Construct an element producer.
   */

  public FormatXMLElements()
  {
    this.document_builders = DocumentBuilderFactory.newDefaultInstance();
  }

  /**
   * Serialize the given repository, using the given document to create elements.
   *
   * @param document   The target document
   * @param repository The repository
   *
   * @return A serialized repository
   */

  public static Element ofRepository(
    final Document document,
    final RuntimeRepositoryDescription repository)
  {
    Objects.requireNonNull(document, "document");
    Objects.requireNonNull(repository, "repository");

    final var root = document.createElementNS(NAMESPACE, "c:runtime-repository");

    root.setAttribute("id", repository.id().toString());
    repository.updated()
      .ifPresent(time -> root.setAttribute("updated", ISO_OFFSET_DATE_TIME.format(time)));

    final var runtimes = document.createElementNS(NAMESPACE, "c:runtimes");
    root.appendChild(runtimes);

    for (final var runtime : repository.runtimes().values()) {
      runtimes.appendChild(ofRuntime(document, runtime, false));
    }
    return root;
  }

  /**
   * Serialize the given runtime, using the given document to create elements.
   *
   * @param document          The target document
   * @param runtime           The runtime
   * @param append_repository True if the repository ID should be included in the element
   *
   * @return A serialized runtime
   */

  public static Element ofRuntime(
    final Document document,
    final RuntimeDescription runtime,
    final boolean append_repository)
  {
    Objects.requireNonNull(document, "document");
    Objects.requireNonNull(runtime, "runtime");

    final var e_runtime = document.createElementNS(NAMESPACE, "c:runtime");
    e_runtime.setAttribute("architecture", runtime.architecture());
    e_runtime.setAttribute("archive", runtime.archiveURI().toString());
    e_runtime.setAttribute("archiveSize", Long.toUnsignedString(runtime.archiveSize()));
    e_runtime.setAttribute("configuration", runtime.configuration().configurationName());
    e_runtime.setAttribute("platform", runtime.platform());
    e_runtime.setAttribute("version", runtime.version().toString());
    e_runtime.setAttribute("vm", runtime.vm());

    if (append_repository) {
      e_runtime.setAttribute("repository", runtime.repository().toString());
    }

    e_runtime.appendChild(ofHash(document, runtime.archiveHash()));
    e_runtime.appendChild(ofTags(document, runtime.tags()));
    runtime.build().ifPresent(build -> e_runtime.appendChild(ofBuild(document, build)));
    return e_runtime;
  }

  /**
   * Serialize the given set of tags, using the given document to create elements.
   *
   * @param document The target document
   * @param tags     The set of tags
   *
   * @return A serialized set of tags
   */

  public static Element ofTags(
    final Document document,
    final Set<String> tags)
  {
    Objects.requireNonNull(document, "document");
    Objects.requireNonNull(tags, "tags");

    final var e_tags = document.createElementNS(NAMESPACE, "c:tags");
    for (final var tag : tags) {
      e_tags.appendChild(ofTag(document, tag));
    }
    return e_tags;
  }

  /**
   * Serialize the given tag, using the given document to create elements.
   *
   * @param document The target document
   * @param tag      The tag
   *
   * @return A serialized tag
   */

  public static Element ofTag(
    final Document document,
    final String tag)
  {
    Objects.requireNonNull(document, "document");
    Objects.requireNonNull(tag, "tag");

    final var e_tag = document.createElementNS(NAMESPACE, "c:tag");
    e_tag.setAttribute("name", tag);
    return e_tag;
  }

  /**
   * Serialize the given hash, using the given document to create elements.
   *
   * @param document The target document
   * @param hash     The hash
   *
   * @return A serialized hash
   */

  public static Element ofHash(
    final Document document,
    final RuntimeHash hash)
  {
    Objects.requireNonNull(document, "document");
    Objects.requireNonNull(hash, "hash");

    final var e_hash = document.createElementNS(NAMESPACE, "c:hash");
    e_hash.setAttribute("algorithm", hash.algorithm());
    e_hash.setAttribute("value", hash.value());
    return e_hash;
  }

  /**
   * Serialize the given build, using the given document to create elements.
   *
   * @param document The target document
   * @param build    The build
   *
   * @return A serialized build
   */

  public static Element ofBuild(
    final Document document,
    final RuntimeBuild build)
  {
    Objects.requireNonNull(document, "document");
    Objects.requireNonNull(build, "build");

    final var e_hash = document.createElementNS(NAMESPACE, "c:build");
    e_hash.setAttribute("number", build.buildNumber());
    e_hash.setAttribute("time", ISO_OFFSET_DATE_TIME.format(build.time()));
    return e_hash;
  }

  /**
   * Create a new document from the given repository.
   *
   * @param repository The repository
   *
   * @return A document
   *
   * @throws ParserConfigurationException On configuration errors
   */

  public Document ofRepository(
    final RuntimeRepositoryDescription repository)
    throws ParserConfigurationException
  {
    Objects.requireNonNull(repository, "repository");

    final var document = this.document();
    final var root = ofRepository(document, repository);
    document.appendChild(root);
    return document;
  }

  /**
   * Create a new empty document.
   *
   * @return An empty document
   *
   * @throws ParserConfigurationException On configuration errors
   */

  public Document document()
    throws ParserConfigurationException
  {
    final var builder = this.document_builders.newDocumentBuilder();
    final var document = builder.newDocument();
    document.setStrictErrorChecking(true);
    document.setXmlStandalone(true);
    return document;
  }
}

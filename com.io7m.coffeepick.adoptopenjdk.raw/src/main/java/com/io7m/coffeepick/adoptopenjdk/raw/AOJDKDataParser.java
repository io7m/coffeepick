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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Functions to parse raw GitHub release data.
 */

public final class AOJDKDataParser
{
  private static final Logger LOG =
    LoggerFactory.getLogger(AOJDKDataParser.class);

  private static final Pattern SHA256_PATTERN =
    Pattern.compile("\\.sha256\\.txt$");

  private final InputStream stream;
  private final ObjectMapper mapper;

  private AOJDKDataParser(
    final InputStream in_stream)
  {
    this.stream = Objects.requireNonNull(in_stream, "stream");
    this.mapper = new ObjectMapper();
  }

  /**
   * Create a parser for the given input stream.
   *
   * @param stream The input stream
   *
   * @return A parser
   */

  public static AOJDKDataParser create(
    final InputStream stream)
  {
    return new AOJDKDataParser(stream);
  }

  private static List<AOJDKArchive> parseArchive(
    final ObjectNode tree)
  {
    final var name = tree.get("name");
    LOG.debug("name: {}", name);

    final var results = new ArrayList<AOJDKArchive>(64);
    final var assets = tree.get("assets");
    if (assets != null && assets.isArray()) {
      final var asset_array = (ArrayNode) assets;
      final var checksums = new HashMap<String, URI>(64);
      for (var index = 0; index < asset_array.size(); ++index) {
        try {
          parseChecksums(checksums, asset_array, index);
        } catch (final Exception e) {
          LOG.error("unable to parse checksum: ", e);
        }
      }

      for (var index = 0; index < asset_array.size(); ++index) {
        try {
          parseAsset(results, checksums, asset_array, index);
        } catch (final Exception e) {
          LOG.error("unable to parse asset: ", e);
        }
      }
    }

    return results;
  }

  private static void parseChecksums(
    final HashMap<String, URI> checksums,
    final ArrayNode asset_array,
    final int index)
    throws URISyntaxException
  {
    final var asset = asset_array.get(index);
    if (asset.isObject()) {
      final var asset_object = (ObjectNode) asset;

      final var name =
        Objects.requireNonNull(asset_object.get("name"), "name")
          .textValue();
      final var content =
        Objects.requireNonNull(asset_object.get("content_type"), "content_type")
          .textValue();

      if (Objects.equals(content, "text/plain")) {
        addChecksum(checksums, asset_object, name);
      }
    }
  }

  private static void parseAsset(
    final ArrayList<AOJDKArchive> results,
    final HashMap<String, URI> checksums,
    final ArrayNode asset_array,
    final int index)
    throws URISyntaxException
  {
    final var asset = asset_array.get(index);
    if (asset.isObject()) {
      final var asset_object = (ObjectNode) asset;

      final var name =
        Objects.requireNonNull(asset_object.get("name"), "name")
          .textValue();
      final var content =
        Objects.requireNonNull(asset_object.get("content_type"), "content_type")
          .textValue();
      final var download_url =
        Objects.requireNonNull(asset_object.get("browser_download_url"), "browser_download_url")
          .textValue();
      final var size =
        Objects.requireNonNull(asset_object.get("size"), "size").asLong();

      if (!Objects.equals(content, "text/plain")) {
        if (!checksums.containsKey(name)) {
          throw new IllegalStateException(
            new StringBuilder(64)
              .append("Checksum missing for file")
              .append(System.lineSeparator())
              .append("  File: ")
              .append(name)
              .toString());
        }

        final var checksum_uri = checksums.get(name);
        final var meta = AOJDKFilenameMetadataParsing.parseFilename(name);
        if (!meta.isPresent()) {
          return;
        }

        final var archive =
          AOJDKArchive.builder()
            .setMetadata(meta.get())
            .setArchiveURI(new URI(download_url))
            .setArchiveSize(size)
            .setArchiveChecksumURI(checksum_uri)
            .build();

        results.add(archive);
      }
    }
  }

  private static void addChecksum(
    final HashMap<String, URI> checksums,
    final ObjectNode asset_object,
    final String file)
    throws URISyntaxException
  {
    final var original = SHA256_PATTERN.matcher(file).replaceAll("");

    final var download_url =
      Objects.requireNonNull(asset_object.get("browser_download_url"), "browser_download_url")
        .textValue();

    if (checksums.containsKey(original)) {
      throw new IllegalArgumentException(
        new StringBuilder(128)
          .append("File already has a checksum")
          .append(System.lineSeparator())
          .append("  File:              ")
          .append(original)
          .append(System.lineSeparator())
          .append("  Existing Checksum: ")
          .append(checksums.get(original))
          .append(System.lineSeparator())
          .append("  Current Checksum:  ")
          .append(download_url)
          .append(System.lineSeparator())
          .toString());
    }

    LOG.debug("checksum: {} -> {}", original, download_url);
    checksums.put(original, new URI(download_url));
  }

  /**
   * @return A list of parsed archives
   *
   * @throws IOException On I/O and parser errors
   */

  public List<AOJDKArchive> parse()
    throws IOException
  {
    final var tree = this.mapper.readTree(this.stream);
    if (tree.isArray()) {
      return AOJDKDataParser.parseArchives((ArrayNode) tree);
    }
    if (tree.isObject()) {
      return parseArchive((ObjectNode) tree);
    }

    final var separator = System.lineSeparator();
    throw new IOException(
      new StringBuilder(128)
        .append("The root element of the list of releases is of the wrong type.")
        .append(separator)
        .append("  Expected: An Object or Array")
        .append(separator)
        .append("  Received: ")
        .append(tree.getNodeType().name())
        .append(separator)
        .toString());
  }

  private static List<AOJDKArchive> parseArchives(
    final ArrayNode tree)
  {
    final List<AOJDKArchive> descriptions = new ArrayList<>(tree.size() * 8);

    for (var index = 0; index < tree.size(); ++index) {
      final var node = tree.get(index);
      if (node.isObject()) {
        descriptions.addAll(parseArchive((ObjectNode) node));
      }
    }

    return descriptions;
  }
}

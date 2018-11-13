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

package com.io7m.coffeepick.openjdk_java_net;

import com.io7m.coffeepick.repository.spi.RuntimeRepositoryType;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimeDescriptions;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A repository based on https://jdk.openjdk.net
 */

@Component(service = RuntimeRepositoryType.class)
public final class OJNRepository implements RuntimeRepositoryType
{
  private static final Logger LOG = LoggerFactory.getLogger(OJNRepository.class);

  private static final URI URI = java.net.URI.create("https://jdk.openjdk.net");
  private static final String BUILDS = "/com/io7m/coffeepick/openjdk_java_net/build.properties";

  /**
   * Construct a repository.
   */

  public OJNRepository()
  {

  }

  @Override
  public URI uri()
  {
    return URI;
  }

  @Override
  public String name()
  {
    return "jdk.java.net";
  }

  @Override
  public List<RuntimeDescription> availableRuntimes()
    throws IOException
  {
    try {
      try (var stream = OJNRepository.class.getResourceAsStream(BUILDS)) {
        final var builds = new Properties();
        builds.load(stream);

        return Stream.of(builds.getProperty("coffeepick.builds").split(" "))
          .map(OJNRepository::loadBuild)
          .collect(Collectors.toList());
      }
    } catch (final UncheckedIOException e) {
      throw e.getCause();
    }
  }

  private static RuntimeDescription loadBuild(
    final String file)
  {
    LOG.debug("loading build {}", file);

    try (var build_stream = OJNRepository.class.getResourceAsStream(file)) {
      final var build = new Properties();
      build.load(build_stream);
      return RuntimeDescriptions.parseFromProperties(build);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}

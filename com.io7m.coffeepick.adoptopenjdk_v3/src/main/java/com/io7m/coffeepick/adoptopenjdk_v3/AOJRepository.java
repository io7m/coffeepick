/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> http://io7m.com
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

package com.io7m.coffeepick.adoptopenjdk_v3;

import com.io7m.coffeepick.repository.spi.RuntimeRepositoryContextType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventUpdateFailed;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventUpdateFinished;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventUpdateRunning;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryEventUpdateStarted;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryProviderType;
import com.io7m.coffeepick.repository.spi.RuntimeRepositoryType;
import com.io7m.coffeepick.runtime.RuntimeBuild;
import com.io7m.coffeepick.runtime.RuntimeConfiguration;
import com.io7m.coffeepick.runtime.RuntimeDescription;
import com.io7m.coffeepick.runtime.RuntimeHash;
import com.io7m.coffeepick.runtime.RuntimeRepositoryDescription;
import com.io7m.coffeepick.runtime.RuntimeVersion;
import com.io7m.coffeepick.runtime.database.RuntimeDescriptionDatabase;
import com.io7m.junreachable.UnreachableCodeException;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import net.adoptopenjdk.v3.api.AOV3Architecture;
import net.adoptopenjdk.v3.api.AOV3ClientType;
import net.adoptopenjdk.v3.api.AOV3Error;
import net.adoptopenjdk.v3.api.AOV3Exception;
import net.adoptopenjdk.v3.api.AOV3ExceptionHTTPRequestFailed;
import net.adoptopenjdk.v3.api.AOV3ImageKind;
import net.adoptopenjdk.v3.api.AOV3JVMImplementation;
import net.adoptopenjdk.v3.api.AOV3OperatingSystem;
import net.adoptopenjdk.v3.api.AOV3Release;
import net.adoptopenjdk.v3.api.AOV3ReleaseKind;
import net.adoptopenjdk.v3.api.AOV3VersionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

public final class AOJRepository implements RuntimeRepositoryType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(AOJRepository.class);

  private final AOV3ClientType client;
  private final RuntimeRepositoryContextType context;
  private final PublishSubject<RuntimeRepositoryEventType> events;
  private final RuntimeRepositoryProviderType provider;
  private final RuntimeDescriptionDatabase database;
  private final RuntimeRepositoryDescription description;

  private AOJRepository(
    final AOV3ClientType inClient,
    final RuntimeRepositoryContextType inContext,
    final PublishSubject<RuntimeRepositoryEventType> inEvents,
    final RuntimeRepositoryProviderType inProvider,
    final RuntimeDescriptionDatabase inDatabase,
    final RuntimeRepositoryDescription inDescription)
  {
    this.client =
      Objects.requireNonNull(inClient, "client");
    this.context =
      Objects.requireNonNull(inContext, "context");
    this.events =
      Objects.requireNonNull(inEvents, "events");
    this.provider =
      Objects.requireNonNull(inProvider, "provider");
    this.database =
      Objects.requireNonNull(inDatabase, "database");
    this.description =
      Objects.requireNonNull(inDescription, "description");
  }

  public static RuntimeRepositoryType create(
    final AOV3ClientType client,
    final RuntimeRepositoryProviderType provider,
    final RuntimeRepositoryContextType context)
    throws IOException
  {
    Objects.requireNonNull(client, "client");
    Objects.requireNonNull(provider, "provider");
    Objects.requireNonNull(context, "context");

    final var events =
      PublishSubject.<RuntimeRepositoryEventType>create();

    final var description =
      RuntimeRepositoryDescription.builder()
        .setId(AOJRepositoryProvider.PROVIDER_URI)
        .setUpdated(OffsetDateTime.now(ZoneId.of("UTC")))
        .build();

    final var database =
      RuntimeDescriptionDatabase.open(
        context.cacheDirectory().resolve("net.adoptopenjdk")
      );

    return new AOJRepository(
      client,
      context,
      events,
      provider,
      database,
      description
    );
  }

  @Override
  public Observable<RuntimeRepositoryEventType> events()
  {
    return this.events;
  }

  @Override
  public RuntimeRepositoryProviderType provider()
  {
    return this.provider;
  }

  @Override
  public void update(
    final BooleanSupplier cancelled)
    throws Exception, CancellationException
  {
    Objects.requireNonNull(cancelled, "cancelled");

    try {
      this.events.onNext(
        RuntimeRepositoryEventUpdateStarted.builder()
          .setRepository(this.provider.uri())
          .build());

      this.events.onNext(
        RuntimeRepositoryEventUpdateRunning.builder()
          .setRepository(this.provider.uri())
          .setProgress(0.0)
          .build());

      final var releaseRequest =
        this.client.availableReleases(AOJRepository::onAOv3Error);

      final var releases = releaseRequest.execute();
      final var releaseCount = releases.availableReleases().size();
      var releaseIndex = 0;

      for (final var release : releases.availableReleases()) {
        if (cancelled.getAsBoolean()) {
          throw new CancellationException();
        }

        final var progress = (double) releaseIndex / (double) releaseCount;
        this.events.onNext(
          RuntimeRepositoryEventUpdateRunning.builder()
            .setRepository(this.provider.uri())
            .setProgress(progress)
            .build());

        this.processForRelease(cancelled, this.fetchForRelease(release));
        ++releaseIndex;
      }

      this.events.onNext(
        RuntimeRepositoryEventUpdateFinished.builder()
          .setRepository(this.provider.uri())
          .build());
    } catch (final Exception e) {
      this.events.onNext(
        RuntimeRepositoryEventUpdateFailed.builder()
          .setRepository(this.provider.uri())
          .setException(e)
          .build());
      throw e;
    }
  }

  private void processForRelease(
    final BooleanSupplier cancelled,
    final ArrayList<AOV3Release> releases)
  {
    for (final var release : releases) {
      for (final var binary : release.binaries()) {
        if (cancelled.getAsBoolean()) {
          throw new CancellationException();
        }

        try {
          final var package_ = binary.package_();
          final var checksumOpt = package_.checksum();
          if (checksumOpt.isPresent()) {
            final var checksum = checksumOpt.get();
            if (checksum.isBlank()) {
              LOG.warn(
                "received invalid blank checksum for release {}",
                release.id());
              continue;
            }

            if (binary.imageType() == AOV3ImageKind.TESTIMAGE) {
              LOG.debug(
                "ignoring {} for release {}",
                binary.imageType(),
                release.id());
              continue;
            }

            final var hash =
              RuntimeHash.of("SHA-256", checksum);
            final var runtimeDescription =
              RuntimeDescription.builder()
                .setArchitecture(archOfAOV3Architecture(binary.architecture()))
                .setArchiveHash(hash)
                .setArchiveSize(package_.size().longValue())
                .setArchiveURI(package_.link())
                .setBuild(buildOfAOV3Build(
                  release.timestamp(),
                  release.versionData()))
                .setConfiguration(configurationOfAOV3Binary(binary.imageType()))
                .setPlatform(platformOfAOV3OperatingSystem(binary.operatingSystem()))
                .setRepository(AOJRepositoryProvider.PROVIDER_URI)
                .setVersion(versionOfAOV3VersionData(release.versionData()))
                .setVm(vmOfAOV3JVM(binary.jvmImplementation()))
                .build();

            this.database.add(runtimeDescription);
          }
        } catch (final Exception e) {
          LOG.error("error parsing runtime: ", e);
        }
      }
    }
  }

  private static RuntimeVersion versionOfAOV3VersionData(
    final AOV3VersionData versionData)
  {
    return RuntimeVersion.builder()
      .setMajor(versionData.major())
      .setMinor(versionData.minor())
      .setPatch(versionData.security())
      .setBuild(versionData.adoptBuildNumber())
      .build();
  }

  private static RuntimeConfiguration configurationOfAOV3Binary(
    final AOV3ImageKind image)
  {
    switch (image) {
      case JDK:
        return RuntimeConfiguration.JDK;
      case JRE:
        return RuntimeConfiguration.JRE;
      case TESTIMAGE:
        throw new UnreachableCodeException();
    }
    throw new UnreachableCodeException();
  }

  private static String vmOfAOV3JVM(
    final AOV3JVMImplementation jvmImplementation)
  {
    return jvmImplementation.nameText().toLowerCase(Locale.ROOT);
  }

  private static RuntimeBuild buildOfAOV3Build(
    final OffsetDateTime timestamp,
    final AOV3VersionData version)
  {
    return RuntimeBuild.builder()
      .setBuildNumber(version.adoptBuildNumber().toString())
      .setTime(timestamp)
      .build();
  }

  private static String platformOfAOV3OperatingSystem(
    final AOV3OperatingSystem operatingSystem)
  {
    switch (operatingSystem) {
      case AIX:
        return "aix";
      case LINUX:
        return "linux";
      case MAC:
        return "macos";
      case SOLARIS:
        return "solaris";
      case WINDOWS:
        return "windows";
    }
    throw new UnreachableCodeException();
  }

  private static String archOfAOV3Architecture(
    final AOV3Architecture architecture)
  {
    return architecture.nameText().toLowerCase(Locale.ROOT);
  }

  private ArrayList<AOV3Release> fetchForRelease(
    final BigInteger release)
    throws AOV3Exception
  {
    var page = BigInteger.ZERO;
    final var pageSize = 250;
    final var releases = new ArrayList<AOV3Release>(128);

    while (true) {
      try {
        final var assetsRequest =
          this.client.assetsForRelease(
            AOJRepository::onAOv3Error,
            page,
            BigInteger.valueOf(pageSize),
            release,
            AOV3ReleaseKind.GENERAL_AVAILABILITY,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
          );

        final List<AOV3Release> assets = assetsRequest.execute();
        releases.addAll(assets);
        LOG.debug(
          "[{}]: retrieved {} releases for page {}",
          release,
          Integer.valueOf(assets.size()),
          page
        );

        if (assets.size() >= pageSize) {
          page = page.add(BigInteger.ONE);
        } else {
          break;
        }
      } catch (final AOV3ExceptionHTTPRequestFailed e) {
        if (e.statusCode() >= 400 && e.statusCode() < 500) {
          try {
            Thread.sleep(5_000L);
          } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
          }
        } else {
          throw e;
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    LOG.debug(
      "[{}]: retrieved {} releases in total",
      release,
      Integer.valueOf(releases.size())
    );
    return releases;
  }

  private static void onAOv3Error(
    final AOV3Error error)
  {
    LOG.error("error: {}", error.show());
    error.exception().ifPresent(ex -> LOG.error("exception: ", ex));
  }

  @Override
  public Map<String, RuntimeDescription> runtimes()
  {
    return this.database.descriptions();
  }

  @Override
  public RuntimeRepositoryDescription description()
  {
    return RuntimeRepositoryDescription.builder()
      .setId(this.provider.uri())
      .setRuntimes(Map.copyOf(this.runtimes()))
      .build();
  }
}

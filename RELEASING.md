# Releasing OWASP Java Encoder

## Release gate for 1.5

Do not tag, publish, or announce a 1.5 release until **all open issues and all
open pull requests have been handled**. Before considering release approval,
inventory the full open backlog and record the outcome and supporting review
or verification for every item. Completing a maintenance batch does not satisfy
this gate on its own. Keep 1.5 development at `1.5.0-SNAPSHOT`; snapshot version
changes and reviewed maintenance merges are not release approval.

The pending Central publication of the already signed 1.4.1 release is separate:
when access is available, publish the retained exact signed bundle and verify
the published artifacts. Do not rebuild or replace 1.4.1 artifacts or move its tag.

## Publishing access and project identity

See [MAINTAINERS.md](MAINTAINERS.md) for named maintainers, security contacts,
signing-key custodians, independent recovery procedures, and the dated status
of outstanding custody and publishing checks.

Artifact signing and permission to publish Maven coordinates are separate.
The release key is the dedicated **OWASP Java Encoder Release** key in `KEYS`;
a maintainer's personal signing key is not required. The maintainer uploading a
release must also have Central Portal access to `org.owasp.encoder` (or a parent
namespace) and a Portal publishing token, or an authenticated Portal session.

Manage publishing access through the Central Portal organization. If the current
publisher is unavailable and no other organization administrator can grant
access, contact Central Support with evidence of project ownership and coordinate
with the OWASP Foundation. GitHub administrator access does not itself grant
Maven Central publishing access.

- [Central organization access](https://central.sonatype.org/publish/publish-portal-organizations/)
- [Central signing requirements](https://central.sonatype.org/publish/requirements/gpg/)
- [Portal bundle uploads](https://central.sonatype.org/publish/publish-portal-upload/)

Jim Manico and Jeremy Long are each responsible for maintaining a complete
recovery copy in their own vault and performing the
[per-custodian recovery drill](MAINTAINERS.md#recovery-drill-for-each-custodian).
Confirm each person's results independently; a local backup test does not
establish either vault's recovery readiness or Central publishing access. Keep
private key material and tokens outside the checkout and build logs. The public
key and full fingerprint belong in `KEYS` and on a Central-supported keyserver.
A rotation creates a new project key and updates that public record and both
recovery copies; it does not replace historical signatures or republish an
existing Maven version.

## Prepare and verify

1. Use a clean checkout, **Eclipse Temurin 17.0.20.1+1** and the committed
   wrapper (**Maven 3.9.16**). The signing profile enforces vendor, exact runtime
   and Maven versions, and a non-SNAPSHOT project version. Ordinary builds allow
   JDK 17+; the release path is stricter because javac output changes on newer
   JDKs. Record the full Git commit and operating system/architecture. Build with a fresh Maven local repository so
   locally installed artifacts cannot hide dependency or version errors.
2. Apply reviewed security fixes privately until publication is ready.
3. Set the version in the root POM, each library module's parent POM, and
   `encoder.version` in `jakarta-test/pom.xml`. Set the root SCM tag to `v<VERSION>`.
   `python3 scripts/check-ci-version.py` requires `-SNAPSHOT` plus SCM `HEAD`
   during development. The explicit non-snapshot version plus matching
   `v<VERSION>` SCM tag is the intentional release-commit form; review these
   changes together. This permits CI on the reviewed release PR/commit before
   creating a tag. The guard is a consistency check, not release approval.
4. Update README dependency examples, the security policy's supported versions,
   and the release notes. Include security advisories, compatibility changes,
   all Maven coordinates, signing fingerprint, and verification commands.
5. Record a reviewed `project.build.outputTimestamp` (UTC timestamp of the
   prepared release sources) in the release POM. Never use build wall-clock time.
   Set `TZ=UTC`, `LC_ALL=C`, `LANG=C`, and
   `MAVEN_OPTS="-Duser.language=en -Duser.country=US -Duser.timezone=UTC -Dfile.encoding=UTF-8"`.
   Run `./mvnw -B -ntp -Dmaven.repo.local=<fresh-cache> clean verify`.
   This checks unit tests, coverage, source policy, API signatures and the
   in-reactor OSGi/JPMS tests. Then run the isolated consumer commands in
   [BUILDING.md](BUILDING.md) on the original JARs. With a running
   Docker-compatible runtime, also run `./mvnw -B -ntp
   -Dmaven.repo.local=<fresh-cache> verify -PtestJakarta`.
6. Commit the release files before tagging. Verify the four binary JARs, their
   source and Javadoc JARs, and five POMs. The optional `jakarta-test` WAR is not a
   published component.

## Sign and stage

For the Maven deploy path, configure a `central` server in a private Maven
settings file using a Central Portal token. Select the full project-key
fingerprint and dedicated GnuPG home explicitly:

```sh
./mvnw -B -ntp -s /private/path/settings.xml \
  -Dmaven.repo.local=/private/path/release-cache \
  -DperformRelease=true \
  -Dgpg.homedir=/private/path/project-gnupg \
  -Dgpg.keyname=1C5F632B86809F2F5DB25092BEA0075F94074A9B \
  clean deploy
```

Use the GnuPG agent to unlock the project key before starting the batch command;
GPG plugin 3.2.8 enables `bestPractices` and does not accept passphrases in POMs or
command-line properties. Noninteractive signing must fail when the key/agent is
unavailable; do not work around failure by skipping signatures for a production
bundle. Run `clean verify -DperformRelease=true` first to test signing without
deployment. For a local bundle, run signed `clean verify` first, then:

```sh
python3 scripts/package-release.py --output /private/path/release-bundle.zip \
  --gnupg-home /private/path/project-gnupg \
  --fingerprint 1C5F632B86809F2F5DB25092BEA0075F94074A9B
```

The assembler verifies all seventeen signatures against that expected fingerprint,
checks the signed POMs match the source POMs, excludes the optional WAR, generates
four checksum types and refuses to overwrite an existing bundle. It never builds,
signs or uploads. Do **not** use `skipPublishing=true` as a bundle-generation
command: despite upstream documentation, 0.11.0 filters out every artifact before
bundling. It also requires a `central` server settings entry even in that mode;
the isolated validation used dummy values and confirmed no upload occurred.
The publisher extension loads only in the signing profile. The current POM uses
`autoPublish=false`: deployment stages the release and does not publish it.
Inspect the deployment in Central Portal and resolve validation errors before
selecting **Publish**.

The Portal also accepts a ZIP bundle containing the repository layout
`org/owasp/encoder/<artifactId>/<version>/`. Include the parent POM and each
library's POM, binary JAR, source JAR, and Javadoc JAR. Sign every POM and JAR
with an armored detached signature (`gpg --local-user <fingerprint> --armor
--detach-sign <file>`). Include the required MD5/SHA-1 checksums and also
SHA-256/SHA-512 checksums for the artifacts. Verify every signature before
uploading. Keep an audit record of the exact uploaded bundle and its SHA-256.

## Publish and resume development

1. Wait for Central Portal validation, publish, and confirm the exact new POMs
   and binaries are downloadable from Maven Central. For an emergency release
   blocked on namespace access, publish the signed GitHub assets first, clearly
   label Central publication as pending in the README and release notes, and
   retain the exact signed bundle for later Central publication.
2. Create and verify a signed `v<VERSION>` tag on the release commit, using the
   project key explicitly. Push the release commit and tag without rewriting
   existing release tags.
3. Publish a GitHub release with the binary/source/Javadoc JARs, POMs, signatures,
   `KEYS`, and signed SHA-256/SHA-512 checksum manifests from the same build.
4. Set the fixed versions in the security advisories and publish the advisories
   in coordination with the available release. Do not announce a Central version
   that has not actually published.
5. Set main to the next development version (currently `1.5.0-SNAPSHOT` for the
   new JSON API), update
   `jakarta-test` accordingly, and reset the SCM tag to `HEAD`. README examples
   and the supported-version table continue to refer to the published release.
6. Verify GitHub CI on main, update the OWASP project page, and check javadoc.io
   after its indexing delay.

If staging fails, repair the cause and drop the failed staging deployment before
retrying. For the pending 1.4.1 delivery, correct access or upload problems and
retry the retained exact bundle; do not rebuild or re-sign it to address a
validation failure. Escalate a failure requiring different artifact bytes to
the release coordinator. After publication, compare all four libraries' binary,
source, and Javadoc JARs and all five POMs and their signatures from Central with
the retained files and signed checksums. Only after that comparison succeeds,
reconcile the Central-pending notices in README, SECURITY.md, release notes,
the GitHub release, and the ESAPI consumer guidance. Check the OWASP project page
and javadoc.io against the actual publication status as well.

Published Maven coordinates are immutable. If release tags are
protected, an incorrect tag requires a maintainer to resolve the protection and
correction explicitly; never silently move an existing release tag.

## Maven storage and repository controls

CI uses `verify` for ordinary builds, including `-PtestJakarta`; it does not need
to install the reactor. The Java 8 test-JVM job deliberately installs libraries
for its second Maven invocation, always in an isolated, uncached runner-temporary
repository. Release builds use the same isolation. No workflow restores or saves
Maven repositories, and packaged consumers use only artifacts from their own
workflow run. See [CI/security operations](.github/CI_SECURITY.md).

If an older local build installed a released encoder version, inspect only the
affected `~/.m2/repository/org/owasp/encoder/<artifact>/<version>` directories.
Look for local-install provenance in `_remote.repositories` (entries without a
remote repository), compare POM/JAR hashes and signatures with the actual Central
or retained signed release, and move uncertain version directories to a dated
quarantine outside the repository. Re-resolve those coordinates using a fresh
local repository. Do not erase the entire encoder repository: other versions,
snapshots and the exact signed 1.4.1 artifacts may be intentional. Never replace
or publish a release to repair local cache contamination.

All Git tags are protected against update and deletion. GitHub commit-signature
rules do not verify annotated tag signatures: run `git verify-tag v<VERSION>`
and check the project key fingerprint separately. Repository recovery and the
limited, audit-visible emergency PR review bypass are documented in
[CI/security operations](.github/CI_SECURITY.md#repository-controls-and-recovery).


## Reproduce the unsigned payload

Use the reference toolchain above, the exact immutable source commit, and its
recorded timestamp. `python3 scripts/check-reproducible.py --commit <commit>
--directory <new-empty-directory>` exports that commit twice, uses separate fresh
Maven repositories, builds the twelve binary/source/Javadoc JARs and installs the
five POMs locally, then compares all seventeen files directly by SHA-256. It never
signs or uploads. The initial experiment is recorded in the batch 04 validation
record. Compare the same source revision, never a different historical release.
OS/architecture, locale, archive permissions and the JDK distribution/version are
part of the recorded reference environment; cross-platform byte identity is not
claimed without a separate comparison. Signatures contain signing-time data and
are verified separately from the deterministic unsigned payload. Never rebuild or
replace the retained 1.4.1 release to retrofit reproducibility.

The publisher's isolated dependencies override Jackson core/databind 2.22.3 and
annotations 2.22, HttpClient 5.6.4 and HttpCore/httpcore5-h2 5.4.4. The upstream
0.11.0 dependency versions matched current OSV advisories; these six reviewed
replacement coordinates did not on 2026-09-26. Local signed bundle validation
exercises the overridden plugin. This is not an audit of every plugin dependency
or a claim that the live Central HTTP path has been tested. Namespace access and
a validated-then-dropped rehearsal remain tracked by #111. `autoPublish=false`
stays mandatory. The optional WAR is excluded and its install/deploy goals skip.


The signing plugin also pins `bcpg-jdk18on`, `bcprov-jdk18on`, and
`bcutil-jdk18on` to 1.86; both release plugins pin Plexus Utils to the compatible
3.6.2 line. The full resolved signing/publishing closure was checked after
these additions, separately from application/runtime dependencies. Validation
covers both the default GnuPG signer and the optional Bouncy Castle signer with
a disposable local key; the project release key and preferred GnuPG path remain
unchanged. See the batch 04 validation record for dated results.

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

1. Use a clean checkout and JDK 17. Record the full Git commit, JDK distribution
   and version, and Maven version. Build with a fresh Maven local repository so
   locally installed artifacts cannot hide dependency or version errors.
2. Apply reviewed security fixes privately until publication is ready.
3. Set the version in the root POM, each library module's parent POM, and
   `encoder.version` in `jakarta-test/pom.xml`. Set the root SCM tag to `v<VERSION>`.
4. Update README dependency examples, the security policy's supported versions,
   and the release notes. Include security advisories, compatibility changes,
   all Maven coordinates, signing fingerprint, and verification commands.
5. Run `mvn -B -ntp -Dmaven.repo.local=<fresh-cache> clean verify`.
   This checks unit tests and the packaged OSGi/JPMS consumers. With a running
   Docker-compatible runtime, also run `mvn -B -ntp
   -Dmaven.repo.local=<fresh-cache> verify -PtestJakarta`.
6. Commit the release files before tagging. Verify the four binary JARs, their
   source and Javadoc JARs, and five POMs. The optional `jakarta-test` WAR is not a
   published component.

## Sign and stage

For the Maven deploy path, configure a `central` server in a private Maven
settings file using a Central Portal token. Select the full project-key
fingerprint and dedicated GnuPG home explicitly:

```sh
mvn -B -ntp -s /private/path/settings.xml \
  -Dmaven.repo.local=/private/path/release-cache \
  -DperformRelease=true \
  -Dgpg.homedir=/private/path/project-gnupg \
  -Dgpg.keyname=1C5F632B86809F2F5DB25092BEA0075F94074A9B \
  clean deploy
```

Use the GnuPG agent to unlock the project key. The current POM uses
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
retrying. Published Maven coordinates are immutable. If release tags are
protected, an incorrect tag requires a maintainer to resolve the protection and
correction explicitly; never silently move an existing release tag.

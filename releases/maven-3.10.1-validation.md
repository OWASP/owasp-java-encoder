# Maven 3.10.1 wrapper validation — 2026-11-16

This record supports the toolchain decision in the
[dependency decision record](../.github/DEPENDENCY_DECISIONS.md) and replaces
Dependabot PR [#267](https://github.com/OWASP/owasp-java-encoder/pull/267).
The local checks below ran on macOS 27.2.1 (26B131) arm64. Windows wrapper
bootstrap and the other runtime jobs are checked by required PR CI and are not
claimed here.

## Distribution

`apache-maven-3.10.1-bin.zip` from `downloads.apache.org` and from Maven Central
are byte-identical. Its SHA-512 matches the `.sha512` file Apache publishes, and its
SHA-256 `9c1ef9174ef250a5009ed0dec97cbe8699acf7479add4e62bf7b0ea791c0adb5`
matches `distributionSha256Sum`. The detached signature is a good signature from
key `88BE34F94BDB2B5357044E2E3A387D43964143E3` (Tamas Cservenak, made
2026-11-11), the same key that signed 3.10.0, checked in an isolated keyring that
held only Apache Maven's `KEYS`. `KEYS` and the ZIP come from the same publisher,
so the signature check does not independently establish publisher identity. The
wrapper scripts stay Apache Maven Wrapper 3.3.4, and `mvnw.cmd` is unchanged and
LF in the index.

## Release scope

The [release notes](https://maven.apache.org/docs/3.10.1/release-notes.html)
describe 3.10.1 as a bugfix release for 3.10.0: regression fixes, Maven 4
backports and lighter startup (JLine is replaced by plain ANSI color codes). It
moves to Resolver 2.0.25 and Sisu 1.1.2 and lists no security fixes. The
potentially breaking changes listed on that page are the 3.10.0 changes already
reviewed for the 3.10.0 wrapper; none is new in 3.10.1.

One listed known issue applies to installs: Maven logs a warning with a stack
trace ("Invalid cross-device link") when the build directory and local
repository are on different volumes. The builds below did not hit it, and
`-Daether.installer.hardLink=false` avoids it.

## Build and wrapper

With 3.10.1, `./mvnw -B -ntp clean verify` passed with Homebrew OpenJDK
17.0.20.1 and a warm local repository: 2,302 unit tests, seven integration
tests, Checkstyle, API and Java 8 signature checks, coverage and both packaged
JSP engines. `python3 scripts/check-wrapper.py` bootstrapped 3.10.1 from an
empty wrapper cache and rejected an incorrect checksum. All 36 policy tests
passed. The legacy `jvnet-nexus-releases` `.meta/prefixes.txt` warning recorded
for 3.10.0 is unchanged.

## Effective POMs

`help:effective-pom` from 3.10.0 and 3.10.1 was compared for the same source
tree, both by default and with `-DperformRelease=true`. Apart from the
generator comment, both outputs are identical.

## Payloads

`main` at `c935f1cc9594cd1d3baee2fd2337f46bd01995b2` (Maven 3.10.0) and the
implementation commit `944527e` (Maven 3.10.1) were each
built from a fresh `git archive` export with its own empty Maven repository and
wrapper cache, using `-DskipTests clean install` and the UTC/locale settings of
`scripts/check-reproducible.py`, on Homebrew OpenJDK 17.0.20.1. All nine JARs
and three module POMs are byte-identical. The parent POM differs only in the
release enforcer's `[3.10.0]` → `[3.10.1]` line.

## Not exercised

`scripts/check-reproducible.py` did not run: it requires reference Eclipse
Temurin 17.0.20.1+1, which is not installed here. The payload comparison above
used a different JDK build, so it shows that the Maven version does not change
artifact bytes, not that the release recipe reproduces. Run that script with the
reference JDK before the next release. No signing, Central Portal staging, tag
or release operation ran.

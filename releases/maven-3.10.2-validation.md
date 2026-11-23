# Maven 3.10.2 wrapper validation — 2026-11-23

This record supports the toolchain decision in the
[dependency decision record](../.github/DEPENDENCY_DECISIONS.md) and replaces
Dependabot PR [#273](https://github.com/OWASP/owasp-java-encoder/pull/273).
The local checks below ran on macOS 27.2.1 (26B131) arm64 with Homebrew OpenJDK
17.0.21.0.3. Windows wrapper bootstrap and the other runtime jobs are checked by
required PR CI and are not claimed here.

## Distribution

`apache-maven-3.10.2-bin.zip` from `downloads.apache.org` and from Maven Central
are byte-identical. Its SHA-512 matches the `.sha512` file Apache publishes, and its
SHA-256 `7d69999d218e025cf1c14691ba748a09706e47999bd505009403cef9d757c2df`
matches `distributionSha256Sum`. The detached signature is a good signature from
key `88BE34F94BDB2B5357044E2E3A387D43964143E3` (Tamas Cservenak, made
2026-11-18), the same key that signed 3.10.1, checked in an isolated keyring
that held only Apache Maven's `KEYS`. `KEYS` and the ZIP come from the same
publisher, so the signature check does not independently establish publisher
identity. The wrapper scripts stay Apache Maven Wrapper 3.3.4, and `mvnw.cmd` is
unchanged and LF in the index.

## Release scope

The [release notes](https://maven.apache.org/docs/3.10.2/release-notes.html)
describe 3.10.2 as a bugfix release for 3.10.1 and list no known issues. Its
Resolver 2.0.26 retries HTTP 502 and 504 responses and improves compatibility
with Resolver 1.9.x. The potentially breaking changes on that page are the
3.10.0 changes already reviewed for the 3.10.0 wrapper; none is new in 3.10.2.

The [GitHub release](https://github.com/apache/maven/releases/tag/maven-3.10.2)
lists seven fixes and none is described as a security fix. They cover imported
dependency management and version-range caches, plugin-level
`<inherited>false</inherited>` with executions, build extensions that override
core lifecycle mappings, error reporting for invalid POMs and `extensions.xml`,
and ANSI colors on Windows 11. No POM here sets `<inherited>`, and the only build
extension is the Central publisher in the signing profile.

Comparing the `lib` and `boot` directories of the 3.10.1 and 3.10.2 ZIPs, the
only changes are Maven's own modules, Resolver 2.0.25 to 2.0.26 and Plexus Utils
3.6.2 to 3.6.4. Sisu stays at 1.1.2.

## Build and wrapper

With 3.10.2, `./mvnw -B -ntp clean verify` passed from an empty local
repository: 2,302 unit tests, seven integration tests, Checkstyle, API and
Java 8 signature checks, coverage and both packaged JSP engines.
`python3 scripts/check-wrapper.py` bootstrapped 3.10.2 from an empty wrapper
cache and rejected an incorrect checksum. All 36 policy tests passed, and
`python3 scripts/check-effective-pom-scm.py` passed. The legacy
`jvnet-nexus-releases` `.meta/prefixes.txt` warning recorded for 3.10.0 is
unchanged.

## Effective POMs

`help:effective-pom` from 3.10.1 and 3.10.2 was compared for the same source
tree, both by default and with `-DperformRelease=true`. Both pairs of outputs
are byte-identical.

## Payloads

`main` at `67f3d061f114bb0d7cfefd6ec50cf33baba1917d` (Maven 3.10.1) and the
implementation commit `99f8b31` (Maven 3.10.2) were each built from a fresh
`git archive` export with its own empty Maven repository and wrapper cache,
using `-DskipTests clean install` and the UTC/locale settings of
`scripts/check-reproducible.py`. All nine JARs and three module POMs are
byte-identical. The parent POM differs only in the release enforcer's
`[3.10.1]` → `[3.10.2]` line.

## Not exercised

`scripts/check-reproducible.py` did not run: it requires reference Eclipse
Temurin 17.0.20.1+1, which is not installed here. The payload comparison above
used a different JDK build, so it shows that the Maven version does not change
artifact bytes, not that the release recipe reproduces. Run that script with the
reference JDK before the next release. No signing, Central Portal staging, tag
or release operation ran, so the publisher extension has not been loaded under
3.10.2. The Docker browser fixture (`-PtestJakarta`) also did not run locally;
required PR CI covers it.

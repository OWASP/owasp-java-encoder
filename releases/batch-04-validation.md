# Batch 04 validation

Validated 2026-09-26. #123 merged separately in PR #184 after all 26 checks and
Sol then Astra reviews. This record covers the subsequent coordinated build
policy/release changes; it is not release approval.

## Reference payload comparison

Commit `d110c3594e3c78e46d2b652adadb356df64bf2cb` (PR #187 release-plugin
follow-up), exported twice with `git archive`; each copy used a
new empty local Maven repository and wrapper cache. Eclipse Temurin
17.0.20.1+1 (macOS aarch64 archive SHA-256
`196d13ba5f10414bef7f6a05a9b3f00edacb18ebacef2b99485db9e2ee18f0e8`),
Maven 3.9.16, macOS 27.0 (26A428), UTC, C locale and explicit UTF-8/en-US JVM
properties. `scripts/check-reproducible.py` compared the installed payloads,
not an older published release. Both clean installs succeeded and all seventeen
files matched. Later documentation/consumer-helper updates do not change these
artifact inputs. Source timestamp: `2026-09-26T00:00:00Z` from the reviewed POM.
No cross-OS/architecture claim and no comparison of signing timestamps is made.

| Artifact | SHA-256 (both builds) |
| --- | --- |
| `encoder-1.5.0-SNAPSHOT-javadoc.jar` | `2c8344ee19f9345c39915ca8c3ca654da60770f43787b8df25e2381b4d131d98` |
| `encoder-1.5.0-SNAPSHOT-sources.jar` | `32e0a28730aa55f2c546cf1122eef8a2971e99be67ac2872475be243649f299b` |
| `encoder-1.5.0-SNAPSHOT.jar` | `2f94c721dec1cd52d76617e51161e6174a498aa882cd409f59d8bc8bee86ee0a` |
| `encoder-1.5.0-SNAPSHOT.pom` | `8a663236fc5ad4d11c7c4f18c02c0a20bb2db3fae63a00bc7bc2e9d6fc858075` |
| `encoder-esapi-1.5.0-SNAPSHOT-javadoc.jar` | `6faacb28bd20720b9673462c434a5af465554f5aeefadda36231068f4b5e638c` |
| `encoder-esapi-1.5.0-SNAPSHOT-sources.jar` | `64e81e13cf000c11d82be10ab611cec53429bd0b0e5632e7480ea89176616c30` |
| `encoder-esapi-1.5.0-SNAPSHOT.jar` | `93434c28eeb31a28aa675164e8c22444b2336dfeb4fa94cc006b2dffd5830211` |
| `encoder-esapi-1.5.0-SNAPSHOT.pom` | `311fdafe83331ae937e6826d363115553c5a9d1e453d41c9ce270661741f890f` |
| `encoder-jakarta-jsp-1.5.0-SNAPSHOT-javadoc.jar` | `1defe3a269062ec12e9103138d3e731a1832767a39e8ee13fc125a55d52d6eef` |
| `encoder-jakarta-jsp-1.5.0-SNAPSHOT-sources.jar` | `1bfd112d8f2ed9e2b9dc2912043ad2caac1daa1c806861c6d6ecc6fe30457129` |
| `encoder-jakarta-jsp-1.5.0-SNAPSHOT.jar` | `9ceb6b4f4ad46a5d2233cc78f37f775acc1fb6115dbf039421bbe8359f318927` |
| `encoder-jakarta-jsp-1.5.0-SNAPSHOT.pom` | `379b3964ec5924175bfe5aa43448a05212348a5cb8cb0875f271cbbd8c65ac08` |
| `encoder-jsp-1.5.0-SNAPSHOT-javadoc.jar` | `e2a8025ae8895af5c71da8a273f2d290a5cd9abbc84dfde99ed4ab14e01c0796` |
| `encoder-jsp-1.5.0-SNAPSHOT-sources.jar` | `8ce08bbcd7c067038556cd525d42d09781875a60f89709f74c4be385302d70a5` |
| `encoder-jsp-1.5.0-SNAPSHOT.jar` | `f95dbbbc2bbe00acbefb2848e7c3c137396271f68358d1d0640c87399aa2dc80` |
| `encoder-jsp-1.5.0-SNAPSHOT.pom` | `6343c9cc2d5a5582b3c98444b139eecb792c416af8f96eb3d3efb88f146678a5` |
| `encoder-parent-1.5.0-SNAPSHOT.pom` | `a4981d5e9a0c617b6341a309299ad7f547f7611c42bb50646dd98a572c556dd9` |

## Build and policy evidence

- Full clean verify on the reference JDK: 2,178 unit tests, eight existing
  integration tests, both packaged JSP engines, API/signature checks and coverage.
- Seventeen binary/source metadata guards and 34-file adapter parity passed;
  Java 17 isolated classpath/explicit/automatic-module and Felix R6/R8 consumers
  passed, including negative old-core wiring.
- Normal plus optional profile Enforcer/Checkstyle validation passed. Maven 4
  prerelease plugins are intentionally not selected. Explicit pins cover clean,
  resources, compiler, JAR, source, Javadoc, install, deploy, versions, helper,
  analysis, API checks and the disabled implicit Site lifecycle.
- Coverage baseline/floors and scope are in BUILDING.md. A fresh reduced-test run
  failed at 76.0% core lines/61.5% branches; a separate clean `jacoco.skip=true`
  verify passed. Unit-test agents do not enter packaged consumers/JSP engines.
- Unix wrapper bootstrap and incorrect-SHA rejection passed locally; Windows
  and Unix CI bootstrap/checksum jobs passed at the initial PR head.
- Source notices preserve historical owners. Four module descriptors and four
  app source files lacked notices; their introduction commits identify Jeremy
  Long, 2024. No encoder-loop implementation changes were made.
- No old OSS repositories/profile inheritance remains. The new plugin graph
  parser was exercised against dependency-plugin 3.11.0 output.

## Local signing rehearsal

The original PR #185 sources were exported to an isolated directory and changed
only there to the non-published fixture version `9.9.9-validation` and matching
SCM tag text. A disposable, one-day **local validation only** RSA key, separate
GnuPG home, dummy Central settings entry and new Maven repository were used.
`clean deploy -DperformRelease=true -DskipPublishing=true -DskipTests` succeeded;
no production keys/tokens were used, no upload occurred, and no Git tag was made.
The plugin's skip flag filters all artifacts rather than generating the documented
ZIP; source inspection and empty staging output confirmed this. The safe local
assembler now handles this path explicitly, after verifying all 17 signatures.
It assembled 5 POMs and 12 JARs, their 17 signatures and 68 checksums (102 entries),
with no WAR. Validation-only bundle SHA-256:
`3aaf5f8986afdb91eb791fcedcd2c3ee7f4c9d38855838450c51541c31d375c1`.
The first attempt exposed Central 0.11.0's requirement for a server settings entry
even when upload is disabled; the documented command now accounts for it.

GPG 3.2.8/Publisher 0.11.0 and plugin-only Jackson/HTTP overrides were exercised.
OSV exact-version checks on the six overrides returned no advisories on the review
date; the upstream old Jackson/HTTP versions did have matches. This is narrower
than certifying the whole build graph or live Portal transport. #111 still owns
namespace access and validated-then-dropped real staging rehearsals.

Negative bundle probes rejected both an unexpected fingerprint and a modified
signed JAR before creating an output ZIP. Full-fingerprint GPG and raw Maven
SHA-256 sidecar instructions were independently exercised by Sol against the
original Central 1.4.0 artifact. The current project key block is unchanged.
Four early-key authorization-record gaps remain explicitly documented in #110.

All 28 PR checks passed at `8b548244233330872ba2c6d165f7624b5e6cddff`, including
both wrapper platforms, ten ESAPI versions, Docker/browser/WAR, Java 8 unit JVM,
original-JAR runtimes 8/11/17/21/25 and CodeQL. Final follow-up CI and model review
are required before merge; AI review is not independent maintainer approval.

Sol's final review found optimization-sensitive `assert` checks in the new release
assembler/reproducibility checker. They were replaced with explicit failures,
including wrapper probes. Four added regression tests run Python with `-O`:
changed/missing comparison payloads, a real valid signature from the wrong key,
a stale signed POM and a snapshot bundle are rejected. All 14 policy/verification
tests pass. A missing-key Maven signing attempt fails noninteractively, and the
release profile rejects the development SNAPSHOT version as intended.

## Release-plugin dependency follow-up (#187)

A broader query of the 29 distinct resolved GPG/Central plugin coordinates found
advisories in Bouncy Castle 1.81 and Plexus Utils 3.5.1/3.6.0. Plugin-only overrides
now align `bcpg-jdk18on`, `bcprov-jdk18on`, and `bcutil-jdk18on` at 1.86 and both
Plexus Utils uses at compatible 3.6.2. Application/runtime dependencies are unchanged.

The reference payload comparison above was repeated after this parent POM change:
all 17 files matched between the two new clean builds. Compared with the earlier
PR #185 evidence, only the parent POM hash changed; all 12 JARs and four module
POMs retained their hashes.

A fresh export of `d110c3594e3c78e46d2b652adadb356df64bf2cb`, changed only to the
local `9.9.9-validation` fixture version/tag text, passed both the default GnuPG
signer (`clean deploy`, publishing disabled) and optional Bouncy Castle signer
(`verify`). Both used a new one-day disposable test key, isolated Maven repository
and dummy Central settings. Each path verified all 17 signatures and assembled
its own 102-entry local bundle. Bundle SHA-256 values:

- GnuPG: `dd27bac64d2226b9b5dd644507167e5520808c101710dceb10b73e366433da2c`.
- Bouncy Castle: `778fd62f2d5c2f2b6287872455351ea1ea91b075ee857cd6ad850cc0cf79c229`.

Signatures were checked independently, not compared for reproducibility. The
disposable private key was deleted afterward. No production signing credentials,
upload or Git tag was used. Live Central transport remains outside this rehearsal.

After signing, dependency-plugin 3.11.0 resolved the actual release profile.
An exact-version [OSV query](https://osv.dev/) on 2026-09-26 returned no advisory
matches for these **28 distinct coordinates**, including the two plugin roots.
This covers the resolved GPG/Central plugin closures, not every project build
plugin, operating system package, or live publishing service.

| Resolved release-plugin coordinate | Version |
| --- | --- |
| `com.fasterxml.jackson.core:jackson-annotations` | `2.22` |
| `com.fasterxml.jackson.core:jackson-core` | `2.22.3` |
| `com.fasterxml.jackson.core:jackson-databind` | `2.22.3` |
| `com.github.package-url:packageurl-java` | `1.4.1` |
| `com.google.code.findbugs:jsr305` | `3.0.2` |
| `com.google.errorprone:error_prone_annotations` | `2.18.0` |
| `com.google.guava:failureaccess` | `1.0.1` |
| `com.google.guava:guava` | `32.1.0-jre` |
| `com.google.guava:listenablefuture` | `9999.0-empty-to-avoid-conflict-with-guava` |
| `com.google.j2objc:j2objc-annotations` | `2.8` |
| `com.kohlschutter.junixsocket:junixsocket-common` | `2.10.1` |
| `com.kohlschutter.junixsocket:junixsocket-core` | `2.10.1` |
| `com.kohlschutter.junixsocket:junixsocket-native-common` | `2.10.1` |
| `commons-io:commons-io` | `2.15.1` |
| `org.apache.commons:commons-lang3` | `3.18.0` |
| `org.apache.httpcomponents.client5:httpclient5` | `5.6.4` |
| `org.apache.httpcomponents.core5:httpcore5-h2` | `5.4.4` |
| `org.apache.httpcomponents.core5:httpcore5` | `5.4.4` |
| `org.apache.maven.plugins:maven-gpg-plugin` | `3.2.8` |
| `org.apache.maven.resolver:maven-resolver-api` | `1.9.22` |
| `org.apache.maven.resolver:maven-resolver-util` | `1.9.22` |
| `org.bouncycastle:bcpg-jdk18on` | `1.86` |
| `org.bouncycastle:bcprov-jdk18on` | `1.86` |
| `org.bouncycastle:bcutil-jdk18on` | `1.86` |
| `org.checkerframework:checker-qual` | `3.33.0` |
| `org.codehaus.plexus:plexus-utils` | `3.6.2` |
| `org.slf4j:slf4j-api` | `1.7.36` |
| `org.sonatype.central:central-publishing-maven-plugin` | `0.11.0` |

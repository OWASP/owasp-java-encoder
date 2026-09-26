# Batch 04 validation

Validated 2026-09-26. #123 merged separately in PR #184 after all 26 checks and
Sol then Astra reviews. This record covers the subsequent coordinated build
policy/release changes; it is not release approval.

## Reference payload comparison

Commit `8b548244233330872ba2c6d165f7624b5e6cddff`, exported twice with `git archive`; each copy used a
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
| `encoder-parent-1.5.0-SNAPSHOT.pom` | `8c15bf168cf491d2cebabd683c3550f6b8dbba8fdaa2647ecea7d1526be1209b` |

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

The same committed sources were exported to an isolated directory and changed
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

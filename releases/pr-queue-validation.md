# PR queue cleanup validation — 2026-09-26

## Reviewed changes and preserved contracts

PR #175 updates verified artifact-action pins together, retains named same-run ZIP
transfer and makes download digest failures explicit. Local actionlint and all
14 policy/verification tests passed. Sol and then Astra found no actionable issues
on `9fafc772b8e7f55d44417a8b1f59b2ea69de2568`; all 28 checks passed before merge
as `011734abc029726a115811b53228cdaa789e7232`. Post-merge workflow evidence is in #169.

The focused Maven change replaces Bundle Plugin 6.1.2 with 6.2.0, which uses bnd
7.4.0. The [dependency decision record](../.github/DEPENDENCY_DECISIONS.md) accounts
for every proposal in #176/#188 and its reconsideration conditions. Eleven exact
coordinates are excluded only from the broad version-update group. Parsed YAML
comparison confirms all directories, schedules, existing ignores and the separate
security-update group are unchanged. Exclusion from a group is not an update ignore.
Sol corrected the attribution of the already delivered versions-maven-plugin
2.22.0 upgrade; only its former reporting execution was retired.

## Local build and packaged consumers

A new isolated Maven repository/wrapper cache with reference Temurin 17.0.20.1+1
and Maven 3.9.16 passed `./mvnw -B -ntp clean verify`: 2,178 unit tests, eight
integration tests, API/Java 8 signature checks, coverage and both forked packaged
JSP engines. All 17 artifact guards, 14 policy/verification tests and 34-file
JSP/Jakarta parity passed. Original-JAR consumers passed locally on Java 17 across
classpath, explicit/automatic JPMS and Felix R6/R8, including old-core rejection.
Docker/browser and other runtime JVMs are checked by required PR CI, not claimed
as locally executed. Final Sol/Astra review and exact-head CI evidence go in #169.

Resolved the actual Bundle Plugin dependency closure before and after the change
with dependency-plugin 3.11.0 `resolve-plugins`: both contain 61 distinct coordinates.
The only replacements are the plugin 6.1.2 → 6.2.0, bndlib 7.3.0 → 7.4.0 and
bnd.util 7.3.0 → 7.4.0. An OSV query on 2026-09-26 returned no matches for those
three newly selected coordinates. This is a delta audit, not a claim that the
58 unchanged plugin dependencies or the full build/runtime graphs are advisory-free.
Existing alerts are not dismissed by this check, nor by test/build scope.

## Reproducibility and artifact differences

Two fresh `git archive` exports of implementation commit
`d9d157d917c4b21208edcd52bdab5515e86a0a52` used separate empty Maven repositories
and wrapper caches, reference Eclipse Temurin 17.0.20.1+1, Maven 3.9.16, macOS
27.0 aarch64, UTC/C locale and explicit UTF-8/en-US JVM properties. The official
Temurin archive matched its published SHA-256
`196d13ba5f10414bef7f6a05a9b3f00edacb18ebacef2b99485db9e2ee18f0e8`.
Both clean builds produced identical copies of all **17 payloads**. Subsequent
workflow/documentation-only commits do not change artifact inputs. No cross-OS
comparison, production signing, Portal upload or release/tag operation is claimed.

Compared with the retained batch 05 reference payloads, all eight source/Javadoc
JARs and four module POMs remain byte-identical. The parent POM changes only the
Bundle Plugin version. Each of the four binary JARs has the same entry set and
identical entry contents except `META-INF/MANIFEST.MF`, whose sole content change
is `Created-By: Apache Maven Bundle Plugin 6.1.2` → `6.2.0`. No class/resource is
added or removed; public identities, API ranges, bytecode and metadata are unchanged.
Both copies and compact comparison/dependency evidence are retained locally.

| Payload | SHA-256 (both builds) |
| --- | --- |
| `encoder-1.5.0-SNAPSHOT-javadoc.jar` | `a756dd361b2d6296fb2cb94f7f0c20c0a470d6dfaab7518832ca83b26641e7ca` |
| `encoder-1.5.0-SNAPSHOT-sources.jar` | `0cd1292c1e03b9554be4f5c0d92ad4702f3a53da20e1c52b5f4423726370541a` |
| `encoder-1.5.0-SNAPSHOT.jar` | `99749a02ade17e470dae672f8b4b0853f4dd6658b35dce5c9d07b9236e3c4cfb` |
| `encoder-1.5.0-SNAPSHOT.pom` | `2c7a39dfb2b04ae75b2b8bda5695a2f3dee373ad0d7f36a55f1010aabe463afe` |
| `encoder-esapi-1.5.0-SNAPSHOT-javadoc.jar` | `cd16a149a0c8aed8d1e244e88f7cde1f6ae482e212dc2377afdb05f63549874b` |
| `encoder-esapi-1.5.0-SNAPSHOT-sources.jar` | `de93cb5d0d4233263f9ed175406fb93215611bc108507a4d31f2b7e3bb21e8c4` |
| `encoder-esapi-1.5.0-SNAPSHOT.jar` | `e16e812e6efe844bb8f09630162714b86fda8e647fc5c7331eac431c7e951288` |
| `encoder-esapi-1.5.0-SNAPSHOT.pom` | `8876d4eac1ad4f23117e0e27a9d184fbdbf20ab35e57bb8a88c2451a750c0e03` |
| `encoder-jakarta-jsp-1.5.0-SNAPSHOT-javadoc.jar` | `494b1e428320798d2678eaf1e0d9f42efc45926b70b0065d8198d74ff7ea53c8` |
| `encoder-jakarta-jsp-1.5.0-SNAPSHOT-sources.jar` | `b8743c89d737a415fc571f8e31cb8d6a11c1a113841bd4bea8ba771eed8bb533` |
| `encoder-jakarta-jsp-1.5.0-SNAPSHOT.jar` | `e010c046b84d27b8852a89fc989199e4989063ae430c10f08d4c643a58e52234` |
| `encoder-jakarta-jsp-1.5.0-SNAPSHOT.pom` | `df4eece564afbb1aeecda9b05d0f8b190a1bc7d1e92f1bf4829a6c4df78847f4` |
| `encoder-jsp-1.5.0-SNAPSHOT-javadoc.jar` | `5dcbfc0e53938e69dae6345e43ba310f7d0e3bc2e67b4c105c85a9601aac2fb2` |
| `encoder-jsp-1.5.0-SNAPSHOT-sources.jar` | `81b201dd6965a8add5726198665d65cf6f258da8e3986a670c4f976ca63f8e7f` |
| `encoder-jsp-1.5.0-SNAPSHOT.jar` | `58cac1f9cd50ca012e5d161edbce8cebd4da392da04d8530269fbb051145545d` |
| `encoder-jsp-1.5.0-SNAPSHOT.pom` | `c2e21aa5107fc215a6cae907776b4c2ee889376b226ae948f1ad841c4910d5ca` |
| `encoder-parent-1.5.0-SNAPSHOT.pom` | `7fbb00ce7f35849eb1476c060ac70207b65f0a69d603252cdadc44aa91bc201d` |

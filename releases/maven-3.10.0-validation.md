# Maven 3.10.0 wrapper validation — 2026-10-11

This record supports the toolchain decision in the
[dependency decision record](../.github/DEPENDENCY_DECISIONS.md) and replaces
Dependabot PR [#235](https://github.com/OWASP/owasp-java-encoder/pull/235).
The local checks below ran on macOS 27.0.1 (26A434) arm64. Windows wrapper
bootstrap and the other runtime jobs are checked by required PR CI and are not
claimed here.

## Distribution

`apache-maven-3.10.0-bin.zip` from `downloads.apache.org` and from Maven Central
are byte-identical. Its SHA-512 matches the `.sha512` file Apache publishes, and its
SHA-256 `1f6d9909266510f039f59aa0e13dcd2c66da85f043e56276e41f2918f8bddaff`
matches `distributionSha256Sum`. The detached signature is a good signature from
key `88BE34F94BDB2B5357044E2E3A387D43964143E3` (Tamas Cservenak, made
2026-09-27), checked in an isolated keyring that held only Apache Maven's `KEYS`.
`KEYS` and the ZIP come from the same publisher, so the signature check does not
independently establish publisher identity. The wrapper scripts stay Apache Maven
Wrapper 3.3.4, and `mvnw.cmd` is unchanged and LF in the index.

## Build and wrapper

With 3.10.0, `./mvnw -B -ntp clean verify` passed with Homebrew OpenJDK 17.0.20.1
and a warm local repository: 2,280 unit tests, seven integration tests,
Checkstyle, API and Java 8 signature checks, coverage and both packaged JSP
engines. `python3 scripts/check-wrapper.py` bootstrapped 3.10.0 from an empty
wrapper cache and rejected an incorrect checksum. All 35 policy tests passed.

Resolver 2 fetches `.meta/prefixes.txt` from every remote repository it consults.
The legacy `jvnet-nexus-releases` repository declared by `net.java:jvnet-parent:5`
fails that request on TLS certificate validation. That produces one warning per
build and no failed resolution, because Central serves every artifact.

## Effective POMs

`help:effective-pom` from 3.9.16 and 3.10.0 was compared per module and per
plugin, both by default and with `-DperformRelease=true`. The only differences are
the Super POM's plugin-management entries for the unused assembly and release
plugins and, in release mode, a redundant `<inherited>true</inherited>` on the
source, Javadoc and deploy plugins. That second difference comes from the Super
POM `release-profile` that 3.10.0 removes. Plugin versions, executions, goals,
phases, configuration, properties, dependencies and repositories are unchanged.

## Reproducibility

`scripts/check-reproducible.py` ran with reference Eclipse Temurin 17.0.20.1+1,
whose archive matched its published SHA-256
`196d13ba5f10414bef7f6a05a9b3f00edacb18ebacef2b99485db9e2ee18f0e8`. Each run
used two fresh `git archive` exports with separate empty Maven repositories and
wrapper caches.

- Implementation commit `07b92e07b2444f6c4b6a08f3af44c720a61defd1` (Maven
  3.10.0) produced identical copies of all 13 payloads.
- Compared with a build of `main` at
  `ee1e2a88893b43c5b494dcc86cb83be4d0d68660` (Maven 3.9.16), all nine JARs and
  three module POMs are byte-identical. The parent POM differs only in the
  release enforcer's `[3.9.16]` → `[3.10.0]` line.

| Payload | SHA-256 (Maven 3.10.0, both builds) |
| --- | --- |
| `encoder-1.5.1-SNAPSHOT-javadoc.jar` | `1dea3061f4ed78c79672472889a84793c4fb1c48bde1a5766389314aa0a9c176` |
| `encoder-1.5.1-SNAPSHOT-sources.jar` | `6eba5c35b89c9d4a8c457f9e774e29560084b2223b1b6799f29eea166bf17f32` |
| `encoder-1.5.1-SNAPSHOT.jar` | `a10cd22386265d8e631ac7793e8a5eb9645a95ae050ec76d0c916d43b9b6291b` |
| `encoder-1.5.1-SNAPSHOT.pom` | `19791032b4a05bdfc855bf4c476437cda8251784b6aa1ebaba77e783bbaf0f66` |
| `encoder-jakarta-jsp-1.5.1-SNAPSHOT-javadoc.jar` | `498f81f2f618452d9b5e180223a17b8128a26c469ad66223e249a568860d02d1` |
| `encoder-jakarta-jsp-1.5.1-SNAPSHOT-sources.jar` | `69ff116828ee2db0223b4d509a9486b653a3984d78787af4ebac8bd3cc4a1b77` |
| `encoder-jakarta-jsp-1.5.1-SNAPSHOT.jar` | `463131114b7d34e2f4627918ecba0d18b53ac9b274945062d3fe04c2ab589836` |
| `encoder-jakarta-jsp-1.5.1-SNAPSHOT.pom` | `ddbfb208fabcc84ceb9cef0ce0e62ea55987eee57d73f6c0de9c192ec154538d` |
| `encoder-jsp-1.5.1-SNAPSHOT-javadoc.jar` | `77b56a2071429e122967647f4512098010bab6c3043b6b30ccf58b36192a1718` |
| `encoder-jsp-1.5.1-SNAPSHOT-sources.jar` | `94193083573ef1752c12284f37e5cf59a5901ce5d7cd80032dc915afe7deccb7` |
| `encoder-jsp-1.5.1-SNAPSHOT.jar` | `fb8d852f656f408697245a9d0aae19035057dbdcf5742d25cf3994c990549c2c` |
| `encoder-jsp-1.5.1-SNAPSHOT.pom` | `0b35d49c56a8c8a6746c09146ccd79a6be2ed10f6b5cc8da2e024f77396f3575` |
| `encoder-parent-1.5.1-SNAPSHOT.pom` | `061a61e632b989732a7ba2db1cc9383e75deaedcf182ff42b15ab349e2052060` |

Later documentation-only commits on this change do not alter artifact inputs.

## Not exercised

No signing, Central Portal staging, tag or release operation ran. Maven 3.10.0
scopes repository credentials to their origins. The first signed `clean deploy`
under 3.10.0 should confirm that Central staging still authenticates with the
`central` server entry.

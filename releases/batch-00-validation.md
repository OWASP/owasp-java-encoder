# Batch 00 validation — 2026-09-25

Work tracked by [#169](https://github.com/OWASP/owasp-java-encoder/issues/169),
[#111](https://github.com/OWASP/owasp-java-encoder/issues/111), and
[#112](https://github.com/OWASP/owasp-java-encoder/issues/112).
Dates use America/Los_Angeles; the Central HTTP check was at
2026-09-26 04:48:36 UTC. The starting main commit was
`bd249f57790b2f85b124e351f2d8684322b0a525`.

## Release availability and integrity

The [Central core metadata](https://repo.maven.apache.org/maven2/org/owasp/encoder/encoder/maven-metadata.xml)
listed 1.4.0 as its release. Each exact 1.4.1 POM returned HTTP 404:

| Artifact | Central POM |
| --- | --- |
| `encoder` | [1.4.1 POM](https://repo.maven.apache.org/maven2/org/owasp/encoder/encoder/1.4.1/encoder-1.4.1.pom) |
| `encoder-jsp` | [1.4.1 POM](https://repo.maven.apache.org/maven2/org/owasp/encoder/encoder-jsp/1.4.1/encoder-jsp-1.4.1.pom) |
| `encoder-jakarta-jsp` | [1.4.1 POM](https://repo.maven.apache.org/maven2/org/owasp/encoder/encoder-jakarta-jsp/1.4.1/encoder-jakarta-jsp-1.4.1.pom) |
| `encoder-esapi` | [1.4.1 POM](https://repo.maven.apache.org/maven2/org/owasp/encoder/encoder-esapi/1.4.1/encoder-esapi-1.4.1.pom) |
| `encoder-parent` | [1.4.1 POM](https://repo.maven.apache.org/maven2/org/owasp/encoder/encoder-parent/1.4.1/encoder-parent-1.4.1.pom) |

Downloaded the existing [signed GitHub release assets](https://github.com/OWASP/owasp-java-encoder/releases/tag/v1.4.1)
without modifying them. GnuPG 2.5.20, using a new public-only keyring, verified:

- The release `KEYS` exactly matches the checkout's `KEYS`.
- All 19 detached signatures (17 JARs/POMs and two checksum manifests) succeed
  with primary fingerprint `1C5F632B86809F2F5DB25092BEA0075F94074A9B`.
- Every entry in both signed SHA-256 and SHA-512 manifests matches its asset.
- The Central bundle contains all four libraries' binary, source, and Javadoc
  JARs and all five POMs. Every artifact and signature matches the corresponding
  standalone GitHub asset byte for byte; all bundled MD5, SHA-1, SHA-256, and
  SHA-512 checksums match.

Retained bundle: `owasp-java-encoder-1.4.1-central-bundle.zip`.
SHA-256: `c70234d2290fff0011d484219b7bc8fae2581cf24b24b03abf5eab4413b6d4c3`.
This is verification of the retained release, not evidence of Central delivery.

## ESAPI dependency examples

The [Central 1.4.0 adapter POM](https://repo.maven.apache.org/maven2/org/owasp/encoder/encoder-esapi/1.4.0/encoder-esapi-1.4.0.pom)
declares `[2.5.1.0,3)`. The verified signed 1.4.1 POM and main's unreleased
`1.5.0-SNAPSHOT` POM use `esapi.version=2.7.0.0`.
Upstream's [latest release](https://github.com/ESAPI/esapi-java-legacy/releases/latest)
and [security policy](https://github.com/ESAPI/esapi-java-legacy/security) both
identified 2.7.0.0 as current and supported at this check. Older versions in the
adapter CI matrix do not gain upstream security support by passing adapter tests.

Validated the XML examples taken directly from [esapi/README.md](../esapi/README.md)
using Maven 3.9.12 and Homebrew OpenJDK 17.0.20.1. Each consumer used its own
initially empty local Maven repository and empty user/global settings, outside
the checkout. No reactor build or existing local install supplied artifacts.

| Consumer | Resolved adapter | Resolved core | Resolved ESAPI | Result |
| --- | --- | --- | --- | --- |
| Central 1.4.0 with the documented `dependencyManagement` pin | 1.4.0 | 1.4.0 | 2.7.0.0 (managed from `[2.5.1.0,3)`) | PASS |
| Signed 1.4.1 with no ESAPI override | 1.4.1 | 1.4.1 | 2.7.0.0 | PASS |

For the second consumer, first ran the [documented local installation](1.4.1.md#install-verified-artifacts-while-central-is-pending)
with `maven-install-plugin:3.1.4`: the retained parent POM and all four libraries,
including their original POMs, sources, and Javadocs. All five installations
succeeded. No artifact was rebuilt or re-signed.

To repeat the resolution checks, place each README example in a minimal consumer
POM, use separate fresh repositories, and run both goals below. For the 1.4.0
case, set the adapter dependency to 1.4.0 and include the documented management
block. For 1.4.1, install the verified files into that consumer's repository first.
Use an empty Maven settings file for both `-s` and `-gs`:

```sh
mvn -B -ntp -s /path/to/empty-settings.xml -gs /path/to/empty-settings.xml \
    -Dmaven.repo.local=/path/to/consumer-cache \
    org.apache.maven.plugins:maven-dependency-plugin:3.8.1:tree -Dverbose
mvn -B -ntp -s /path/to/empty-settings.xml -gs /path/to/empty-settings.xml \
    -Dmaven.repo.local=/path/to/consumer-cache \
    org.apache.maven.plugins:maven-dependency-plugin:3.8.1:resolve
```

Both goals succeeded for both consumers. These checks establish dependency
resolution and local installation, not application security or full upstream
dependency support. The 1.4.0 pin still resolves the affected encoder 1.4.0;
consumers must also upgrade Java Encoder.

## Operational checks and remaining work

The current Central Portal session identified Jim Manico, showed **No Namespace(s)
Found**, and disabled **Publish Component**. No deployment or new Support request
was created. Continue the existing Support request to restore access. Jim
confirmed initial key setup on this date; independent vault recovery and
publisher rehearsals remain unconfirmed as recorded in [MAINTAINERS.md](../MAINTAINERS.md#batch-00-follow-up-2026-09-25-americalos_angeles).

The [OWASP project page](https://owasp.org/www-project-java-encoder/) still tells
consumers to use 1.3.0 in its Getting Started text and needs a follow-up update.
[javadoc.io](https://javadoc.io/doc/org.owasp.encoder/encoder) identifies 1.4.0 as
current. Neither is evidence of 1.4.1 Central publication; recheck destinations
after delivery, using the signed GitHub release for current consumer guidance.

Keep #111 open until each publisher's access and distinct validated-then-dropped
rehearsal, each custodian's vault drill, exact Central publication, and the
post-publication comparisons and notice updates are evidenced. The full
[1.5 backlog gate](../RELEASING.md#release-gate-for-15) remains in force.

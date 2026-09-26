# ESAPI adapter dependency policy

`encoder-esapi` uses ESAPI 2.7.0.0 as its fixed default dependency. This is the
latest stable ESAPI release; unlike the previous Maven range, it cannot silently
select a release candidate when a new artifact is published.

The supported compatibility range is the stable ESAPI releases from 2.5.1.0
through 2.7.0.0, inclusive. CI builds and runs the adapter tests against every
stable release in that range:

- 2.5.1.0, 2.5.2.0, 2.5.3.0, and 2.5.3.1
- 2.5.4.0 and 2.5.5.0
- 2.6.0.0, 2.6.1.0, and 2.6.2.0
- 2.7.0.0

This is an adapter compatibility statement, not an upstream security-support
statement. The [ESAPI security policy][esapi-security] supports only 2.7.0.0
and recommends upgrading from earlier versions. Production applications should
use the default unless a tested dependency constraint prevents it.

Maintainers can deliberately test another version without changing the POM:

```shell
mvn -pl esapi -am clean verify -Desapi.version=2.5.1.0
```

Applications can select another tested version with normal Maven dependency
management. The adapter's published POM still defaults deterministically to
2.7.0.0.

## Runtime and security notes

`ESAPIEncoder.getInstance()` and its OWASP Java Encoder-backed methods do not
load ESAPI configuration. Delegated operations resolve ESAPI's reference encoder
on each call, so a missing `ESAPI.properties` causes an ESAPI configuration
exception for that operation and can be retried after configuration becomes
available in the same JVM. ESAPI retains responsibility for caching its reference
encoder. Obtaining the adapter through `ESAPI.encoder()` still requires ESAPI
configuration to select the implementation.

The ESAPI dependency remains a compile dependency because its `Encoder` type is
part of the adapter's public API. Its transitive dependencies are therefore also
available to applications. The ESAPI module enforces dependency convergence for
this graph, but applications should continue to scan their complete resolved
graph because their other dependencies may change Maven conflict resolution.

The [ESAPI 2.7.0.0 release][esapi-release] addresses CVE-2025-5878 and updates
transitive dependencies for CVE-2025-48976 and CVE-2025-48734. Its upstream POM
intentionally depends on the milestone `commons-collections4` 4.5.0-M2; this
adapter does not override ESAPI's tested graph.

A review of the default graph on 2026-09-11 also found later advisories in
ESAPI's legacy dependencies and the HTTP Components versions provided through
AntiSamy:

- Commons Configuration 1.10: [GHSA-pvp8-3xj6-8c6x][]; no patched 1.x release
- Commons Lang 2.6: [GHSA-j288-q9x7-2f5v][]; no patched 2.x release
- HttpClient 5.4.4: [GHSA-hjcp-jmpx-g3qm][]; patched in 5.6.3
- HttpCore and HttpCore H2 5.3.4: [GHSA-hf6x-8p5f-cgmf][] and
  [GHSA-v3jc-474w-2wm6][]; patched in 5.4.3

The adapter does not force untested transitive upgrades. Applications that use
the affected ESAPI or AntiSamy features should assess those advisories and
manage patched versions where compatible.

ESAPI 2.7 disables `encodeForSQL` by default. The adapter preserves that safer
behavior; use parameterized queries instead of enabling the legacy method.

Every supported ESAPI JAR derives the automatic JPMS module name `esapi` from
its filename. Keep the original `esapi-<version>.jar` filename when placing it
on the module path. This stable identity is the one used by the adapter's JPMS
dependency declaration.

[esapi-security]: https://github.com/ESAPI/esapi-java-legacy/security
[esapi-release]: https://github.com/ESAPI/esapi-java-legacy/releases/tag/esapi-2.7.0.0
[GHSA-pvp8-3xj6-8c6x]: https://github.com/advisories/GHSA-pvp8-3xj6-8c6x
[GHSA-j288-q9x7-2f5v]: https://github.com/advisories/GHSA-j288-q9x7-2f5v
[GHSA-hjcp-jmpx-g3qm]: https://github.com/advisories/GHSA-hjcp-jmpx-g3qm
[GHSA-hf6x-8p5f-cgmf]: https://github.com/advisories/GHSA-hf6x-8p5f-cgmf
[GHSA-v3jc-474w-2wm6]: https://github.com/advisories/GHSA-v3jc-474w-2wm6

# ESAPI adapter dependency policy

The ESAPI dependency depends on the `encoder-esapi` release you consume:

| Adapter version | ESAPI dependency in its POM | Availability |
| --- | --- | --- |
| `1.4.0` | Maven range `[2.5.1.0,3)`; resolution can change and can select a release candidate | Maven Central; affected by Java Encoder's 1.4.1 security advisories |
| `1.4.1` | Fixed default `2.7.0.0` | [Signed GitHub security release][encoder-release]; Central publication is pending |
| `1.5.0-SNAPSHOT` | Fixed default `2.7.0.0` | Unreleased development; not a published release |

The fixed dependency was introduced in 1.4.1. It does not change the POM already
published for 1.4.0. As checked on 2026-09-25, upstream's [latest stable release][esapi-latest]
and [security policy][esapi-security] identify ESAPI 2.7.0.0 as current and supported.
Recheck those sources when choosing a version; adapter compatibility does not
establish upstream security support.

## Upgrade to 1.4.1

Upgrade **all OWASP Java Encoder dependencies to 1.4.1**, including the core
`encoder` if your application declares or manages it separately. While Central
publication is pending, obtain the [signed 1.4.1 artifacts][encoder-release],
follow the [verification and local installation instructions][encoder-verification],
and install the retained POMs and JARs in your local or organizational Maven
repository. Version 1.4.1 will not resolve from Central alone.

```xml
<dependency>
    <groupId>org.owasp.encoder</groupId>
    <artifactId>encoder-esapi</artifactId>
    <version>1.4.1</version>
</dependency>
```

## Temporary ESAPI pin for 1.4.0 consumers

If you temporarily remain on `encoder-esapi:1.4.0`, add this to your application's
POM (or merge it into its existing `dependencyManagement`) to replace the ESAPI
range with a deterministic version:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.owasp.esapi</groupId>
            <artifactId>esapi</artifactId>
            <version>2.7.0.0</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Pinning ESAPI does not fix Java Encoder's security issues.** Version 1.4.0
remains affected by [the three advisories fixed in 1.4.1][encoder-advisories].
This pin only controls the ESAPI dependency; upgrade Java Encoder as well.
Check the application's resolved graph with `mvn dependency:tree`, particularly
if another dependency or BOM also manages ESAPI or the core encoder.

## Tested adapter compatibility

The supported compatibility range is the stable ESAPI releases from 2.5.1.0
through 2.7.0.0, inclusive. CI builds and runs the adapter tests against every
stable release in that range:

- 2.5.1.0, 2.5.2.0, 2.5.3.0, and 2.5.3.1
- 2.5.4.0 and 2.5.5.0
- 2.6.0.0, 2.6.1.0, and 2.6.2.0
- 2.7.0.0

This is an adapter compatibility statement, not an upstream security-support
statement. At the check date above, the [ESAPI security policy][esapi-security]
supports only 2.7.0.0 and recommends upgrading from earlier versions. Passing
adapter tests on an older ESAPI version does not make it supported upstream.

Maintainers can deliberately test another version without changing the POM:

```shell
mvn -pl esapi -am clean verify -Desapi.version=2.5.1.0
```

Applications can select another tested version with normal Maven dependency
management. The signed 1.4.1 POM and current development POM default
deterministically to 2.7.0.0; the Central 1.4.0 POM still uses the range above.

## URL encoding migration in 1.5 (unreleased)

Starting with `1.5.0-SNAPSHOT`, `ESAPIEncoder.getInstance().encodeForURL(value)`
uses `Encode.forUriComponent` to encode **one raw URL component** with UTF-8.
Earlier adapter releases, including signed 1.4.1, use deprecated `Encode.forUri`
and leave delimiters such as `& = / ? # +` unchanged. For example:

| Raw input | Adapter through 1.4.1 | Adapter 1.5 | ESAPI reference with UTF-8 |
| --- | --- | --- | --- |
| `a b+c&admin=true` | `a%20b+c&admin=true` | `a%20b%2Bc%26admin%3Dtrue` | `a+b%2Bc%26admin%3Dtrue` |
| `~*` | `~*` | `~%2A` | `%7E*` |
| `%20` | `%2520` | `%2520` | `%2520` |
| Java `null` | String `"null"` | String `"null"` | Java `null` |
| Unpaired UTF-16 surrogate | `-` | `-` | `%3F` (replacement `?`) |

The adapter deliberately uses RFC 3986 component encoding with `%20` for spaces,
preserving its existing space, null, and malformed-Unicode policies. ESAPI's
[reference implementation][esapi-url-reference] uses form encoding with `+` for
spaces and the configured `Encryptor.CharacterEncoding`. Both escape URL
delimiters, but their output is not interchangeable for byte comparisons or
request signatures. Use `java.net.URLEncoder.encode(value, "UTF-8")` if a
protocol requires form encoding. The adapter still declares `EncodingException`
and does not read ESAPI configuration for this operation.

Encode each raw parameter name/value or path segment once, then assemble the
URL using trusted delimiters. Validate the final URL against application rules,
including allowed schemes and any destination/path restrictions. Component
encoding does not prevent every application-specific path issue (for example,
`.` and `..` are unreserved). URI parsing alone is not a safety check. For a
quoted HTML URL attribute, encode the assembled, validated URL with
`Encode.forHtmlAttribute`. See the [shared migration guidance](../README.md#migrating-from-foruri).

Callers that passed complete URLs must change that call pattern before adopting
1.5: component encoding escapes the scheme colon, slashes, and other structural
delimiters. Already percent-encoded input is encoded again. Existing core,
registry, JSP, and Jakarta `forUri` entry points retain their behavior throughout
1.x; only this adapter delegate changes. This is unreleased 1.5 behavior, not a
change to the retained 1.4.1 artifacts.

## Supported output contexts

The adapter intentionally preserves these contexts rather than matching every
escape emitted by ESAPI's reference implementation:

| Method | Supported context and caller responsibility |
| --- | --- |
| `encodeForHTMLAttribute` | A quoted HTML text attribute. Supply single or double quotes. HTML escaping alone does not make event-handler code or an unvalidated URL safe. |
| `encodeForCSS` | A quoted CSS string using `Encode.forCssString`; not arbitrary unquoted CSS values or expressions. |
| `encodeForJavaScript` | A single/double-quoted string or literal text in an ordinary untagged template, using `Encode.forJavaScript`. Not JSON, tagged templates (including `String.raw`), `${...}` expression bodies, arbitrary unquoted code, or script URLs. |
| `encodeForURL` (1.5) | One raw URL component; assemble and validate the URL, then encode for its enclosing context. |

More escaping does not make arbitrary unquoted JavaScript or CSS safe. The CSS
size fix, JavaScript template-boundary and Unicode handling, JSON delegation,
lazy reference lookup, and ESAPI's default disablement of unsafe SQL encoding
remain intact. Parser and contract tests run against the stable ESAPI matrix
listed above; that matrix remains separate from upstream security support.

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

### Dependency security triage

The resolved dependency submissions and [Dependabot alerts][dependency-alerts]
are the ongoing inventory, including transitive runtime/test dependencies and
build plugins. Review the actual version, path and execution scope of each new
alert; the fact that ESAPI introduces a dependency is not a dismissal reason.
On 2026-09-26 UTC, Maven Central still had 2.7.0.0 as the newest stable ESAPI
release (2.7.0.1-RC1 was a prerelease). The default graph includes these findings:

- Commons Configuration 1.10: [GHSA-pvp8-3xj6-8c6x][]; no patched 1.x release
- Commons Lang 2.6: [GHSA-j288-q9x7-2f5v][]; no patched 2.x release
- HttpClient 5.4.4: [GHSA-hjcp-jmpx-g3qm][]; patched in 5.6.3
- HttpCore and HttpCore H2 5.3.4: [GHSA-hf6x-8p5f-cgmf][] and
  [GHSA-v3jc-474w-2wm6][]; patched in 5.4.3

The Commons Configuration finding concerns resource use while loading untrusted
configuration; delegated ESAPI calls initialize reference configuration, so keep
that configuration trusted. Commons Lang's finding concerns attacker-controlled
class names passed to `ClassUtils.getClass`. HttpClient's finding concerns classic
HTTP response decoding and connection release; HttpCore's findings concern HTTP/1
header parsing and HTTP/2 HPACK decoding. The adapter's Java Encoder-backed
methods perform string encoding without these HTTP or configuration operations.
Delegated methods and applications using other ESAPI/AntiSamy features have a
different scope, so this is not a blanket application reachability conclusion.

Disposition: retain these findings for upstream/application assessment; no
blanket suppression or untested POM override is applied. Prefer a stable upstream
ESAPI release with a tested fixed graph. If it is unavailable, a mitigation or
override may be accepted after review of the affected feature's reachability,
API/runtime compatibility, dependency convergence, and the complete adapter and
packaged-consumer matrix. Commons Configuration 2.x and Commons Lang 3.x use
different APIs/namespaces and cannot silently replace the legacy coordinates.
Record the advisory, affected versions, scope, evidence, owner and recheck date
for any exception; time-limit suppressions and reopen them when assumptions
change. Recheck this disposition on the next ESAPI release or within 90 days.
Do not close an alert simply because it is transitive or adapter tests pass.

ESAPI 2.7 disables `encodeForSQL` by default. The adapter preserves that safer
behavior; use parameterized queries instead of enabling the legacy method.

Every supported ESAPI JAR derives the automatic JPMS module name `esapi` from
its filename. Keep the original `esapi-<version>.jar` filename when placing it
on the module path. This stable identity is the one used by the adapter's JPMS
dependency declaration.

[esapi-security]: https://github.com/ESAPI/esapi-java-legacy/security
[dependency-alerts]: https://github.com/OWASP/owasp-java-encoder/security/dependabot
[esapi-latest]: https://github.com/ESAPI/esapi-java-legacy/releases/latest
[esapi-release]: https://github.com/ESAPI/esapi-java-legacy/releases/tag/esapi-2.7.0.0
[esapi-url-reference]: https://github.com/ESAPI/esapi-java-legacy/blob/esapi-2.7.0.0/src/main/java/org/owasp/esapi/reference/DefaultEncoder.java#L506-L516
[encoder-release]: https://github.com/OWASP/owasp-java-encoder/releases/tag/v1.4.1
[encoder-verification]: ../releases/1.4.1.md#verification
[encoder-advisories]: ../releases/1.4.1.md#security-fixes
[GHSA-pvp8-3xj6-8c6x]: https://github.com/advisories/GHSA-pvp8-3xj6-8c6x
[GHSA-j288-q9x7-2f5v]: https://github.com/advisories/GHSA-j288-q9x7-2f5v
[GHSA-hjcp-jmpx-g3qm]: https://github.com/advisories/GHSA-hjcp-jmpx-g3qm
[GHSA-hf6x-8p5f-cgmf]: https://github.com/advisories/GHSA-hf6x-8p5f-cgmf
[GHSA-v3jc-474w-2wm6]: https://github.com/advisories/GHSA-v3jc-474w-2wm6

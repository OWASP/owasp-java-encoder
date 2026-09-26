# OWASP Java Encoder

![Build status](https://github.com/OWASP/owasp-java-encoder/actions/workflows/build.yaml/badge.svg?branch=main)
[![BSD 3-Clause](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](LICENSE)

Contextual output encoding for Java 8+. Choose an encoder for the parser context
receiving untrusted text: HTML, JavaScript, CSS, XML or a URL component. The core
has no runtime dependencies; optional JSP, Jakarta and ESAPI adapters have their
own dependency graphs. Encoding is one part of [XSS prevention][xss], alongside
safe templates, URL validation and other application controls.

**Upgrade all Java Encoder artifacts to 1.4.1. Versions through 1.4.0 are affected
by the [security issues fixed in 1.4.1](releases/1.4.1.md#security-fixes).**
Maven Central publication is still pending (checked 2026-09-26); the signed
[GitHub 1.4.1 release][release] is available. Download, [verify](VERIFYING.md) and
[install its retained artifacts](releases/1.4.1.md#verification) in your local or
organizational Maven repository. Central alone cannot resolve 1.4.1. Do not use
Central's affected 1.4.0 just because it is the latest version shown there.

`main` is **unreleased 1.5.0-SNAPSHOT**. Its JSON API, JavaScript template support,
XML 1.1 tag bindings and ESAPI URL change are described below with version labels;
they are not features of the signed 1.4.1 release. See [CHANGELOG.md](CHANGELOG.md).

## Start using the OWASP Java Encoders

After installing the verified 1.4.1 artifacts, select the dependency you need.
All four use group ID `org.owasp.encoder` and version `1.4.1`:

| Artifact ID | Purpose and runtime dependencies |
| --- | --- |
| `encoder` | Core String/Writer API; no runtime dependencies |
| `encoder-jsp` | Legacy `javax` JSP tags/EL functions; core plus container-provided JSP API |
| `encoder-jakarta-jsp` | Jakarta JSP tags/EL functions; core plus container-provided Jakarta JSP API |
| `encoder-esapi` | ESAPI `Encoder` adapter; core and ESAPI 2.7.0.0 with its transitive dependencies |

```xml
<dependency>
    <groupId>org.owasp.encoder</groupId>
    <artifactId>encoder</artifactId>
    <version>1.4.1</version>
</dependency>
```

Replace `encoder` with one adapter artifact ID when needed; each adapter brings
in core. Keep separately managed core/adapter versions aligned. Use **one** of the
javax or Jakarta taglib JARs: they share `org.owasp.encoder.tag` and must not coexist
on the same classpath or module path. See [ESAPI dependency and migration
policy](esapi/README.md), [runtime matrix](compatibility/README.md), and
[dependency/license inventory](docs/dependencies.md). Development snapshots are
not security releases or a substitute for the signed 1.4.1 artifacts.

```java
import org.owasp.encoder.Encode;

out.write("<p>");
Encode.forHtmlContent(out, userText); // Writer overload; no intermediate String
out.write("</p>");
```

The equivalent String call is `Encode.forHtmlContent(userText)`. Callers supply
trusted surrounding syntax and attribute quotes. Encode raw data once, at the
output boundary; account for any escaping your template engine already performs.

## Choose the output context

| Destination | API and limits |
| --- | --- |
| HTML text, including textarea content | `forHtmlContent`; `forHtml` also covers quoted ordinary text attributes |
| Quoted HTML text attribute | `forHtmlAttribute`; not an event-handler expression or URL validator |
| One raw URL component | `forUriComponent`; assemble with trusted delimiters, validate the URL, then encode for the enclosing HTML attribute |
| JavaScript string | `forJavaScript`; supply single/double quotes. Ordinary untagged template literal text requires **1.5**. Never use in tagged templates, expression bodies, JSON or script URLs |
| JSON string content | `forJson` (**1.5**); supply double quotes. Prefer a serializer for a complete document |
| Quoted CSS string / CSS `url(...)` value | `forCssString` / `forCssUrl`; validate URLs and obey the method's surrounding-context rules |
| XML 1.0 text / quoted attribute | `forXmlContent` / `forXmlAttribute`; `forXml` covers both |
| XML 1.1 text / quoted attribute | `forXml11Content` / `forXml11Attribute` (core **1.4+**); requires an XML 1.1 document/parser, not HTML |
| XML CDATA / comment | `forCDATA` / `forXmlComment`; not HTML comments |
| Java source string literal | `forJava`; caller supplies quotes; unpaired surrogates may not compile |

Every listed facade method has String and Writer overloads. The
[context guide](docs/contexts.md) explains nesting, null/Unicode behavior,
JavaScript variants, template boundaries, JSON and unsafe contexts. The
[Java/JSP examples](docs/usage.md) show complete surrounding syntax.

Encoding does not sanitize HTML, validate input/URLs, serialize JSON documents,
perform SQL parameterization, or decode/canonicalize data. Use an HTML sanitizer
when markup must be allowed; use parameterized queries for SQL. See the
[OWASP Java security-library guide][java-libraries] for these distinct roles.

## Taglib

Taglib URIs are **identifiers**, not URLs that must open in a browser:

| Adapter | Basic identifier | Advanced identifier |
| --- | --- | --- |
| Jakarta | `owasp.encoder.jakarta` | `owasp.encoder.jakarta.advanced` |
| javax JSP | `https://www.owasp.org/index.php/OWASP_Java_Encoder_Project` | Same identifier with `#advanced` appended |

```jsp
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" %>
<%@ taglib prefix="e" uri="owasp.encoder.jakarta" %>
<p>${e:forHtmlContent(param.message)}</p>
```

Use the javax identifier for a javax container. Tags use an empty body and a
required `value` attribute, for example `<e:forHtmlContent value="${param.message}" />`.
See [bindings and EL evaluation](docs/usage.md#tag-bindings-and-el-evaluation) for
basic versus advanced methods and version-sensitive deployment settings. In 1.5,
advanced taglibs expose every `Encode.forX(String)` context except `forJava`;
Java source generation is not a JSP context. XML 1.1 bindings are new in 1.5.

## Migrating from forUri

`Encode.forUri` is deprecated in the released API. **Unreleased 1.5** extends
that deprecation to `Encoders.URI`, both `ForUriTag` classes and the `forUri`
tag/function documentation. All of these entry points are retained through 1.x. Encoding a whole URI does not validate it:
`forUri("javascript:alert(1)")` returns it unchanged. Existing `%` signs are encoded
again. Use `forUriComponent` for one raw parameter name/value, path segment or
fragment; validate complete URLs separately, then use `forHtmlAttribute` when
placing one in a quoted HTML attribute. Parsing with `java.net.URI` alone does
not establish safety. See the [worked URL example](docs/usage.md#urls).

The ESAPI adapter's `encodeForURL` changes in **unreleased 1.5** to component
encoding, escaping delimiters and literal `+`, with `%20` spaces. Earlier adapter
releases preserve whole-URI delimiters. Its existing null and malformed-Unicode
policies remain. Read the [adapter migration guide](esapi/README.md#url-encoding-migration-in-15-unreleased)
before upgrading. Removing the legacy API needs a separately reviewed future-major
decision; deprecation is not a removal schedule.

## Java 9+ module names and OSGi

Use the [published module names and OSGi ranges](compatibility/README.md#published-identities-and-development-import-ranges)
and [runnable JPMS example](docs/usage.md#java-modules). Explicit multi-release
module names intentionally differ from historical automatic names. Do not rename
dependency JARs used for automatic modules. CI exercises original packaged JARs
on Java 8/11/17/21/25, classpath, JPMS and Felix R6/R8; this does not certify every
container or downstream dependency combination.

## Building and contributing

Build with JDK 17 and the committed wrapper (Maven 3.9.16); use `mvnw.cmd` on Windows:

```sh
./mvnw clean verify
./mvnw clean verify -PtestJakarta  # optional locally; requires Docker
```

Normal verify includes the Docker-free JSP engines. The browser/WAR suite remains
required in CI. See [BUILDING.md](BUILDING.md) for style/coverage policy,
[CONTRIBUTING.md](CONTRIBUTING.md) for review and diagnostic commands, and
[RELEASING.md](RELEASING.md) for the distinct release gates. There is no benchmark
or Maven Site publishing profile.

The 1.x line preserves Java 8 library APIs/bytecode, published API/module identities
and dependency scopes. Exact encoded output is also observable behavior: adding
escapes is not automatically patch-compatible. Changes need context/parser tests,
an output-change note and migration guidance when required. Security fixes can
correct unsafe behavior in a patch with explicit advisories; other compatibility
breaks require a future-major decision, not just a version label.

## Project, security and support

Leaders: [Jim Manico](https://github.com/jmanico) and
[Jeremy Long](https://github.com/jeremylong); original author: Jeff Ichnowski.
See [MAINTAINERS.md](MAINTAINERS.md) and the [OWASP project page][project].
The separate OWASP website can lag this repository's release status.

Report suspected vulnerabilities **privately** through [SECURITY.md](SECURITY.md).
Verify downloads with [KEYS](KEYS) and [VERIFYING.md](VERIFYING.md).
The project uses the [BSD 3-Clause license](LICENSE) and the
[OWASP Code of Conduct](CODE_OF_CONDUCT.md). [Funding](CONTRIBUTING.md#funding)
links distinguish maintainer support from OWASP Foundation donations.

[xss]: https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html
[java-libraries]: https://devguide.owasp.org/en/05-implementation/03-secure-libraries/04-java-secure-libs/
[project]: https://owasp.org/projects/java-encoder
[release]: https://github.com/OWASP/owasp-java-encoder/releases/tag/v1.4.1

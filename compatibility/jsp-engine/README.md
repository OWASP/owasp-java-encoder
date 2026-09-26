# Packaged taglibs through real JSP engines

Normal JDK 17 `./mvnw clean verify` runs this Docker-free fixture after each adapter
has been packaged. It deploys only the packaged core and adapter JARs into a
loopback-only temporary web application. Jasper discovers their actual TLDs,
compiles generated JSPs and serves HTTP responses. No install is necessary.

The engine dependencies belong to the AntRun plugin realm and its forked JVM,
not to the libraries' compile/test classpaths or published runtime dependencies.
Test classes and generated JSPs live under each adapter's `target/jsp-engine`,
not its JAR. `-DskipTests` skips this integration fixture as well as unit tests.

| Adapter | Test engine | Engine APIs | Engine JVM |
| --- | --- | --- | --- |
| `encoder-jsp` | Tomcat/Jasper 9.0.122 | Servlet 4.0 / JSP 2.3 / EL 3.0 (`javax`) | 17 |
| `encoder-jakarta-jsp` | Tomcat/Jasper 10.1.60 | Servlet 6.0 / Pages 3.1 / EL 5.0 (`jakarta`) | 17 |

These maintained engine generations accept the adapters' older provided APIs;
this does not raise the libraries' Java 8 or API baselines. The separate
[packaged consumer matrix](../README.md) still uses JSP 2.2.1 / Servlet 3.0.1 /
EL 2.2.5 and Pages 3.0 / Servlet 5 / EL 4, including actual Java 8 execution.
The engines here demonstrate compiler/container integration, not full container
certification or a running container at every historical API floor.

Versions and the [Tomcat support table](https://tomcat.apache.org/whichversion.html),
[9.x advisories](https://tomcat.apache.org/security-9.html) and
[10.x advisories](https://tomcat.apache.org/security-10.html) were reviewed on
2026-09-25. These versions include the September security fixes. They run only
as disposable test servers, on a random loopback port, without writable default
servlets, AJP, HTTP/2, TLS or application authentication configuration.
Dependabot reviews the plugin dependencies in the adapter POMs; recheck upstream
advisories with each update and before a release.

The current surface is 72 bindings per adapter: 576 exact-byte assertions and
72 rejected pages on each engine (1,152 byte assertions and 144 rejections total).
Counts are derived from the packaged descriptors rather than frozen in the test.

For both basic and advanced descriptors the fixture discovers every tag and
function and fails on empty, duplicate, missing or unexercised bindings. Each
binding renders hostile markup, controls and Unicode, a long buffer-boundary
value, empty input, missing/null EL values, numbers, booleans and scriptlet null.
The expected String/Writer facade output is compared to the complete HTTP UTF-8
response bytes. EL null-to-empty coercion is intentionally distinct from a
scriptlet passing Java null directly to the tag's String setter.

Every tag must also reject a nonempty body and an omitted required value at JSP
translation time. A generic HTTP 500 does not suffice: the response must identify
a Jasper exception and the expected constraint. Generated sources and engine
work directories remain under `target` for diagnosis; `./mvnw clean` removes them.

This fixture proves binding, compilation, coercion and emitted bytes. It does
not prove that an arbitrary use of those bytes is safe in a browser context.
The required Docker/browser suite in `jakarta-test` retains those distinct
JavaScript grammar, HTML parser and DOM assertions. The reflection/descriptor
contracts, source parity, OSGi and JPMS consumer checks also remain in place.

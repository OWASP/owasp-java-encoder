# Required browser and packaged WAR fixture

This optional application is a test fixture, not a dependency of an encoder
library. From the repository root, with JDK 17, Maven and Docker available:

```sh
mvn -B -ntp -Dmaven.repo.local=/tmp/encoder-browser-m2 clean verify -PtestJakarta
```

Use an empty task-specific Maven directory for fresh validation. The reactor
packages the matching encoder JARs without installing them. CI requires this
profile in the `Java CI gate`; unavailable Docker is a failure, not a skipped or
advisory test. CI also compares the JAR inside the WAR byte-for-byte with the
reactor's Jakarta adapter.

## Coverage decision (#93)

Retain the browser fixture. The Docker-free [Jasper tests](../compatibility/jsp-engine/README.md)
cover every packaged basic/advanced tag and EL binding, coercions, output bytes,
and invalid JSP translation. They cannot replace these browser assertions:

- `ItemControllerTest`: JSP/JSTL startup, tag and EL output interpreted as text in
  actual DOM cells, no injected script elements, standards-mode HTML.
- `JavaScriptTemplateTest`: all four JavaScript encoders through quoted strings
  and ordinary template literals, interpolation boundaries, HTML script and
  event-attribute parsing, controls and lone surrogates through UTF-8, and the
  explicitly unsupported raw-template round-trip behavior.
- `PackagedWarIT`: launches `java -jar` on the finished executable WAR on a
  random loopback port, renders both packaged views, checks exact encoded cell
  content, and confirms JSTL API/implementation and adapter JARs are packaged.
  It terminates the server even on failure. This test needs no Docker.

Browser sessions and containers are explicitly closed in `AfterAll` with
`finally` cleanup. Video recording is disabled, so no unused recorder image is
started. Surefire/Failsafe output and `target/packaged-war.log` are retained by CI.
A local Chrome-only diagnostic for the JavaScript suite remains available with
`-Dencoder.browser.local=true -Dtest=JavaScriptTemplateTest`; it is not the CI gate.

## Framework and API boundaries

As reviewed on 2026-09-25, this fixture uses supported Spring Boot **4.1.1** and
its managed dependencies, on JDK **17**, with Tomcat/Jasper **11.0.26**
(Servlet **6.1**, Pages **4.0**, EL **6.0**). The two deliberate BOM overrides are
Tomcat 11.0.26, which contains the September fixes absent from Boot's managed
11.0.24, and Selenium 4.49.0, aligned with the current reviewed browser image.
Testcontainers **2.0.5**, JSTL API **3.0.2** and implementation **3.0.1** follow the
Boot BOM. The standalone Servlet/Pages/EL API JARs are removed; Tomcat supplies
the coherent implementation/API set. Both JSTL components remain packaged.
The unused JSON starter, empty test configuration/launcher, and unused service
mutation scaffold are removed.

See the [Boot support policy](https://github.com/spring-projects/spring-boot/wiki/Supported-Versions),
[system requirements](https://docs.spring.io/spring-boot/system-requirements.html),
[Spring advisories](https://spring.io/security/), and
[Tomcat 11 advisories](https://tomcat.apache.org/security-11.html).
An OSV query of the 34 resolved third-party JAR coordinates in the packaged WAR
(including provided container libraries) returned no advisories on 2026-09-25.
That dated result excludes container OS packages, build plugins and test-only
JARs; it is not a permanent or repository-wide clean bill.
Recheck these sources and the resolved dependency graph with each upgrade and
before release. Do not copy these fixture requirements into library support claims.
The published adapters retain Java 8 and their existing provided APIs. The
separate javax/Jakarta engines and Java 8/11/17/21/25 packaged consumers still
exercise older contracts. OSGi's conservative Pages import range is unchanged;
this Boot application is not an OSGi container test.

## Container provenance and updates

`BrowserFixture.java` and test-only `testcontainers.properties` contain immutable
multi-platform index digests fetched from Docker Hub's registry and verified
against the SHA-256 of each manifest response on 2026-09-25:

| Use | Reviewed tag | Index SHA-256 |
| --- | --- | --- |
| Browser | `selenium/standalone-chrome:4.49.0-20260909` | `7efe71e7e4a83bdf574b26bd354690928075e8f443223d2ced16a2c208eae1d7` |
| Cleanup | `testcontainers/ryuk:0.14.0` | `7c1a8a9a47c780ed0f983770a662f80deb115d95cce3e2daa3d12115b8cd28f0` |
| Host-port forwarding | `testcontainers/sshd:1.3.0` | `c50c0f59554dcdb2d9e5e705112144428ae9d04ac0af6322b365a18e24213a6a` |
| Docker startup probe | `alpine:3.24.2` | `294b683cb724975bec92580e1e685676bd4b50bda910ddb8c51d4cabeaec77e6` |

The [Selenium release](https://github.com/SeleniumHQ/docker-selenium/releases/tag/4.49.0-20260909)
and [Testcontainers 2.0.5 source](https://github.com/testcontainers/testcontainers-java/tree/2.0.5)
control the browser/helper choices. The startup probe uses maintained Alpine
instead of the old default 3.17. Digest pins provide immutable identity, not a
claim that an image contains no vulnerabilities. Review publisher release notes,
image scan results and all helper versions when updating. Keep the Selenium
client and image aligned, update the tag and digest together, verify the manifest
hash/platforms again, and run the full required profile before merging. These
source/property pins require manual review; Maven Dependabot does not update them.

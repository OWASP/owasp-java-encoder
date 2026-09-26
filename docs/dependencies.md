# Dependencies and declared licenses

The core `encoder` has **no runtime dependencies**. `encoder-jsp` and
`encoder-jakarta-jsp` depend on core and declare their matching JSP API as
`provided`; the container supplies it. `encoder-esapi` has compile dependencies
on core and ESAPI 2.7.0.0, including ESAPI's transitive graph. None of these
third-party classes is shaded into the four Encoder JARs. The optional Boot/WAR
fixture, test engines and build plugins are development tooling, not published
library runtime dependencies. Review the [live dependency graph](https://github.com/OWASP/owasp-java-encoder/network/dependencies)
for those separate scopes and [ESAPI advisory triage](../esapi/README.md#dependency-security-triage).

## Consumer dependency inventory — 2026-09-26

Generated from dependency-plugin 3.11.0's resolved reactor `dependency:tree`
JSON for current `1.5.0-SNAPSHOT`. Includes compile/runtime and provided scopes,
excludes test/plugin dependencies and this project's own BSD-3-Clause modules.
The scope column identifies the consuming module. These are **upstream POM
license declarations**, following parent POM inheritance where necessary; the
source links identify the exact declaring POM. They are not a legal conclusion
about every file or application distribution. Preserve required notices and
review the artifacts you actually distribute. The project license is [LICENSE](../LICENSE).

```sh
./mvnw dependency:tree -DoutputType=json -DoutputFile=target/dependencies.json
```

Dependency management in an application can resolve a different graph. Recheck
the inventory after a version/scope change; a listed license says nothing about
security support or advisory status. This inventory does not rewrite published
1.4.1 POMs or claim that every third-party POM uses a normalized SPDX identifier.

| Coordinate | Consumer scope | Declared licenses | Provenance |
| --- | --- | --- | --- |
| `commons-beanutils:commons-beanutils:1.11.0` | esapi: compile | Apache-2.0 | [POM](https://repo.maven.apache.org/maven2/org/apache/apache/34/apache-34.pom) |
| `commons-collections:commons-collections:3.2.2` | esapi: compile | Apache License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/apache/apache/16/apache-16.pom) |
| `commons-configuration:commons-configuration:1.10` | esapi: compile | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/apache/apache/13/apache-13.pom) |
| `commons-fileupload:commons-fileupload:1.6.0` | esapi: compile | Apache-2.0 | [POM](https://repo.maven.apache.org/maven2/org/apache/apache/34/apache-34.pom) |
| `commons-io:commons-io:2.19.0` | esapi: compile | Apache-2.0 | [POM](https://repo.maven.apache.org/maven2/org/apache/apache/33/apache-33.pom) |
| `commons-lang:commons-lang:2.6` | esapi: compile | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/apache/apache/7/apache-7.pom) |
| `commons-logging:commons-logging:1.3.5` | esapi: compile | Apache-2.0 | [POM](https://repo.maven.apache.org/maven2/org/apache/apache/33/apache-33.pom) |
| `jakarta.servlet.jsp:jakarta.servlet.jsp-api:3.0.0` | jakarta: provided | Eclipse Public License v. 2.0; GNU General Public License, version 2 with the GNU Classpath Exception | [POM](https://repo.maven.apache.org/maven2/org/eclipse/ee4j/project/1.0.6/project-1.0.6.pom) |
| `javax.servlet.jsp:javax.servlet.jsp-api:2.2.1` | jsp: provided | CDDL + GPLv2 with classpath exception | [POM](https://repo.maven.apache.org/maven2/javax/servlet/jsp/javax.servlet.jsp-api/2.2.1/javax.servlet.jsp-api-2.2.1.pom) |
| `org.apache-extras.beanshell:bsh:2.0b6` | esapi: compile | Apache License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/apache-extras/beanshell/bsh/2.0b6/bsh-2.0b6.pom) |
| `org.apache.commons:commons-collections4:4.5.0-M2` | esapi: compile | Apache-2.0 | [POM](https://repo.maven.apache.org/maven2/org/apache/apache/32/apache-32.pom) |
| `org.apache.httpcomponents.client5:httpclient5:5.4.4` | esapi: compile | Apache License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/apache/apache/27/apache-27.pom) |
| `org.apache.httpcomponents.core5:httpcore5:5.3.4` | esapi: compile | Apache License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/apache/apache/27/apache-27.pom) |
| `org.apache.httpcomponents.core5:httpcore5-h2:5.3.4` | esapi: compile | Apache License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/apache/apache/27/apache-27.pom) |
| `org.apache.xmlgraphics:batik-constants:1.19` | esapi: compile | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik/1.19/batik-1.19.pom) |
| `org.apache.xmlgraphics:batik-css:1.19` | esapi: compile | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik/1.19/batik-1.19.pom) |
| `org.apache.xmlgraphics:batik-i18n:1.19` | esapi: compile | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik/1.19/batik-1.19.pom) |
| `org.apache.xmlgraphics:batik-shared-resources:1.19` | esapi: compile | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik/1.19/batik-1.19.pom) |
| `org.apache.xmlgraphics:batik-util:1.19` | esapi: compile | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik/1.19/batik-1.19.pom) |
| `org.apache.xmlgraphics:xmlgraphics-commons:2.11` | esapi: compile | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/xmlgraphics-commons/2.11/xmlgraphics-commons-2.11.pom) |
| `org.htmlunit:neko-htmlunit:4.11.0` | esapi: compile | Apache License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/htmlunit/neko-htmlunit/4.11.0/neko-htmlunit-4.11.0.pom) |
| `org.owasp.antisamy:antisamy:1.7.8` | esapi: compile | BSD 3 | [POM](https://repo.maven.apache.org/maven2/org/owasp/antisamy/antisamy/1.7.8/antisamy-1.7.8.pom) |
| `org.owasp.esapi:esapi:2.7.0.0` | esapi: compile | BSD; Creative Commons 3.0 BY-SA | [POM](https://repo.maven.apache.org/maven2/org/owasp/esapi/esapi/2.7.0.0/esapi-2.7.0.0.pom) |
| `org.slf4j:slf4j-api:2.0.16` | esapi: compile | MIT License | [POM](https://repo.maven.apache.org/maven2/org/slf4j/slf4j-bom/2.0.16/slf4j-bom-2.0.16.pom) |
| `xerces:xercesImpl:2.12.2` | esapi: compile | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/xerces/xercesImpl/2.12.2/xercesImpl-2.12.2.pom) |
| `xml-apis:xml-apis:1.4.01` | esapi: compile | The Apache Software License, Version 2.0; The SAX License; The W3C License | [POM](https://repo.maven.apache.org/maven2/xml-apis/xml-apis/1.4.01/xml-apis-1.4.01.pom) |
| `xml-apis:xml-apis-ext:1.3.04` | esapi: compile | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/apache/apache/3/apache-3.pom) |
| `xom:xom:1.3.9` | esapi: compile | The GNU Lesser General Public License, Version 2.1 | [POM](https://repo.maven.apache.org/maven2/xom/xom/1.3.9/xom-1.3.9.pom) |

The XML API declarations need component-level context. Their exact Central JARs
provide more detailed notices than the POM metadata alone:

- [`xml-apis:1.4.01`](https://repo.maven.apache.org/maven2/xml-apis/xml-apis/1.4.01/xml-apis-1.4.01.jar)
  contains `license/LICENSE` (Apache 2.0), `LICENSE.dom-software.txt` and
  `LICENSE.dom-documentation.txt` (W3C notices), `LICENSE.sax.txt` (the SAX
  public-domain statement), and `license/NOTICE`. The bundled `README.dom.txt`
  and `README.sax.txt` explain which files each notice covers.
- [`xml-apis-ext:1.3.04`](https://repo.maven.apache.org/maven2/xml-apis/xml-apis-ext/1.3.04/xml-apis-ext-1.3.04.jar)
  contains `license/LICENSE` (Apache 2.0), DOM software/documentation notices,
  `LICENSE.sac.html` (W3C notice), `license/NOTICE` and `README.dom.txt`.

These notices cover different components; the list is not a claim that a consumer
can choose any one license for the entire JAR. Several old declared license URLs
are obsolete, so the table links to the retained declaring POM rather than
substituting a new license or treating a dead URL as absence of a license.

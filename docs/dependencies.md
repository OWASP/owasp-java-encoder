# Dependencies and declared licenses

The core `encoder` artifact has **no runtime dependencies**. `encoder-jsp` and
`encoder-jakarta-jsp` depend on core and declare their matching JSP API as
`provided`; the container supplies it. No third-party classes are shaded into
the three Java Encoder 1.5.0 JARs.

The optional Boot/WAR fixture, parser and JSP-engine tests, build plugins, and
release tooling are development infrastructure. They are not dependencies of a
published Java Encoder library. Review the [live dependency graph][graph] for
their separate scopes.

`encoder-esapi` was retired after version 1.4.1 and is absent from the 1.5.0
reactor and dependency graph. Its historical artifact remains immutable but is
unsupported; see the [retirement and migration notice](encoder-esapi-retirement.md).

## Consumer dependency inventory — 2026-09-27

Generated from dependency-plugin 3.11.0's resolved reactor `dependency:tree`
JSON for current `1.5.0-SNAPSHOT`. This inventory includes the publishable
modules' compile/runtime and provided scopes, excludes test/plugin dependencies
and this project's own BSD-3-Clause modules, and identifies the consuming module.

```sh
./mvnw dependency:tree -DoutputType=json -DoutputFile=target/dependencies.json
```

| Coordinate | Consumer scope | Declared licenses | Provenance |
| --- | --- | --- | --- |
| `jakarta.servlet.jsp:jakarta.servlet.jsp-api:3.0.0` | jakarta: provided | Eclipse Public License v. 2.0; GNU General Public License, version 2 with the GNU Classpath Exception | [POM](https://repo.maven.apache.org/maven2/org/eclipse/ee4j/project/1.0.6/project-1.0.6.pom) |
| `javax.servlet.jsp:javax.servlet.jsp-api:2.3.3` | jsp: provided | CDDL + GPLv2 with Classpath Exception | [POM](https://repo.maven.apache.org/maven2/javax/servlet/jsp/javax.servlet.jsp-api/2.3.3/javax.servlet.jsp-api-2.3.3.pom) |

These are upstream POM license declarations, not a legal conclusion about every
file or application distribution. Preserve required notices and review the
artifacts actually distributed. The Java Encoder project license is
[BSD-3-Clause](../LICENSE). Dependency management in an application can resolve
a different graph; recheck it after any version or scope change. A listed license
says nothing about security support or advisory status.

[graph]: https://github.com/OWASP/owasp-java-encoder/network/dependencies

# Packaged consumer compatibility

The build JDK and library runtime baseline are separate. Releases use JDK 17 and
the 1.x library line targets Java 8 APIs and bytecode (`--release 8`), with Java 9
module descriptors in `META-INF/versions/9`. CI executes the same original JARs
on Temurin 8, 11, 17, 21, and 25. Java 8 has no JPMS, so that leg runs classpath
and OSGi consumers. Java 9+ legs additionally run explicit and automatic modules.

| Artifact | Library runtime baseline | Fixture APIs |
| --- | --- | --- |
| `encoder` | Java 8 | No runtime dependencies |
| `encoder-jsp` | Java 8 | JSP 2.2.1, Servlet 3.0.1, EL 2.2.5 (`javax`) |
| `encoder-jakarta-jsp` | Java 8 with compatible container APIs | JSP 3.0.0, Servlet 5.0.0, EL 4.0.0 (`jakarta`) |
| `encoder-esapi` | Java 8 with compatible ESAPI dependencies | ESAPI 2.7.0.0 and its runtime dependencies |

The Jakarta fixture deliberately uses Servlet 5.0, whose minimum Java SE version
is 8 ([specification](https://jakarta.ee/specifications/servlet/5.0/)). Newer servlet
containers/APIs can require a newer JVM. The reactor's Jakarta tests use Servlet 6,
and the Docker/Selenium application remains in the separate JDK 17 `Java CI` job.
This smoke matrix is not certification of every container, ESAPI operation, or
transitive dependency on every JDK. The separate ESAPI version matrix tests the
adapter's broader supported ESAPI range.

## What runs

`consumers.py prepare` copies the four JARs produced by `mvn clean verify`, resolves
the pinned fixture dependencies, and compiles consumers independently of reactor
classes or test classpaths. Core consumers assert String and Writer output,
including input that crosses internal buffer boundaries, plus JavaScript and URI
encoding. JSP consumers instantiate the actual tag, set a minimal `JspContext`,
call `doTag()`, and assert output. The ESAPI consumer obtains the real adapter and
asserts an HTML encoding result. Its minimal `config/ESAPI.properties` is supplied
explicitly; it is not a production configuration or a claim that ESAPI needs no
configuration. SLF4J may report that the fixture has no logging provider.

The runtime jobs download the prepared fixtures into fresh checkouts, verify the
original JAR SHA-256 values, assert the actual JVM specification version, and run
without Maven or compilation. Published classes must come from JARs. No reactor
class directories or test libraries are on the consumer classpaths.

Explicit JPMS tests use the original multi-release JARs. For automatic fallback,
the JVM uses those **same original JARs** with
`-Djdk.util.jar.enableMultiRelease=false`
([JDK property documentation](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/jar/JarFile.html));
consumers assert the historical automatic
names documented in the root README. Because javac does not use that runtime
property for module discovery, preparation makes visibly separate
`compile-only-automatic/` copies without module descriptors. These copies are used
only to compile the automatic consumers, never on a runtime path. The ESAPI JAR
is on the module path; its other dependencies remain on the classpath to avoid
unrelated split packages among legacy dependency JARs.

OSGi tests start Felix 5.6.12 (R6) and 7.0.5 (R8), install the actual core/adapter
JARs and a consumer probe bundle, assert ACTIVE state, invoke encoding through
the probe's bundle class loader, and shut down the framework. The framework host
supplies the pinned servlet/JSP/EL or ESAPI API packages via system-package exports.
Encoder code is absent from the host classpath. This tests the encoder bundles'
imports, resolution, and execution; it does not test independently installed
vendor API bundles or a full servlet container. Legacy Felix URL handlers are
disabled because this fixture does not use them.

## Guards and limits

Preparation asserts all four artifacts' automatic/explicit module names,
descriptor requirements (including transitive API readability), exports, OSGi
identities, imports and export versions, absence of execution-environment
requirements, multi-release layout, Java 8 class versions, TLD identities and
referenced packaged classes, and allowed published runtime dependencies. Base
classes must stay within each artifact's own package; test classes and embedded
JARs are rejected. Negative fixture tests verify representative broken packages
fail these guards.

During ordinary `mvn verify`, Animal Sniffer checks each library against the Java 8
API signature. This catches linkage such as Java 9's covariant `CharBuffer.flip()`
even when bytecode still has class version 52. japicmp checks public/protected API
binary and source compatibility against **1.4.0**, the latest available Central
release when this guard was added; update the pinned baseline only after a newer
release is available there. Missing types are not broadly ignored: dependencies
are resolved so inherited API changes remain visible. Both checks run in existing
`build.yaml` jobs because those jobs reach the `verify` phase.

There are no API exclusions today. A future intentional tag-package move requires
an explicit compatibility decision. If approved, add narrowly scoped japicmp
`parameter/excludes/exclude` entries for the affected classes in the parent POM,
with an issue link and migration notes; do not disable compatibility checks for
the whole adapter. New public API members must carry `@since` for their first
release. The XML 1.1 additions already carry `@since 1.4.0`.

JDK 21 and 25 additionally run advisory builds to expose compiler/plugin drift.
They do not replace the supported JDK 17 release build. Their compiler warnings
about obsolete Java 8 options remain visible; warnings alone do not fail these
jobs. There is no assumed permanent javac ceiling: if a future javac removes
`--release 8`, keep the 1.x baseline/build JDK and evaluate a baseline change for
a future major release. Release verification must record the chosen build JDK
and successful runtime matrix, including an actual Java 8 run, before publication.

## Local reproduction

With JDK 17, Maven, and Python 3.8+ on PATH:

```sh
mvn -B -ntp clean verify
python3 compatibility/consumers.py prepare
python3 -m unittest discover -s compatibility/tests
python3 compatibility/consumers.py run --runtime 17 --java-home "$JAVA_HOME"
python3 compatibility/consumers.py run --runtime 8 --java-home /path/to/jdk8
```

Preparation requires an empty `target/compatibility`; use `mvn clean verify` or
choose a new `--directory` when rebuilding. `--maven` and `--repository` allow an
explicit Maven executable and isolated dependency cache. Runtime `--directory`
must point to the prepared fixture directory. CI uploads build reports, API diff
reports, preparation output, and each runtime's output even when a step fails.

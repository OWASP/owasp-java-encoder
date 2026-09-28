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

The published `encoder-jsp` POM defaults its provided API to JSP 2.3.3. The
JSP 2.2.1 row is deliberately older: it is an independent minimum-consumer
fixture that prevents a dependency refresh from silently raising the runtime
contract. The Jakarta Servlet and EL dependencies used by the reactor are
test-only: their declarations remain in the published source POM, but they do
not propagate into ordinary consumer dependency graphs or enter the adapter JAR.

The Jakarta fixture deliberately uses Servlet 5.0, whose minimum Java SE version
is 8 ([specification](https://jakarta.ee/specifications/servlet/5.0/)). Newer servlet
containers/APIs can require a newer JVM. The reactor's Jakarta tests use Servlet
6.1.0 and EL 6.0.1 on the JDK 17 build, and the Docker/Selenium application remains
in the separate JDK 17 `Java CI` job. This smoke matrix is not certification of
every container or transitive dependency on every JDK.

The Java 8 proof includes packaged consumer execution across all three artifacts,
including Jakarta with Java 8-compatible APIs. A separate CI job builds on JDK 17,
then forks the core and JSP unit tests on Temurin 8 and requires a JaCoCo
execution-data file from each module. The Java 8 unit job does not run the
packaged/module-path integration tests or the Jakarta test suite; those remain on
JDK 17. The isolated packaged consumer job covers those three artifacts on Java 8.

## What runs

`consumers.py prepare` copies the three JARs produced by `./mvnw clean verify`, resolves
the pinned fixture dependencies, and compiles consumers independently of reactor
classes or test classpaths. Core consumers assert String and Writer output,
including input that crosses internal buffer boundaries, plus JavaScript and URI
encoding. JSP consumers instantiate the actual tag, set a minimal `JspContext`,
call `doTag()`, and assert output.

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
only to compile the automatic consumers, never on a runtime path.

OSGi tests start Felix 5.6.12 (R6) and 7.0.5 (R8), install the actual core/adapter
JARs and a consumer probe bundle, assert ACTIVE state, invoke encoding through
the probe's bundle class loader, and shut down the framework. The framework host
supplies the pinned servlet/JSP/EL API packages via system-package exports at the
versions the pinned API JARs declare. The
JSP and Jakarta probes also run `ForJsonTag`, which needs the 1.5 core API. Each
adapter is then installed with the released 1.4.0 core and must fail to resolve,
proving its `org.owasp.encoder` import range excludes cores it cannot run on.
Encoder code is absent from the host classpath. This tests the encoder bundles'
imports, resolution, and execution; it does not test independently installed
vendor API bundles or a full servlet container. Legacy Felix URL handlers are
disabled because this fixture does not use them.

## Guards and limits

Preparation asserts all three artifacts' automatic/explicit module names,
descriptor requirements (including transitive API readability), exports, OSGi
identities, imported packages with their exact version ranges, export versions, absence of execution-environment
requirements, multi-release layout, Java 8 class versions, TLD identities and
referenced packaged classes, and allowed published runtime dependencies. Base
classes must stay within each artifact's own package; test classes and embedded
JARs are rejected. Negative fixture tests verify representative broken packages
fail these guards.

During ordinary `./mvnw verify`, Animal Sniffer checks each library against the Java 8
API signature. This catches linkage such as Java 9's covariant `CharBuffer.flip()`
even when bytecode still has class version 52. japicmp checks public/protected API
binary and source compatibility against **1.4.1**, the immutable release immediately
preceding 1.5.0. Update the pinned baseline for the next development version. Missing
types are not broadly ignored: dependencies are resolved so inherited API changes
remain visible. Both checks run in existing `build.yaml` jobs because those jobs
reach the `verify` phase.

There are no API exclusions today. A future intentional tag-package move requires
an explicit compatibility decision. If approved, add narrowly scoped japicmp
`--exclude` arguments for the affected classes in the parent POM, with an issue
link and migration notes; do not disable compatibility checks for a whole artifact.
New public API members must carry `@since` for their first
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
./mvnw -B -ntp clean verify
python3 compatibility/consumers.py prepare
python3 -m unittest discover -s compatibility/tests
python3 compatibility/consumers.py run --runtime 17 --java-home "$JAVA_HOME"
python3 compatibility/consumers.py run --runtime 8 --java-home /path/to/jdk8
```

Preparation requires an empty `target/compatibility`; use `./mvnw clean verify` or
choose a new `--directory` when rebuilding. `--maven` and `--repository` allow an
explicit Maven executable and isolated dependency cache. Runtime `--directory`
must point to the prepared fixture directory. CI uploads build reports, API diff
reports, preparation output, and each runtime's output even when a step fails.

The Docker-free [Jasper engine fixture](jsp-engine/README.md) also runs in normal
`verify`: it compiles and serves both packaged TLD surfaces on maintained javax
and Jakarta engines, with exact-byte and translation-rejection assertions.

Preparation also verifies each source attachment against the main sources and
requires its Java 9 descriptor at `META-INF/versions/9/module-info.java`, plus a
Javadoc index in every documentation attachment. Descriptor sources are added
only after compilation/resource copying; Java 8 compiler inputs and the binary
multi-release layout remain unchanged.

## Published identities and development import ranges

The tables below describe the prepared **1.5.0** artifacts. The names are
historical identities preserved in 1.x; the OSGi import floors reflect the new
1.5 calls and must not be projected onto older published JARs.

### Java 9+ module names

| JAR                 | Explicit JPMS Module  | Automatic-Module-Name     |
|---------------------|-----------------------|--------------------------|
| encoder             | owasp.encoder         | org.owasp.encoder        |
| encoder-jakarta-jsp  | owasp.encoder.jakarta | org.owasp.encoder.jakarta |
| encoder-jsp          | owasp.encoder.jsp     | org.owasp.encoder.jsp     |

The multi-release descriptors define the explicit Java 9+ module names. The
manifest names intentionally retain their historical values for consumers that
disable multi-release support or otherwise use automatic-module discovery.

The adapter modules also require their public API dependency on the module path:

| Adapter module            | Required dependency module | Supported Maven artifact                              |
|---------------------------|----------------------------|-------------------------------------------------------|
| `owasp.encoder.jsp`       | `javax.servlet.jsp.api`    | `javax.servlet.jsp:javax.servlet.jsp-api:2.3.3`       |
| `owasp.encoder.jakarta`   | `jakarta.servlet.jsp`      | `jakarta.servlet.jsp:jakarta.servlet.jsp-api:3.0.0`   |

These dependencies are transitive in the module descriptors because their types
appear in the adapters' public APIs. The JSP dependencies are automatic modules;
use the original Maven artifact filenames so Java derives the module
names shown above. Servlet containers continue to provide the JSP APIs at runtime,
and classpath-based applications are unaffected.


### OSGi bundles

| JAR                 | Bundle-SymbolicName             | Export-Package           | Imports `org.owasp.encoder` | Imports API packages                                        |
|---------------------|---------------------------------|--------------------------|-----------------------------|-------------------------------------------------------------|
| encoder             | `org.owasp.encoder`             | `org.owasp.encoder`      | (none)                      | (none)                                                      |
| encoder-jsp         | `org.owasp.encoder.jsp`         | `org.owasp.encoder.tag`  | `[1.5,2)`                   | `javax.servlet.jsp`, `javax.servlet.jsp.tagext`: `[2.0,3)`   |
| encoder-jakarta-jsp | `org.owasp.encoder.jakarta-jsp` | `org.owasp.encoder.tag`  | `[1.5,2)`                   | `jakarta.servlet.jsp`, `jakarta.servlet.jsp.tagext`: `[3.0,4)` |

The symbolic names are fixed; note that the Jakarta bundle's differs from its
`Automatic-Module-Name`. Core exports `org.owasp.encoder` at its release version.
The JSP and Jakarta tags require core 1.5 because they call `Encode.forJson`.
Jakarta Pages 4 is not in the accepted range until compatibility with it has been
verified.

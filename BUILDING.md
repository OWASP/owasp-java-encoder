# Building and checking Java Encoder

Use JDK 17+ and the committed `./mvnw` (`mvnw.cmd` on Windows). The wrapper is
Apache Maven Wrapper 3.3.4's unmodified `only-script` distribution; no wrapper
JAR executes before the download check. It pins Maven 3.9.16 and its SHA-256.
The downloaded Maven ZIP was independently compared to the SHA-512 served at
[Apache's distribution site](https://downloads.apache.org/maven/maven-3/3.9.16/binaries/).
Wrapper archive SHA-256: `6cb584c2bc907b849a0b931d8266d3ff3214cdd3127115ed4f49fb7176413d36`.
Both bootstrap paths are CI gates, including a deliberately wrong checksum with
an empty wrapper cache. `.mvn/maven.config` requires strict transfer checksums.
These checks detect corruption against the recorded hash; checksums fetched from
the same publisher do not independently establish publisher identity. Review
wrapper scripts, distribution URLs and hashes together when upgrading.

```sh
./mvnw -B -ntp clean verify
./mvnw -B -ntp clean verify -PtestJakarta  # requires Docker
python3 compatibility/consumers.py prepare --repository /path/to/isolated/m2
python3 compatibility/consumers.py run --runtime 17
```

Run from the root, with `-pl core -am`, or from a module using `../mvnw`.
The wrapper's `.mvn` root also anchors Checkstyle paths. Maven's JVM must be 17+
(Maven 3.9.16+); Java 8 is a **forked unit-test/consumer JVM**, never the build JVM.
Newer JDK build jobs remain advisory. Library class files retain releases 8/9.
The five library POMs share one Enforcer execution: tool minimums, duplicate
coordinates, dependency convergence, upper bounds and explicit plugin versions.
The separate Boot application applies those rules under its own parent/BOM.

## Source policy

Checkstyle plugin 3.6.0 with engine 12.3.1 checks main sources during validate:
headers, whitespace/newlines, illegal/redundant/unused imports, empty statements,
equals/hashCode and file/type names. No method-size rule forces encoder-loop
refactoring. Test sources remain out of scope. Checkstyle's current 13/14 engine
requires Java 21; 12.3.1 is the explicit Java 17 compatibility exception, not a
claim of upstream support for older engines. Review migration when the build JDK
changes. Its parser cannot parse module declarations, so `module-info.java` is
excluded from Checkstyle; compiler, packaged descriptor/source guards and actual
JPMS consumers cover it. Headers were added to those four descriptors and four
app files using their 2024 Jeremy Long introduction commits. All existing BSD
notices and original attribution remain. TLD license comments remain intact;
JSP server-side comments do not emit output. The app declares the same BSD license.

## Coverage

Normal `verify` writes HTML/XML/CSV in each library's `target/site/jacoco` and
checks per-module floors. The empty aggregator has no classes/data and is skipped.
Coverage measures unit-test execution only. Failsafe packaged/OSGi consumers and
forked JSP engines deliberately have no agent; neither do isolated runtime jobs.
Surefire uses late evaluation of both the prepared agent and caller `argLine`.
Empty defaults support `-Djacoco.skip=true`. Appending execution data intentionally
unions successive unit JVMs in one build; use `clean` for an independent baseline.
CI's Java 8 run starts in its own job/cache and uploads its own reports/data.

| Module | Measured lines | Measured branches | Line floor | Branch floor |
| --- | --- | --- | --- | --- |
| core | 1228/1240 (99.032%) | 890/903 (98.560%) | 99.0% | 98.5% |
| jsp | 66/66 | no branches | 100% | 100% |
| jakarta | 66/66 | no branches | 100% | 100% |
| esapi | 27/28 (96.429%) | no branches | 96.4% | 100% |

Floors round the current baseline down to 0.1 percentage points. They are not a
claim that every encoding behavior is covered. CI retains reports alongside test
results. Changes that intentionally alter these baselines require reviewed evidence.

## Retired Maven Site

The old Site/Reflow/Velocity/Doxia, FindBugs, PMD, JXR and report-only bindings
were dormant. Unique core/JSP examples moved to [docs/usage.md](docs/usage.md).
README/module docs and attached source/Javadoc JARs remain supported. Build-bound
Checkstyle/JaCoCo, Surefire XML, CodeQL and dependency submission replace useful
reports. Maven's implicit Site plugin is pinned to 3.22.0 and skipped to prevent
falling back to an old lifecycle default; `mvn site` is not a publishing path.
Existing GitHub Pages and the `gh-pages` branch remain available and unchanged.

## Dependency signature trust decision

Mandatory dependency/plugin PGP verification is deferred. An arbitrary key
retrieved on first use is not trusted merely because a signature verifies. A
future enforced policy needs reviewed full expected fingerprints per publisher,
rotation/revocation handling, expiring unsigned exceptions and a trusted verifier
bootstrap. A non-failing trial would only collect observations. No current rule
claims to verify the provenance of dependencies, plugins, Maven bootstrap or
extensions that execute before a verifier could run. Today the boundaries are
reviewed pins/checksums, TLS repositories, isolated caches and dependency/advisory
review. Release artifact PGP verification is a separate policy in RELEASING.md.

The measured regression probe ran only `EncodeFacadeTest` with fresh execution
data: core line coverage fell to 76.0% and branch coverage to 61.5%; both checks
failed. A separate clean `-Djacoco.skip=true` verify passed without a test-agent
argument error. The full baseline data was kept separate from that probe.

Lifecycle pins are in root `pluginManagement`, with explicit versions on API,
source-helper and signature plugins. Maven 4 prerelease plugins were deliberately
not selected for this Maven 3 build. The optional app inherits maintained plugin
pins from Boot 4.1.1 and adds explicit Enforcer/Checkstyle/disabled Site pins.
Review effective POMs for normal, `testJakarta`, and `sign-artifacts` profiles;
`dependency:resolve-plugins` feeds their resolved closures into dependency review.
Release-only publisher dependencies are submitted separately with the same
GitHub detector and a distinct correlator, so they do not hide runtime graphs.

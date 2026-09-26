# Issue #91 implementation checkpoint — incomplete

Stopped at the user's request to save progress for a new session. This is work in
progress, not a completed CI change and not ready for PR review or merge.

## Location and ownership

- Repository: OWASP/owasp-java-encoder
- Issue: https://github.com/OWASP/owasp-java-encoder/issues/91
- Worktree: `/private/tmp/encoder-91`
- Branch: `test-91-packaged-runtime-matrix`
- Starting main: `339bf2bfa07ebc54185b317da1411694970e5e15` (PR #160 merge)
- Maven: `/private/tmp/encoder-maintenance-tools/apache-maven-3.9.12/bin/mvn`
- JDK 17: `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`
- Isolated Maven cache: `/private/tmp/encoder-astra-xml-m2`

The authorized workflow for every PR remains Astra implementation -> Sol review
and fixes -> Astra review of fixes and exact-head CI verification -> merge. Do not
merge this checkpoint. No PR has been opened for #91. The clearly marked WIP
branch is pushed to origin so the checkpoint survives temporary-directory cleanup.

## Added so far

`consumers.py` stages the four packaged JARs, checks manifest/module/OSGi identities,
base and descriptor bytecode versions, expected package exports/imports, TLD class
references, and some test-content/dependency exclusions. It resolves separate API
dependency sets and attempts to compile independent consumers for classpath,
explicit JPMS, automatic module fallback, and OSGi execution.

The standalone Java consumers contain real core String/Writer calls, an ESAPI
adapter call, and JSP `doTag()` output assertions using a minimal context/writer.
The JSP source is a javax template; the preparation script derives Jakarta source.
`OsgiConsumer` is intended to install the actual core/adapter/probe JARs, with API
libraries exported by the framework host. No encoder JAR is on the host classpath.
This host-provided dependency arrangement must be documented rather than claimed
as complete vendor bundle/container integration.

Dependency fixtures use JSP 2.2.1 / Servlet 3.0.1 / EL 2.2.5; Jakarta JSP 3.0.0 /
Servlet 5.0.0 / EL 4.0.0 (Java 8 baseline, intentionally not Servlet 6); ESAPI 2.7.0.0;
and Felix 5.6.12 (R6) / 7.0.5 (R8).

## Validation actually performed

- Existing reactor `mvn package -DskipTests` completed on JDK 17; this built the
  packaged artifacts, not runtime execution tests. Log: `/private/tmp/encoder-91-package.log`.
- Metadata assertions passed for all four built JARs.
- The preparation script downloaded its isolated dependencies.
- Core classpath consumer compiled with `--release 8`.
- Core explicit-module consumer compiled with `--release 9`.
- Preparation then FAILED compiling the core automatic module consumer:
  `module not found: org.owasp.encoder`. Passing
  `-J-Djdk.util.jar.enableMultiRelease=false` to javac did not make its module
  discovery use the automatic name. Log: `/private/tmp/encoder-91-prepare.log`.
- No consumer has been executed on any runtime. No Java 8 hosted job exists yet.
- No GitHub Actions workflow or finished support documentation has been added.

All commands have exited. Generated artifacts are under ignored `target/compatibility`.

## Exact next steps

1. Read the current issue #91 acceptance criteria and this checkpoint. Fetch/merge
   the latest `origin/main`; #153 and other maintenance PRs may have merged since
   this worktree's base. Preserve 1.4.1 security changes and the current version.
2. Correct automatic-module compilation. Investigate a javac-supported way to
   compile against automatic names, or create documented compile-only descriptor-
   stripped JAR copies while runtime tests still use ORIGINAL published JARs with
   multi-release support disabled. Never silently replace the artifact under test.
3. Complete preparation for all consumers and run them locally on JDK 17; fix
   concrete compile/runtime issues. Check dependency module names, ESAPI runtime
   dependencies/configuration, real JSP output, OSGi class loading, and shutdown.
4. Finish artifact guards: explicit descriptor contents/readability, published
   metadata and TLD identities, robust test-dependency exclusion, and Java 8 API
   baseline validation. Current metadata checks alone are not exhaustive API checks.
5. Add a separate `.github/workflows/consumer-compatibility.yaml`: build and stage
   artifacts/fixtures using JDK 17, then download them into isolated runtime jobs
   on actual Java 8, 11, 17, 21, and 25. Keep Docker/Selenium under its existing JDK
   17 job. Preserve failure logs as artifacts. Coordinate with changes to build.yaml
   from PR #154; a separate workflow was agreed to avoid conflict.
6. Document runtime baseline per artifact, exact API versions, intentional automatic
   vs explicit names, applicability of Java 8 (no JPMS), and the OSGi host fixture.
7. Open one focused PR only after coherent local verification. Sol must review and
   fix it; Astra must review those fixes and all CI before merge. Obtain a real
   successful hosted Java 8 run and record its link. Do not claim compilation or
   class version checks are Java 8 runtime execution.

Preparation command used (from this worktree):

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
python3 compatibility/consumers.py prepare \
  --maven /private/tmp/encoder-maintenance-tools/apache-maven-3.9.12/bin/mvn \
  --repository /private/tmp/encoder-astra-xml-m2
```

Planned runtime command after preparation succeeds (not yet run):

```sh
python3 compatibility/consumers.py run --runtime 17 \
  --java-home /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
```

Do not start subsequent issues #133 or #126 until the user resumes the broader
maintenance effort. Root owns final cleanup of retained worktrees.

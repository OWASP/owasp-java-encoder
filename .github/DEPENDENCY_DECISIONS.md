# Dependency proposal decisions — 1.5.0

Initial review: 2026-09-26. Focused API follow-up: 2026-09-27. Dependabot
proposal-policy follow-up: 2026-09-28.

PRs [#176](https://github.com/OWASP/owasp-java-encoder/pull/176) and
[#188](https://github.com/OWASP/owasp-java-encoder/pull/188) mixed ordinary build
maintenance with changes to intentional tool/API/engine baselines. These decisions
apply to the reviewed proposals, not to every future version or security advisory.
The [1.x compatibility record](../docs/compatibility-decisions.md) and
[build policy](../BUILDING.md) define the contracts being preserved.

## Accepted build update

Use Maven Bundle Plugin **6.2.0** in place of 6.1.2. Its bnd library moves from
7.3.0 to 7.4.0; the [Felix release notes](https://felix.apache.org/documentation/news.html)
include a fix for dependency resources entering JARs when a module descriptor is
present. This is a packaging tool update, not a reason to change bundle identities,
OSGi import floors, JPMS names, library dependencies or Java 8 runtime support.
Validate generated JAR entries/manifests, original-JAR consumers and deterministic
payloads after the change. Felix's **Maven plugin** is distinct from the deliberately
old **OSGi framework** compatibility fixture.

## Accepted focused API updates

Use `javax.servlet.jsp-api` **2.3.3** as the published `encoder-jsp` provided
dependency. Keep JSP 2.2.1, Servlet 3.0.1 and EL 2.2.5 as the independent minimum
consumer fixture. Japicmp resolves 1.4.1 against the old support API and the 1.5
artifact against the new one, so inherited API changes are not hidden. The Java 8,
JPMS, OSGi and packaged Jasper checks continue to exercise the original artifacts.
This is a consumer-POM dependency change, not a claim that the minimum supported
JSP container was raised to 2.3.

Use Jakarta Servlet **6.1.0** and Jakarta EL **6.0.1** for the reactor's test
classpath. Their test-scope declarations remain visible in the published source
POM, but Maven does not propagate them into ordinary consumer dependency graphs
and their classes do not enter the adapter JAR. Keep the independent Java 8
packaged-consumer fixture on Jakarta Pages 3.0.0, Servlet 5.0.0 and EL 4.0.0. The
old Servlet 6.0.0 and EL 4.0.0 support JARs remain explicit japicmp inputs for the
1.4.1 side of the comparison; the new side uses the new test APIs.

## Deferred proposals and reconsideration conditions

| Proposal | Disposition and required evidence before reconsideration |
| --- | --- |
| Checkstyle 12.3.1 → 14.1.0 | Keep 12.3.1 for the JDK 17 build. PR #188 fails with Java 21 classfile version 65 on Java 17. Reconsider with a separately reviewed build/release-JDK migration and source-policy validation; this is not a claim that the old engine has upstream support. |
| Plexus Utils 3.6.2 → 4.1.0 in GPG/Central plugin dependencies | Keep the reviewed 3.6.2 mitigation. The [upstream 4.x migration](https://github.com/codehaus-plexus/plexus-utils) moves XML utilities to a separate artifact; 4.1 also changes DirectoryScanner default exclusions. A newer major is not a drop-in plugin-realm security fix. Reconsider with actual plugin linkage, isolated signing/bundle/rehearsal evidence, transitive-advisory review and repeatable payloads. |
| javax Servlet 3.0.1 → 4.0.1; EL 2.2.5 → 3.0.0 | Keep the test-only minimum API fixtures. These are not bundled production container implementations. Modern engine coverage is separate; replacing the minimum tests would remove evidence for existing consumers. |
| Jakarta Pages 3.0.0 → 4.0.0 | Keep the published provided Pages 3 API and existing `[3.0,4)` package ranges. Reconsider only with a reviewed minimum-runtime/API migration, public POM implications and compatibility evidence. |
| Jasper/annotations 9.0.122 or 10.1.60 → 11.0.26 in the isolated tag fixtures | Keep coherent Tomcat 9 (`javax`) and 10.1 (`jakarta`) engines. PR #188 fails the javax engine with missing `javax.servlet.jsp.tagext.SimpleTagSupport` after the Tomcat 11 switch. The optional Boot/browser WAR already exercises Tomcat 11. Patch updates within each intended engine line remain candidates for manual review; Dependabot's lowest-version classification cannot automate both lines independently. Cross-line migration needs a separate coverage decision. |

A dependency's test/build scope does not dismiss an advisory. Check each finding's
actual affected versions, executed path and proposed remedy; use a supported fix
or document a specific mitigation/decision. Security alerts remain visible. These
proposals do not authorize raising a library or release baseline, and a future
security fix may require revisiting a decision above.

## Older PR #176

Its GPG 3.2.8, Central 0.11.0, versions-maven-plugin 2.22.0, Boot 4.1.1 and
optional-app API modernization were already delivered by #180/#185; #187 supplied
the reviewed publisher-plugin mitigations. Site is deliberately disabled with an
explicit lifecycle version. Doxia/Reflow and dormant project-info/FindBugs/PMD/JXR
report tooling were retired in #185, so their old update proposals are obsolete.
The versions plugin pin remains; only its old reporting execution was retired. The remaining Felix change is
accepted above, and the library API proposals have explicit dispositions above.
Close #176 as superseded, without restoring removed tooling or bypassing its
failed tests. Replace #188's mixed group with the focused accepted change and this
record; a grouped PR closure is not proof that every proposed upgrade was applied.

## Future Dependabot proposals

The [configuration](dependabot.yml) excludes the eleven baseline-sensitive
coordinates above from the broad Maven **version-update group**. For the ten
coordinates with a rejected proposal in #218–#227, it also ignores only the
SemVer minor or major version-update classes covered by the decisions above.
This prevents the weekly job from recreating proposals that merely replace
historical comparators, minimum-consumer fixtures, coherent servlet-engine
lines, or reviewed tool majors. The accepted Felix Maven Bundle Plugin remains
individually updateable.

Every new rule uses `update-types`, not a version range or an unqualified
coordinate ignore. GitHub documents that `update-types` affects version updates,
not security updates, so the Maven security-update group and security alerts
remain eligible:
<https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference#update-types-ignore>.
Future minor or major upgrades covered by these rules require a deliberate
compatibility change and manual proposal; remove or narrow the matching rule as
part of that reviewed change. Ordinary Maven changes continue separately.

Dependabot combines repeated Maven coordinates and classifies an update from the
lowest version it found. Consequently, when one coordinate has both a historical
input and a newer current input, a current-line patch may be classified as a
minor or major change from the old floor and suppressed from routine version
PRs. This affects, for example, the split JSP/Jakarta API comparators and the
mixed Plexus Utils plugin realms. It is an explicit noise-versus-automation
tradeoff: manually review those coordinates during dependency and release
maintenance, and always when an advisory appears. The behavior is pinned in
Dependabot's [`DependencySet`](https://github.com/dependabot/dependabot-core/blob/a6e095f540c6542facc06ba47f66418da50d669c/common/lib/dependabot/file_parsers/base/dependency_set.rb#L158-L170)
and [`IgnoreCondition`](https://github.com/dependabot/dependabot-core/blob/a6e095f540c6542facc06ba47f66418da50d669c/common/lib/dependabot/config/ignore_condition.rb#L50-L65)
implementations.

The root directory monitors the complete Maven reactor once; listing each reactor
module again produced duplicate pull requests. The standalone
`compatibility/dependencies` Maven project remains a separate monitored directory.
No security-alert dismissals or automatic merges are introduced. The pre-existing
unqualified Felix framework fixture exception remains scoped and documented in
[CI/security operations](CI_SECURITY.md); it is the only ignore rule without an
explicit version-update class.

When a new security proposal or manually raised compatibility change revisits a
deferred baseline, compare it with this dated record and current upstream evidence.
Do not automatically close a security proposal or infer permanent rejection from
an older version decision.

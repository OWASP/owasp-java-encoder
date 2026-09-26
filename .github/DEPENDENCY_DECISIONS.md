# Dependency proposal decisions — 2026-09-26

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

## Deferred proposals and reconsideration conditions

| Proposal | Disposition and required evidence before reconsideration |
| --- | --- |
| Checkstyle 12.3.1 → 14.1.0 | Keep 12.3.1 for the JDK 17 build. PR #188 fails with Java 21 classfile version 65 on Java 17. Reconsider with a separately reviewed build/release-JDK migration and source-policy validation; this is not a claim that the old engine has upstream support. |
| Plexus Utils 3.6.2 → 4.1.0 in GPG/Central plugin dependencies | Keep the reviewed 3.6.2 mitigation. The [upstream 4.x migration](https://github.com/codehaus-plexus/plexus-utils) moves XML utilities to a separate artifact; 4.1 also changes DirectoryScanner default exclusions. A newer major is not a drop-in plugin-realm security fix. Reconsider with actual plugin linkage, isolated signing/bundle/rehearsal evidence, transitive-advisory review and repeatable payloads. |
| javax JSP 2.2.1 → 2.3.3 | Keep the published provided API and minimum fixture. A changed consumer POM dependency is observable even if no new API method is called. Reconsider only with explicit compatibility policy and old-container/OSGi/JPMS evidence. |
| javax Servlet 3.0.1 → 4.0.1; EL 2.2.5 → 3.0.0 | Keep the test-only minimum API fixtures. These are not bundled production container implementations. Modern engine coverage is separate; replacing the minimum tests would remove evidence for existing consumers. |
| Jakarta Pages 3.0.0 → 4.0.0 | Keep the published provided Pages 3 API and existing `[3.0,4)` package ranges. Reconsider only with a reviewed minimum-runtime/API migration, public POM implications and compatibility evidence. |
| Jakarta Servlet 6.0.0 → 6.1.0; EL 4.0.0 → 6.0.1 | Keep the deliberate test API set. Reconsider test-baseline changes with explicit coverage goals and minimum-consumer evidence, rather than automatically matching the newest application container. |
| Jasper/annotations 9.0.122 or 10.1.60 → 11.0.26 in the isolated tag fixtures | Keep coherent Tomcat 9 (`javax`) and 10.1 (`jakarta`) engines. PR #188 fails the javax engine with missing `javax.servlet.jsp.tagext.SimpleTagSupport` after the Tomcat 11 switch. The optional Boot/browser WAR already exercises Tomcat 11. Patch updates within each intended engine line remain reviewable; cross-line migration needs a separate coverage decision. |

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
coordinates above from the broad Maven **version-update group**, not from update
eligibility. They therefore receive individual proposals and compatibility review;
ordinary Maven changes can proceed separately. This follows GitHub's
[group matching rules](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference#groups).
All original directories remain monitored. The Maven security-update group is
unchanged, and no new `ignore` rules, security-alert dismissals or automatic merges
are introduced. The pre-existing Felix framework fixture exception remains scoped
and documented in [CI/security operations](CI_SECURITY.md).

When a new proposal repeats a deferred baseline change, compare it with this dated
record and any new advisory or upstream evidence. Do not automatically close a
security proposal or infer permanent rejection from an older version decision.

# CI and security operations

## Required coverage and trust boundaries

`Java CI gate` requires the clean JDK 17 reactor build, JSP/Jakarta source parity,
CI policy tests, the Docker/Selenium browser test, exact reactor JAR inclusion in
the Jakarta WAR, and every ESAPI version from 2.5.1.0 through 2.7.0.0.
`Packaged consumer gate` requires JDK 17 artifact preparation/API and Java 8
signature checks, package guard tests, the original packaged bytes on Java
8/11/17/21/25, and core/JSP/ESAPI unit tests with the Java 8 JVM and coverage.
JDK 21/25 build probes remain advisory. Require **both** gates: neither observes
the other workflow. The gates run with `always()` and accept only `success` for
every expected dependency; missing, failed, skipped and cancelled jobs fail.

Both workflows run on pushes to main, pull requests, weekly schedules and manual
dispatch, with bounded timeouts and per-workflow/ref concurrency cancellation.
There are no path filters that could silently omit a required check. Changes to
workflow topology must update the gate dependencies and required check names
only after the replacement checks have succeeded.

Every checkout disables credential persistence. PRs use `pull_request`, never
`pull_request_target` or privileged `workflow_run` execution. Fork tokens are
read-only and receive no repository secrets; all external contributors require
run approval. A fork's artifacts are consumed only within that same unprivileged
run. Artifact download has no cross-run/repository/token input, preserving the
original packaged bytes. No privileged job restores PR artifacts or caches.

All Maven invocations use a fresh runner-temporary repository. Shared Maven
caching is deliberately disabled, including Java 8's necessary `install`, all
scanner builds, and intentional release commits. Setup Java's pinned v6.0.1
does expose cache controls, but this configuration does not rely on any of them.
See [local quarantine guidance](../RELEASING.md#maven-storage-and-repository-controls).

Baseline main runs [Java CI 36220505798](https://github.com/OWASP/owasp-java-encoder/actions/runs/36220505798)
and [consumers 36220505783](https://github.com/OWASP/owasp-java-encoder/actions/runs/36220505783)
had 14 Maven cache misses and one hit (Java 8 install job). Build took 3m14s;
ESAPI jobs 65–97s; preparation 85s; Java 8 tests 87s; runtimes 12–16s.
The separate clean test-compilation and install lifecycles are now one clean
verify lifecycle. Further core sharing between ESAPI versions is deferred: it
would complicate reactor resolution and package checks for little measured gain.

## Scanning and dependency updates

Advanced CodeQL in `codeql.yaml` is the single analysis owner; leave default
setup unconfigured. Java uses a manual JDK 17 build of all four libraries and
the optional `testJakarta` application; Actions and Python tooling use extraction
without a build. It runs on PRs, main, a weekly schedule and manual dispatch.
Only analysis jobs request `security-events: write`; fork PRs use GitHub's
restricted PR SARIF upload path, not a general write token. Reports are retained
as artifacts and in Code scanning. No scanner secrets are required.

Dependency submission runs only for the trusted default branch, including manual
dispatch. Its only elevated permission is job-level `contents: write` for the
snapshot API. It builds its own checkout without caches or imported artifacts.
Separate correlators submit the normal reactor and the optional Jakarta profile.
The pinned Maven submission action includes all resolved project scopes,
including runtime, test and provided dependencies. Maven dependency plugin
3.9.0 `resolve-plugins` separately resolves build/report plugins and their
transitives; `scripts/build-dependency-snapshot.py` submits those edges as
development dependencies. Graph reports and submission JSON are retained for
inspection. Inspect representative ESAPI/AntiSamy HTTP transitives and Jakarta
Spring/Tomcat dependencies in the resulting graph; alert counts are not gates.

Dependabot checks all library POMs, the parent and optional app weekly, with
separate Maven and SHA-pinned Actions groups and grouped Maven security updates.
Normal review and complete CI apply to automated PRs; no automatic merging is
configured. Review new action source and transitive downloads as well as pins.
The nonstandard XML files under `compatibility/dependencies` remain explicit
manual compatibility fixtures. In particular Felix 5.6.12 is an intentional
OSGi R6/Java 8 baseline, not a production dependency; the Maven ignore prevents
an automatic baseline replacement. Inspect any advisory against its actual
local bundle-loading test scope, and record a specific disposition. Do not
suppress advisories across all Felix versions or application deployments.

ESAPI advisory triage lives in [the adapter guide](../esapi/README.md#dependency-security-triage).
Upstream fixes are preferred; tested mitigations remain possible. No transitive
finding is dismissed merely because another library introduces it. Optional
Scorecard publication, best-practices registration and another scheduled scanner
are follow-ups, not prerequisites for these operating controls.

## Repository controls and recovery

Before changes, snapshot repository/ruleset, Actions, Pages, collaborator/team
and webhook settings. Store webhook configuration privately; do not publish
endpoint tokens. Record changes and negative tests in issue #169.

Use read-only default Actions tokens, no bot PR approvals, supported secret
scanning/push protection, all-external-contributor run approval, and an allowlist
of the exact action repositories/paths in [the action inventory](ACTION_PINS.md).
Enable SHA enforcement after pins and the allowlist are ready. Re-read both
Actions endpoints afterward: updating the general permissions endpoint may reset
`github_owned_allowed` to true; reapply the exact allowlist and test rejection of
an unlisted GitHub-owned action as well as a mutable action tag. When adding a
reviewed action, update the allowlist before merging the workflow. Old open PRs
must refresh their workflows before rerunning under the SHA policy.

Main requires both complete CI gates and successful CodeQL language jobs in a
separate ruleset with no bypass. The review rule keeps one independent approval,
dismisses stale approvals, requires approval of the latest push, and requires
resolution of review threads. The
verified maintainers Jim Manico and Jeremy Long have only
PR-based, audit-visible emergency **review** bypass; it cannot bypass required
checks or directly push main. An emergency bypass is not independent approval.
CODEOWNERS enforcement remains with #127's ownership validation; do not name
unverified owners or treat review rules as CODEOWNERS validation.

All tags prohibit update/deletion with no bypass; new signed release tags still
follow `RELEASING.md`. GitHub `required_signatures` checks commit signatures,
not annotated tag signatures. Legacy Codacy and Travis webhooks have no current
workflow/required-check owner and return 404/502; disable them while retaining
their private configuration for recovery. Site retirement remains with #96.

For recovery, an administrator uses Settings or the REST API, records the actor,
reason and affected PR in #169, makes the narrowest temporary change, then
restores and verifies the controls. Do not introduce privileged PR workflows to
repair policy failures. Check names can be repaired without removing review
requirements; action patterns can be added without disabling SHA enforcement.
There is no standing direct-push bypass. Never move a published tag or blanket
disable tag protection. A disposable unpublished policy-test tag can be removed
using an exclusion for that exact tag only, then immediately removing the
exclusion. Review branch cleanup against current main and open PRs individually.

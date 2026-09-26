# Batch 02 validation — 2026-09-26 UTC

Implementation: [PR #173](https://github.com/OWASP/owasp-java-encoder/pull/173),
initial implementation `49bfc33`. This covers #102, #109, #97, #119 and #108 under
[#169](https://github.com/OWASP/owasp-java-encoder/issues/169). No release artifacts,
release tags, library algorithms or dependency versions were changed.

## Implementation validation

- Actionlint 1.7.12 passes every workflow. Every external `uses` reference is a
  full 40-character SHA; authoritative release mappings and runtime trust review
  are in [ACTION_PINS.md](../.github/ACTION_PINS.md).
- Ten Python policy/parser tests pass. Negative cases cover accidental release
  versions, mismatched module/application versions, snapshot/tag mismatch,
  intentional release and next-snapshot flow, missing/failed/cancelled/skipped
  required jobs, malformed build reports, dependency edges and classifiers.
- Local Maven 3.9.12 / OpenJDK 17.0.20.1 `clean verify` passed in a newly created
  repository: 2,118 unit tests and eight Failsafe tests, with API compatibility
  and Java 8 signatures. No release coordinates were installed into shared
  storage. Packaged consumers pass on Java 17, including classpath/module-path
  and OSGi; all 11 package-guard tests pass.
- The optional Jakarta application packages and generates its graph in the
  same reactor without `install`. Its WAR contains the byte-identical
  `encoder-jakarta-jsp` JAR from this build. CI also asserts this after the
  required Docker/browser test. Local Docker browser execution was not used;
  the real test passed in GitHub CI.
- Local resolved graphs include Commons Configuration 1.10, Commons Lang 2.6,
  HttpClient 5.4.4, HttpCore 5.3.4, Spring Web 6.2.19 and Tomcat 10.1.55.
  Build-plugin graphs include direct plugins and transitives such as
  `maven-compiler-plugin` and `plexus-compiler-javac`, submitted as development
  dependencies. The normal and optional-profile submissions have separate
  correlators. Default-branch delivery and final SBOM verification are recorded
  in #169 after merge; an arbitrary number of alerts is not an acceptance gate.

The first live default-branch run exposed GitHub's cross-detector manifest
precedence: build-only submissions hid runtime dependencies for the same POM.
The follow-up uses one detector with four separate correlators, which GitHub
merges, preserving the real source POM paths and all runtime/test/build scopes.
Final SBOM and follow-up delivery evidence are recorded in #169. A transient
Dependabot security-update attempt during the initial incomplete graph reported
`dependency_not_found`; no alert was manually dismissed or suppressed.

All **26 checks** passed on the initial PR head, including the two new gates,
all ten ESAPI versions, every packaged runtime, Java 8 unit tests, the browser
build, three CodeQL analyses and both advisory build probes:

- [Java CI](https://github.com/OWASP/owasp-java-encoder/actions/runs/36221392615)
- [Packaged consumers](https://github.com/OWASP/owasp-java-encoder/actions/runs/36221392627)
- [CodeQL](https://github.com/OWASP/owasp-java-encoder/actions/runs/36221392642)

CodeQL's Java, Actions and Python analyses all uploaded reports with zero
findings. The final provenance check replaced CodeQL's annotated tag-object ID
with its dereferenced commit SHA, preserving the reviewed release source. Final
head check evidence is recorded in #169. The browser build took 2m39s versus the 3m14s baseline; packaged
preparation took 1m52s versus 1m25s; Java 8 tests took 1m13s versus 1m27s.
These are single-run observations, not a benchmark. See the baseline cache
inventory and preserved coverage in [CI_SECURITY.md](../.github/CI_SECURITY.md).

Fork behavior was checked against the real external-contributor PR #168
[Java CI run](https://github.com/OWASP/owasp-java-encoder/actions/runs/36220576710):
`pull_request`, fork head repository, read-only contents/metadata token,
`Secret source: None`, and successful browser/matrix checks. The new workflows
retain that event boundary and add no PR content-write job, shared Maven cache,
credential persistence, or cross-run artifact source. CodeQL uses GitHub's
restricted fork-PR SARIF path. A new CodeQL run from an external fork was not
manufactured; the full three-language PR analyses passed on #173.

## Live settings and negative tests

Settings were read and privately snapshotted before editing. REST readback
confirms read-only default Actions tokens, disabled Actions PR approvals,
`all_external_contributors` approval policy, enabled secret scanning and push
protection, and Pages HTTPS enforcement. HTTPS required the settings UI because
REST returned a certificate-not-found error; both the checked UI and subsequent
REST readback confirm the saved setting. The existing site and Pages source
were preserved. Codacy/Travis webhooks returning 404/502 are disabled, with
configuration retained privately for recovery.

The exact seven-pattern action allowlist is active, with GitHub-owned and
verified-publisher blanket allowances disabled, and full-SHA enforcement enabled.
Updating the general Actions endpoint initially reset the GitHub-owned allowance;
a live negative probe caught this, and the exact allowlist was reapplied.

- [Mutable action tag rejected before execution](https://github.com/OWASP/owasp-java-encoder/actions/runs/36221410237).
- [Unlisted pinned GitHub-owned action rejected before execution on rerun](https://github.com/OWASP/owasp-java-encoder/actions/runs/36221409741).
  Its initial harmless probe ran and explicitly failed, exposing the reset;
  the final startup rejection confirms the corrected policy.
- The all-tags ruleset rejects update and deletion, with no bypass. Both REST
  attempts against a disposable test tag returned HTTP 422. The tag was removed
  through a temporary exclusion for that exact test ref, then the exclusion was
  removed. Every original tag/ref/object was compared unchanged afterward.
- Main's review rules require one approval, stale-review dismissal, latest-push
  approval and resolved review threads. The prior all-admin `always` bypass is
  replaced by PR-only bypass for verified maintainers `jmanico` and `jeremylong`.
  GitHub rejected the otherwise verified team ID, so the supported individual
  user actors are used. Emergency bypass is explicitly not independent review.
- A separate no-bypass ruleset requires `Java CI gate`, `Packaged consumer gate`,
  and the three CodeQL language jobs, bound to the GitHub Actions application
  (15368), with up-to-date branches required. It was enabled only after all five
  check names had successful runs.
- A disposable branch with exact copies of both main rulesets rejected a direct
  update with an unchanged-tree commit: HTTP 422, PR required and all five status
  checks expected. The branch remained unchanged; temporary rulesets and branch
  were removed. Main was unchanged. #173 separately remained `REVIEW_REQUIRED`
  after successful checks, verifying the normal review gate.

Recovery steps were recorded before restrictions and are now in
[CI_SECURITY.md](../.github/CI_SECURITY.md#repository-controls-and-recovery).
CODEOWNERS validation stays with #127; site retirement with #96; future release
workflow pins with #95. Optional public Scorecard/best-practices registration and
additional scheduled scanners are explicitly deferred. Existing ESAPI advisories
remain visible with version/reachability/scope triage in the adapter guide; none
is blanket-suppressed. The 1.5 full-backlog release gate remains unchanged.

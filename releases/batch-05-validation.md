# Batch 05 validation

Checked 2026-09-26 against implementation based on `3bd8625`. This is maintenance
validation, not release approval. No historical signed artifact, release title,
release timestamp, key or tag was changed.

## Consumer docs and provenance

- All five exact 1.4.1 Central POMs returned HTTP 404; the signed GitHub release
  exists. README/context/taglib guidance labels unreleased 1.5 behavior explicitly.
- The documented JPMS example compiled and ran on JDK 17 against the original
  GitHub `encoder-1.4.1.jar`, after detached-signature verification against full
  primary fingerprint `1C5F632B86809F2F5DB25092BEA0075F94074A9B`. Output matched
  `&lt;hello&gt;`. Sol independently repeated the example.
- Basic/advanced method lists were compared with all four current TLDs and the
  immutable 1.4.1 tag. JSON/XML 1.1 tag bindings are new in 1.5; extended forUri
  deprecation is distinguished from the already deprecated released facade.
- CHANGELOG was checked against the eight repository tags, five GitHub releases,
  merged PRs and retained README announcements. The 1.2.2 tag/announcement dates
  differ and are labeled; early missing release timestamps are not invented.
  Historical GitHub metadata is retained explicitly, not backfilled or reformatted.
- `docs/dependencies.md` inventories all 28 distinct resolved consumer third-party
  compile/runtime/provided coordinates from dependency-plugin 3.11.0 tree JSON.
  License declarations follow exact POM parent inheritance, including unnamespaced
  POMs and AntiSamy's HTTPS namespace. XML API artifact notices provide additional
  component-level provenance. Test/build/WAR graphs are outside this inventory.

## Build and metadata

A new isolated Maven repository and wrapper cache with JDK 17 passed
`./mvnw -B -ntp clean verify`: the full library tests, API/Java 8 signature checks,
coverage and both packaged JSP engines passed. Java 17 isolated classpath,
explicit/automatic JPMS and Felix R6/R8 consumers passed, including old-core
rejection. All 17 artifact guards, 14 policy/verification tests and 34-file
javax/Jakarta source parity passed. Docker/browser and other runtime legs are
verified by the required PR CI, not claimed as locally executed.

All four generated binary manifests have `Bundle-Vendor: OWASP Foundation` and
`Bundle-DocURL: https://owasp.org/projects/java-encoder`. All five effective POMs
share that URL/organization, verified maintainer IDs `jmanico`/`jeremylong` and
original-author credit for Jeff Ichnowski. Existing API/module/bundle identities,
dependency scopes and bytecode constraints pass the packaged guards. No Java
implementation is changed. Release instructions now require this metadata check
and a final byte comparison after metadata edits.

## Community and live settings

The official [OWASP project page](https://owasp.org/projects/java-encoder) identifies
Jim and Jeremy as leaders; its current public Jim contact is `jim@owasp.org`, used
in POM metadata. Existing private-security/custody contact channels are preserved.
The official [Code of Conduct](https://owasp.org/policy/code-of-conduct) and
[Foundation donation page](https://owasp.org/donate) resolve. The funding manifest
from merged #157 resolves and identifies maintainer support through Manicode
Security separately from Foundation donations.

Both CODEOWNERS accounts were read back as repository administrators. Current
rules match CONTRIBUTING: one approval, stale dismissal, latest-push approval,
resolved threads, strict required gates/CodeQL without bypass, and named-user
PR-only review bypass. Mandatory owner approval remains off. No new DCO or public
response SLA is introduced. Discussions remains disabled; ordinary blank issues
remain allowed. Public forms direct suspected vulnerabilities to private channels.
Community YAML parses and local Markdown targets were checked.

The signed-in GitHub settings showed wiki editing unrestricted. It was restricted
to users with push access before migration. Four latest pages are preserved
byte-for-byte with hashes at `docs/archive/wiki-2019`, from wiki commit
`a10dea70c502c02d072d3d9d2dc520d7c042587a`; full mirror/history bundle retained for
recovery. Current docs replace useful examples and explicitly retire old unsafe
CSS/URL and unpatched-IE advice. Hide the wiki only after the reviewed archive and
replacements merge, preserving its repository/history. Repository description and
topics will then reflect Java 8+, contextual output encoding and core/adapter scope.
Post-merge setting/community readback and CI results are recorded in #169.

The separate OWASP website still has older download examples. Its publication
update remains coordinated under #111; changing this repository does not update
that website. Central access/custody and four early signing-key authorization
gaps (#111/#110) remain separate from this documentation batch.

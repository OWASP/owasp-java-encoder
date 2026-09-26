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

## Final deterministic payload comparison

Commit `c319456d719d6c2af3968ea6028348520380bf2b` was exported twice with
`git archive`; each build used its own new empty Maven repository and wrapper
cache. Reference Eclipse Temurin 17.0.20.1+1 (macOS aarch64 archive SHA-256
`196d13ba5f10414bef7f6a05a9b3f00edacb18ebacef2b99485db9e2ee18f0e8`), Maven 3.9.16,
macOS 27.0, UTC/C locale and explicit UTF-8/en-US JVM properties were used.
Both clean installs passed, and all 12 binary/source/Javadoc JARs plus five
installed POMs matched. Later template/validation-only commits do not affect
artifact inputs. No cross-OS comparison or signing-timestamp reproducibility is
claimed. There was no release signing, upload or tag operation in this batch.

| Artifact | SHA-256 (both builds) |
| --- | --- |
| `encoder-1.5.0-SNAPSHOT-javadoc.jar` | `a756dd361b2d6296fb2cb94f7f0c20c0a470d6dfaab7518832ca83b26641e7ca` |
| `encoder-1.5.0-SNAPSHOT-sources.jar` | `0cd1292c1e03b9554be4f5c0d92ad4702f3a53da20e1c52b5f4423726370541a` |
| `encoder-1.5.0-SNAPSHOT.jar` | `5cb47d6dda79f404440ba629121db9f618b1dcbbfdf9164f8b6d72a4c0f681d8` |
| `encoder-1.5.0-SNAPSHOT.pom` | `2c7a39dfb2b04ae75b2b8bda5695a2f3dee373ad0d7f36a55f1010aabe463afe` |
| `encoder-esapi-1.5.0-SNAPSHOT-javadoc.jar` | `cd16a149a0c8aed8d1e244e88f7cde1f6ae482e212dc2377afdb05f63549874b` |
| `encoder-esapi-1.5.0-SNAPSHOT-sources.jar` | `de93cb5d0d4233263f9ed175406fb93215611bc108507a4d31f2b7e3bb21e8c4` |
| `encoder-esapi-1.5.0-SNAPSHOT.jar` | `801e02da63462266c45864a7448d09fdcc1a69515bcef63847a017470d6fa749` |
| `encoder-esapi-1.5.0-SNAPSHOT.pom` | `8876d4eac1ad4f23117e0e27a9d184fbdbf20ab35e57bb8a88c2451a750c0e03` |
| `encoder-jakarta-jsp-1.5.0-SNAPSHOT-javadoc.jar` | `494b1e428320798d2678eaf1e0d9f42efc45926b70b0065d8198d74ff7ea53c8` |
| `encoder-jakarta-jsp-1.5.0-SNAPSHOT-sources.jar` | `b8743c89d737a415fc571f8e31cb8d6a11c1a113841bd4bea8ba771eed8bb533` |
| `encoder-jakarta-jsp-1.5.0-SNAPSHOT.jar` | `6bb58fb26ebc521c8c01718425304489bfa8c8ee482aedb8f3b6551e01726e1a` |
| `encoder-jakarta-jsp-1.5.0-SNAPSHOT.pom` | `df4eece564afbb1aeecda9b05d0f8b190a1bc7d1e92f1bf4829a6c4df78847f4` |
| `encoder-jsp-1.5.0-SNAPSHOT-javadoc.jar` | `5dcbfc0e53938e69dae6345e43ba310f7d0e3bc2e67b4c105c85a9601aac2fb2` |
| `encoder-jsp-1.5.0-SNAPSHOT-sources.jar` | `81b201dd6965a8add5726198665d65cf6f258da8e3986a670c4f976ca63f8e7f` |
| `encoder-jsp-1.5.0-SNAPSHOT.jar` | `18440b84b8af3fb7f09c58411305cf236b6dc8cf107e8b1daf1afcb0c4afa3ce` |
| `encoder-jsp-1.5.0-SNAPSHOT.pom` | `c2e21aa5107fc215a6cae907776b4c2ee889376b226ae948f1ad841c4910d5ca` |
| `encoder-parent-1.5.0-SNAPSHOT.pom` | `1ed1ac590f76f07113f4a14381902490aad63e47bc00da05c5720f73185182bd` |

Sol's reviews caught extended-deprecation version labeling, an overbroad XML 1.1
warning, a moved relative link and an unstaged template. All were corrected;
final Sol re-review found no actionable issues. Astra's final review and final-head
CI results are recorded with the merge in #169. Model review is not independent
maintainer approval.

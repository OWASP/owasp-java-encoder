# OWASP Java Encoder <VERSION>

<!-- Replace every placeholder and remove instructions before publication. -->

**Publication status:** <signed GitHub artifacts available / Central pending /
Central availability independently verified, with date and exact links>.
Do not equate a GitHub tag, successful build or staging validation with Central
publication. State whether this is a security release and the upgrade destination.

## Highlights and security

- <User-visible highlights; only merged changes in the release tag.>
- <Advisory links, affected versions/entry points, fixed behavior and required action.>
- <Explicitly state when there are no security fixes; do not imply an audit.>

## Compatibility and migration

- Runtime/build JDK requirements and tested packaged consumers.
- API, exact encoded output, null/Unicode, contexts, dependency scopes/versions,
  Maven coordinates, JPMS/automatic/OSGi identities and servlet namespace effects.
- Before/after examples for changed output and steps consumers must take.
- Features or proposals that are still unreleased must not appear as shipped.

## Changes

### Added

- <Feature and PR.>

### Fixed

- <Correctness/security fix and PR/advisory.>

### Changed or deprecated

- <Compatibility impact, replacement, retention/removal policy and PR.>

### Build and documentation

- <Relevant maintenance and verification improvements; avoid claiming runtime changes.>

## Coordinates and availability

Group `org.owasp.encoder`, version `<VERSION>`: `encoder`, `encoder-jsp`,
`encoder-jakarta-jsp`, `encoder-esapi`; parent `encoder-parent`.
The optional `jakarta-test` WAR is not published. Include exact Central artifact
links only after verification. While Central is pending, give signed GitHub
artifact verification and local/organizational installation instructions.

## Verification and evidence

- Source commit and immutable tag: <...>.
- Signing identity and independently checked **full primary fingerprint**: <...>.
- Link release `KEYS`, detached signatures, signed checksum manifests and the
  version-specific verification commands; follow `VERIFYING.md`.
- Reference toolchain/OS, clean-cache build and CI links, packaged runtime/parser
  tests, deterministic payload comparison and exact bundle SHA-256: <...>.
- Publication verification date and unresolved limitations: <...>.

Use [RELEASING.md](../RELEASING.md) for gates and [1.4.1.md](1.4.1.md) as an example.
Never rebuild, re-sign, move a tag or replace historical signed artifacts merely
to apply this format. A notes edit must preserve security and pending notices.

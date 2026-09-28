# Verify downloaded release artifacts

Use the **full expected fingerprint**, authenticated through the project's release
record, alongside the signature. A keyserver is a public-key transport, not an
identity authority. Obtain [KEYS](KEYS) from a trusted project revision and check
the fingerprint before use. Never import private key material to verify a release.

For 1.4.1 and later releases until a documented rotation, the expected project key
is `1C5F632B86809F2F5DB25092BEA0075F94074A9B`. The [1.5.0 release record](releases/1.5.0.md#verification-and-evidence)
covers its signed GitHub assets. The [Central publication verification](releases/1.5.0-central-publication.md)
confirms that Central serves the same artifacts and signatures.

## Fresh public-only keyring

Download the artifact, its original `.asc`, and the trusted `KEYS`. This example
verifies the published 1.5.0 release. For historical releases, use the full
fingerprint mapped below and review the historical key's expiry and algorithms.
Commands use GnuPG and `shasum` (or equivalent SHA tools).

```sh
verify_home=$(mktemp -d)
chmod 700 "$verify_home"
gpg --homedir "$verify_home" --batch --no-autostart --import KEYS
expected_fingerprint=1C5F632B86809F2F5DB25092BEA0075F94074A9B
artifact=encoder-1.5.0.jar
gpg --homedir "$verify_home" --no-autostart --fingerprint "$expected_fingerprint"
gpg --homedir "$verify_home" --batch --no-autostart --status-fd 1 \
  --verify "$artifact.asc" "$artifact" > signature.status || exit 1
awk -v expected="$expected_fingerprint" \
  '$2 == "VALIDSIG" && ($3 == expected || $NF == expected) { valid=1 }
   END { exit !valid }' signature.status || exit 1
# After verification, remove this disposable public-only keyring.
rm -r "$verify_home"
```

`VALIDSIG` supplies the signing fingerprint and, for a subkey, its primary key's
fingerprint. An untrusted-owner warning is expected in a fresh keyring; do not
mark arbitrary keys trusted to hide it. Expired/revoked-key warnings require an
explicit historical review, not a blanket bypass. A signature establishes that
the bytes were signed by the selected key; it does not prove the software is safe.

## Checksums: two distinct formats

A Maven `.sha256` sidecar usually contains **only a hex digest**, not a filename.
After fetching it over the intended distribution channel, form a check manifest:

```sh
artifact=encoder-1.5.0.jar
expected_hash=$(tr -d '[:space:]' < "$artifact.sha256")
printf '%s\n' "$expected_hash" | grep -Eq '^[[:xdigit:]]{64}$' || exit 1
printf '%s  %s\n' "$expected_hash" "$artifact" | shasum -a 256 --check || exit 1
```

Use the analogous 128-digit check with `shasum -a 512` for `.sha512`. Old Central
versions may offer only MD5/SHA-1 sidecars; do not invent a stronger upstream
checksum or treat those as signature substitutes. In contrast, a release's
`SHA256SUMS` already contains filenames. Verify `SHA256SUMS.asc` with the expected
key as above, then run `shasum -a 256 --check SHA256SUMS`. The same applies to
`SHA512SUMS` with `-a 512`. A checksum alone, especially from the same download
origin, does not authenticate a publisher.

## Historical signing-key evidence

Reviewed 2026-09-26 against the original core JARs and detached signatures at
[Maven Central](https://repo.maven.apache.org/maven2/org/owasp/encoder/encoder/).
Every listed original signature mathematically verified in a fresh public-only
keyring; the pre-1.3 certificates archived here are now expired. Signature
verification and the authentication records below are separate evidence. The
table records the primary fingerprint, not a short key ID, and release-use
periods rather than all possible key uses.

| Releases | Observed primary fingerprint | Authentication/archival status |
| --- | --- | --- |
| 1.1 | `37D880CD406BAD34CA2A8DD61845EF37A3B6533A` | Expired; archived after Jim Manico's retrospective exact-fingerprint authentication on 2026-09-26 |
| 1.1.1 | `AD0C981AEE36D3880512E28F5AD6F7C8740E3CF2` | Expired; archived after Jim Manico's retrospective exact-fingerprint authentication on 2026-09-26 |
| 1.2 | `C82AF58D3985677F9D575CEC9BC190E3DA071BD4` | Expired; archived after Jim Manico's retrospective exact-fingerprint authentication on 2026-09-26 |
| 1.2.1 | `33F28D32BAB335D03EC5DAD6F7EBA8ECD6F22BFE` | Expired; archived after matching Jeremy Long's public GitHub key record 213069 |
| 1.2.2–1.2.3 | `F9514E84AE3708288374BBBE097586CFEA37F9A6` | Expired 2021-10-13; archived in KEYS |
| 1.3.0, 1.3.1, 1.4.0 | `259A55407DD6C00299E6607EFFDE55BE73A2D1ED` | Historical personal key; archived in KEYS |
| 1.4.1 onward until rotation | `1C5F632B86809F2F5DB25092BEA0075F94074A9B` | Current dedicated project key |

The 1.2.2–1.2.3 fingerprint is authenticated by Jeremy Long's contemporaneous
[OWASP Dependency-Check v6.0.0 verification guide](https://github.com/dependency-check/DependencyCheck/blob/b7040668cdc83976bb3f4e11d65e49f2a00d7ac4/cli/src/site/markdown/index.md.vm),
which records the full fingerprint; its public key verifies both Encoder releases.
The 1.3.0–1.4.0 fingerprint was already recorded in this project's
[KEYS at the rotation](https://github.com/OWASP/owasp-java-encoder/blob/b51c575/KEYS)
and is corroborated by the [maintainer's Dependency-Check guide](https://dependency-check.github.io/DependencyCheck/dependency-check-cli/index.html).
Retrieved keys were checked against those full records before adding minimal
public exports to KEYS. For 1.2.1, the primary public-key packet in
[Jeremy's GitHub account key listing](https://api.github.com/users/jeremylong/gpg_keys)
(record 213069, added 2017-08-20) yields the exact full fingerprint above.
For 1.1, 1.1.1 and 1.2, Jim authenticated the exact fingerprints on 2026-09-26,
citing his involvement from the project's beginning and recruitment of Jeff
Ichnowski. This is retrospective maintainer authentication, not a recovered
contemporaneous fingerprint announcement or a claim that Jim held those private
keys. See the [authentication and verification record](releases/historical-key-authentication.md)
for source limits, certificate dates, signature times and artifact hashes.
Keyserver availability and matching UIDs alone are still insufficient authority.

The three oldest signatures use SHA-1 and 1.1 uses a 1024-bit DSA key; these are
historical facts, not algorithms to use for new releases. Preserve their original
bytes/signatures. Current key custody, independent recovery and Central access
are recorded in [MAINTAINERS.md](MAINTAINERS.md) / #111. Record each newly authorized
project key's full fingerprint before first use and retain the old public
verification record.

OpenPGP detached artifact signing is separate from Java `jarsigner`: it signs the
whole downloaded file without adding JAR entries. This project does not claim
that its JARs carry Java code-signing certificates or that a PGP signature creates
a Java runtime trust decision.

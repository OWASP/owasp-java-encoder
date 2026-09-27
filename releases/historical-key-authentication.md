# Historical signing-key authentication and verification

Recorded 2026-09-26 (America/Los_Angeles); verification completed
2026-09-27 05:12 UTC. This completes the remaining archival evidence for
[#110](https://github.com/OWASP/owasp-java-encoder/issues/110), following
[PR #185](https://github.com/OWASP/owasp-java-encoder/pull/185).
The complete release-to-fingerprint mapping is in [VERIFYING.md](../VERIFYING.md).

## Authentication of the four newly archived keys

Jim Manico explicitly confirmed that he could authenticate the following exact
fingerprints in the maintainer work session on 2026-09-26:

| Release | Full primary fingerprint |
| --- | --- |
| 1.1 | `37D880CD406BAD34CA2A8DD61845EF37A3B6533A` |
| 1.1.1 | `AD0C981AEE36D3880512E28F5AD6F7C8740E3CF2` |
| 1.2 | `C82AF58D3985677F9D575CEC9BC190E3DA071BD4` |

Asked for the basis, he stated: “I have been part of this project from the
beginning, I originally recruited Jeff Ichnowski”. This record relies on Jim's
retrospective maintainer authentication. No contemporaneous fingerprint
announcement or separate direct confirmation from Jeremy was recovered for
these three keys. It does not claim that Jim possessed their private keys.
Original authorship, release preparation and artifact signing are distinct;
all four newly archived certificates contain Jeremy Long UIDs. Matching UIDs
or successful signature verification alone were not used as authorization.

For **1.2.1**, Jeremy's public
[GitHub account GPG-key listing](https://api.github.com/users/jeremylong/gpg_keys)
contains record **213069**, added `2017-08-20T08:20:01.000-07:00`. Decoding its
`public_key` and calculating the version-4 primary-key fingerprint gives
`33F28D32BAB335D03EC5DAD6F7EBA8ECD6F22BFE`, matching the original Central
signature. The returned 272-byte public-key packet has SHA-256
`5372283957ae77546761756ec6da71fcfebeaebeee3d16e957c47b63a89cdae7`.
The public packet and selected API metadata are preserved in the
[machine-readable evidence](historical-key-evidence.json), so this comparison
does not depend on the account retaining that key indefinitely. This is an
account-associated historical key record, not a new statement by Jeremy.

The API's `raw_key` was null; it did not supply a complete certificate. Its
expiry metadata extends into 2019, whereas the retrieved full certificate's
self-signature expires in 2017. The archived certificate is the latter; no
unavailable renewal certificate is claimed. The February 2017 artifact signature
predates both expiry dates. The observed API record was not marked revoked.

Public certificates were transported from Ubuntu's keyserver using each full
fingerprint, then checked against the authentication above. Minimal public
exports were added to [KEYS](../KEYS). The existing three key blocks, including
the current dedicated project key, are byte-for-byte unchanged. No private key,
owner-trust assignment, signature replacement or key-rotation operation is part
of this archival work.

## Archived certificate dates and historical limits

| Release | Primary-key creation (UTC) | Archived certificate expiry (UTC) | Original signature packet time (UTC) |
| --- | --- | --- | --- |
| 1.1 | 2012-11-14 02:04:18 | 2013-11-14 02:04:18 | 2013-02-15 01:49:06 |
| 1.1.1 | 2013-11-16 14:53:13 | 2014-11-16 14:53:13 | 2014-01-28 00:57:29 |
| 1.2 | 2015-03-03 11:12:38 | 2017-03-05 15:50:31 | 2015-04-10 19:59:40 |
| 1.2.1 | 2016-04-23 20:53:53 | 2017-04-23 20:53:53 | 2017-02-19 11:59:54 |

Signature packet times fall within the archived certificates' validity periods;
they are signer-supplied times, not independent timestamp-service attestations.
GnuPG reports `EXPKEYSIG` for 1.1 through 1.2.3 today. The three oldest artifact
signatures use SHA-1; 1.1 also uses 1024-bit DSA. These keys and algorithms are
retained solely for historical verification and are not authorized for new
releases. Consumers with stricter algorithm policies may reject them; this
record does not recommend weakening those policies or deploying old releases.

## Consumer verification results

GnuPG 2.5.20 imported the final `KEYS` into a new isolated home containing seven
primary public keys and **zero secret keys**. Each of the ten original Central
core JAR/signature pairs (1.1, 1.1.1, 1.2, 1.2.1, 1.2.2, 1.2.3, 1.3.0, 1.3.1,
1.4.0 and 1.4.1) returned success and `VALIDSIG` with its expected full primary
fingerprint. No weak-digest override, clock substitution or trust assignment
was used. Expiry warnings remain recorded. The JSON evidence contains source
URLs, JAR/signature SHA-256 hashes, certificate dates and signature status for
all ten pairs. These locally computed hashes identify the inspected bytes;
they are not represented as upstream SHA-256 sidecars for old releases.

Both shell blocks in `VERIFYING.md` ran successfully against the original
Central 1.4.1 core JAR, detached signature and raw SHA-256 sidecar in a separate
fresh public-only home. The analogous raw SHA-512 check also passed. Supplying
the wrong expected fingerprint to the documented status check failed. Both
original 1.4.1 `SHA256SUMS` and `SHA512SUMS` signatures verified against the
dedicated project key, and each manifest passed all 35 checksum entries.
README links to `KEYS` and `VERIFYING.md` are present.

This verifies the historical core release mapping and the consumer commands;
it does not claim an exhaustive re-download of every historical adapter,
source JAR or POM. The separate [1.4.1 publication audit](1.4.1-central-publication.md)
covers all of that release's published payloads. Original release bytes, tags
and signatures were preserved. Future authorized fingerprints must still be
recorded before first use, and OpenPGP artifact verification remains distinct
from Java `jarsigner` and runtime trust.

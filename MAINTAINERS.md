# Maintainers and release-key custody

## Responsibilities

The project leaders are [Jim Manico](https://github.com/jmanico) and
[Jeremy Long](https://github.com/jeremylong). They coordinate maintenance,
releases, and security response. Private vulnerability reports use the channels
in [SECURITY.md](SECURITY.md), including jim.manico@owasp.org and
jeremy.long@owasp.org as the email fallback.

For each release, the coordinating maintainer records the source commit, review
and test results, signing fingerprint, exact artifact checksums, and publication
status using [RELEASING.md](RELEASING.md). A release coordinator must separately
verify the permissions needed to stage and publish; being named here does not
establish GitHub or Maven Central access.

Jim and Jeremy are the two designated custodians of the dedicated **OWASP Java
Encoder Release** signing key. Each is responsible for an independent recovery
copy in his own vault, a recovery drill using that copy, and keeping its records
current. The key's public fingerprint is:

`1C5F632B86809F2F5DB25092BEA0075F94074A9B`

The public key is in [KEYS](KEYS). This is a shared project signing identity;
custody of it does not grant permission to publish Maven coordinates.

## Latest recorded custody and publishing status

The latest custody and rehearsal confirmations remain those reported on
2026-09-26 below; this cleanup does not assert that new recovery drills ran.
The [1.6.0 publication record](releases/1.6.0-central-publication.md) documents
Jim's subsequent successful Central publication on 2026-10-18.

### Central publication follow-up: 2026-09-26 (America/Los_Angeles)

Jim's signed-in Portal account now shows the verified `org.owasp.encoder`
namespace in the Owasp organization. The exact retained signed 1.4.1 bundle was
validated and published as deployment `ce91e36f-756c-489f-bbea-3629b728ad28`.
All 17 POM/JAR files and their 17 signatures downloaded from Central matched
the retained release byte for byte. See the [publication record](releases/1.4.1-central-publication.md).

This supersedes the earlier namespace-access and pending-publication status
for Jim and 1.4.1.

Jim also confirmed on 2026-09-26 (America/Los_Angeles) that he and Jeremy both
completed the independent vault-recovery drills, namespace-access checks, and
separate validated-and-dropped Central staging rehearsals that day. This is
maintainer-reported completion; the individual private recovery records and
rehearsal deployment IDs were not supplied or independently inspected in this
publication session. Keep those operational records in each custodian's vault.

| Custodian | Independent vault-recovery drill | Namespace access | Separate validated-and-dropped rehearsal |
| --- | --- | --- | --- |
| Jim Manico | Completed 2026-09-26, reported by Jim | Verified directly by 1.4.1 publication | Completed 2026-09-26, reported by Jim |
| Jeremy Long | Completed 2026-09-26, reported by Jim | Confirmed 2026-09-26, reported by Jim | Completed 2026-09-26, reported by Jim |

Together with the publication verification and reconciled consumer notices,
this records completion of #111 on the stated evidence. Earlier observations
are retained in the [historical status record](docs/archive/maintenance-2026-09/maintainer-status.md).
Future recovery drills and release rehearsals follow the procedures below and
[RELEASING.md](RELEASING.md).

## Independent recovery copies

Each custodian's vault must contain a complete recovery set:

- the encrypted private-key export;
- the public key and full fingerprint, checked against `KEYS`;
- the private revocation certificate;
- the key passphrase, in a protected vault item;
- recovery instructions and the custodian's import and drill records.

The passphrase may be in a separate protected item, but each custodian must be
able to retrieve it without the other person's vault, computer, or login. A link
to the other custodian's copy is not an independent recovery copy. Confirm that
attachments can be retrieved from the vault before recording an import as done.

Transfer recovery material only through an agreed secure vault-sharing process.
Keep private keys, passphrases, revocation certificates, and publishing tokens
out of the repository, Dropbox folders, email, issue/PR text, and build logs.
Local recovery files and keyrings must be owner-only (directories `0700`, files
`0600` on Unix) and outside source checkouts and synchronized folders.

The revocation certificate is for deliberate retirement or compromise recovery.
Do not import it during a routine recovery drill or publish it as a release
asset. A key rotation updates the public key and fingerprint records and both
custodians' recovery sets; historical artifacts and signatures remain unchanged.

## Recovery drill for each custodian

Perform this drill after importing a vault copy and after changing the key,
passphrase, or recovery set. Both custodians must record their own results;
testing the original local backup does not test either person's vault copy.

1. Retrieve the encrypted private-key export and passphrase from your own vault.
   Create two new, empty, owner-only GnuPG homes in a private local temporary
   directory: one for recovery and one for verification. Use `--homedir`
   explicitly for every GnuPG command so the drill cannot use an existing
   keyring or its cached passphrase.
2. Import the retrieved encrypted private-key export into the recovery home.
   Inspect its full fingerprint and expiration; compare the fingerprint with
   `KEYS` from a trusted project checkout and the record above. Stop on a
   mismatch, expiration, or revocation.
3. Create a harmless challenge file identifying the custodian, date, and drill.
   Make an armored detached signature, selecting the full project fingerprint
   with `--local-user`. Unlock through GnuPG's passphrase prompt using the vault
   copy. If a passphrase cache or system keychain signs without requiring that
   copy, clear or disable that cached retrieval and repeat the signing step
   before counting the drill. Never place the passphrase in command arguments
   or logs. Do not sign a release artifact or tag for this drill.
4. Import only the public `KEYS` into the verification home and verify the
   detached signature with both filenames specified explicitly. Require a
   successful verification and compare the full `primary-key-fpr` argument
   (after `sig-class`) in the OpenPGP `VALIDSIG` status line with the expected
   project key. The first fingerprint identifies the signing key or subkey;
   it equals the primary fingerprint when the primary key signs. See the
   [GnuPG status specification](https://github.com/gpg/gnupg/blob/master/doc/DETAILS#validsig-args).
   A trust warning in a fresh keyring does not replace this fingerprint check.
5. Record the result and remove the temporary recovered files and both temporary
   keyrings after stopping their GnuPG agents. Preserve the vault copies.

The key operations, with paths replaced by the private drill locations, are:

```sh
gpg --homedir /private/path/recovery-home --import /private/path/encrypted-private-key.asc
gpg --homedir /private/path/recovery-home --fingerprint --list-secret-keys
gpg --homedir /private/path/recovery-home \
  --local-user 1C5F632B86809F2F5DB25092BEA0075F94074A9B \
  --armor --detach-sign /private/path/challenge.txt
gpg --homedir /private/path/verification-home --import /path/to/trusted/checkout/KEYS
gpg --homedir /private/path/verification-home --status-fd 1 \
  --verify /private/path/challenge.txt.asc /private/path/challenge.txt
```

See the [GnuPG command reference](https://www.gnupg.org/documentation/manuals/gnupg/Operational-GPG-Commands.html)
for these operations. Keep a private record in each custodian's vault with the
custodian's name, import date, drill date, fingerprint, key expiration, GnuPG
version, challenge and signature, verification result, and cleanup result.
Record failures and follow-up work as well as successes. Share only a status
summary with the other maintainer; never include recovery secrets in it.

## Publishing access and unavailable maintainers

Maintain separate evidence for signing-key recovery and Central publishing
access. Each publisher needs his own Portal account with access to
`org.owasp.encoder` (or its parent namespace), and his own token when using
Maven. Verify namespace permissions in the Portal before staging. Follow
[RELEASING.md](RELEASING.md) for access recovery and Central's
[organization access documentation](https://central.sonatype.org/publish/publish-portal-organizations/).

Each publisher's staging rehearsal must use a distinct, nonpublished rehearsal
version and bundle, validate a deployment with `autoPublish=false`, then drop it
without publishing. Never reuse the retained 1.4.1 bundle for a rehearsal or
attempt to republish 1.4.1. Record the account, date,
source commit, bundle checksum, deployment identifier, validation result, and
confirmed drop, without recording credentials. A key-recovery drill or a
successful local build does not establish this staging result.

If one custodian is unavailable, the other can recover signing capability from
his own vault after a successful drill. Publishing still requires verified
namespace access and the release gates. If neither maintainer is available,
coordinate with the OWASP Foundation and Central Support to establish project
ownership and restore namespace access; that process cannot recover a lost
private key. If neither independent key copy is recoverable, arrange a new
project key and publish the new fingerprint before signing any future release.

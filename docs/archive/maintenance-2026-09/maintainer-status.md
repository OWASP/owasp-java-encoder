# Historical maintainer status — September 2026

These observations predate the completed publication and custody follow-up.
For the latest recorded status and current procedures, use
[MAINTAINERS.md](../../../MAINTAINERS.md). No new recovery result is asserted here.

## Recorded status: 2026-09-25

The encrypted local backup was successfully imported and used to sign and verify
a challenge in an isolated temporary keyring; that keyring was then removed.
The local release and recovery directories were checked as owner-only (`0700`).
These observations do not confirm the individual vault copies:

| Custodian | Import into own vault | Drill from own vault copy | Central access and staging rehearsal |
| --- | --- | --- | --- |
| Jim Manico | Unconfirmed | Unconfirmed | Portal showed no namespaces and disabled publishing; validated-and-dropped rehearsal unconfirmed |
| Jeremy Long | Unconfirmed | Unconfirmed | Current namespace access and validated-and-dropped rehearsal unconfirmed |

Central Support has acknowledged the existing access request. Continue that
request rather than opening a duplicate. The exact `encoder` 1.4.1 POM still
returned HTTP 404 from Maven Central at this check. Retain the pending notices
and the existing signed 1.4.1 bundle until publication is verified; do not rebuild
or replace those artifacts or move their tag.

### Batch 00 follow-up: 2026-09-25 (America/Los_Angeles)

Jim confirmed initial key setup on this date. This confirms setup only; retrieval
from his own vault, the isolated recovery drill, and a validated-and-dropped
Central staging rehearsal remain unconfirmed. No new independent confirmation
was received from Jeremy.

The signed-in Central Portal account displayed `JIM MANICO`, **No Namespace(s)
Found**, and a disabled **Publish Component** button. All five exact 1.4.1 POM
URLs (the four libraries and parent) returned HTTP 404, and core metadata still
listed 1.4.0. Publication and Jim's staging rehearsal remain blocked on namespace
access; Jeremy's access remains unconfirmed. Continue the existing Support
request. No new request or deployment was created.

The retained public GitHub bundle, all artifact signatures, and signed checksum
manifests were verified; see [batch 00 validation](batch-00-validation.md).
Verification of public artifacts is separate from private-key recovery and
publisher access.

Update this dated record only from each custodian's confirmation and actual
publishing evidence. [#111](https://github.com/OWASP/owasp-java-encoder/issues/111)
owns the remaining independent vault recovery, namespace access, publication,
and staging checks. [#95](https://github.com/OWASP/owasp-java-encoder/issues/95)
owns future release-tooling changes.

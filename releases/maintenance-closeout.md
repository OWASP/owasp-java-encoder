# Maintenance backlog closeout

Audited 2026-09-26 (America/Los_Angeles), after
[PR #208](https://github.com/OWASP/owasp-java-encoder/pull/208) merged as
`1111f7982e492c60abb4897844a02e7b3a44cef7`. The live GitHub inventory before this
documentation PR contained **two open issues (#110 and #169) and zero open
PRs**. This PR supplies the final historical-key evidence and closure record
for those two issues. Its checks, merge and post-merge inventory are recorded
in the linked [maintenance tracker](https://github.com/OWASP/owasp-java-encoder/issues/169).

## Batch dispositions

The original tracker reviewed 30 issues and one PR. The final readback found
every other original batch issue closed, with the following dispositions.
The tracker's dated entries retain exact merge commits, validation results,
review/bypass provenance and scope limits; earlier statements of outstanding
#110/#111 work are superseded by the records below, not erased.

| Batch | Issues and final disposition | Delivery/evidence |
| --- | --- | --- |
| 00 | #112 completed; #111 completed by the publication/access follow-up | PRs #171, #208; [publication and custody evidence](1.4.1-central-publication.md) |
| 01 | #100, #130 completed | PRs #170, #172; tracker validation and migration records |
| 02 | #102, #109, #97, #119, #108 completed | PRs #173, #177; [batch 02 validation](batch-02-validation.md) |
| 03 | #137, #131, #120, #93 completed | PRs #174, #168, #179, #180; tracker validation and consumer/browser evidence |
| 04 | #123, #96, #104, #122, #95, #103, #124, #125 completed; #110 resolved by this archival follow-up | PRs #184, #185, #187; [batch 04 validation](batch-04-validation.md) and [historical-key authentication](historical-key-authentication.md) |
| 05 | #128, #115, #116, #117, #127 completed; #114 consolidated into #128 and closed as not planned, not separately implemented | PR #189; [batch 05 validation](batch-05-validation.md) |
| 06 | #142 completed as a compatibility decision record; #149 rejected and closed as not planned | PR #190; [compatibility decisions](../docs/compatibility-decisions.md) |

The subsequent dependency queue is also accounted for: Actions PR #175 and
focused Maven PR #191 merged; #176 and #188 closed as superseded. The sixteen
individual proposals #192–#207 were inspected and closed with their specific
compatibility rationale. These were explicit dispositions, not claims that
every proposed upgrade was applied. See the
[dependency decisions](../.github/DEPENDENCY_DECISIONS.md) and
[PR queue validation](pr-queue-validation.md). Future proposals remain subject
to review, and existing scoped dependency/advisory decisions retain their
reconsideration requirements.

## Final operational and evidence work

- **#111:** Central deployment `ce91e36f-756c-489f-bbea-3629b728ad28` published
  the retained 1.4.1 bundle. All 34 downloaded Central payload/signature files
  matched the original signed release. Jim reported both custodians' independent
  vault recovery, both publishers' separate validated-then-dropped rehearsals
  and Jeremy's namespace access complete on 2026-09-26. The report is explicitly
  attributed to Jim; no private rehearsal records or deployment IDs were
  independently inspected. PR #208 merged after all 28 checks passed, using the
  existing PR-only maintainer review bypass. All four subsequent main workflows
  passed: [Java CI](https://github.com/OWASP/owasp-java-encoder/actions/runs/36295798270),
  [packaged consumers](https://github.com/OWASP/owasp-java-encoder/actions/runs/36295798276),
  [CodeQL](https://github.com/OWASP/owasp-java-encoder/actions/runs/36295798274), and
  [dependency submission](https://github.com/OWASP/owasp-java-encoder/actions/runs/36295798278).
- **#110:** the remaining four public keys are archived with explicit
  authentication provenance: Jim's retrospective exact-fingerprint confirmation
  for 1.1/1.1.1/1.2, and Jeremy's public GitHub key record for 1.2.1. Ten original
  core release signatures and current consumer commands passed isolated
  verification. Expiry/algorithm limits and the distinction between signing,
  authorship and authentication remain visible in the linked evidence.
- **#169:** all seven batches and the subsequent PR queue now have completion
  or explicit disposition records. The final documentation PR and post-merge
  inventory complete the maintenance execution tracker.

## The 1.5 release gate remains active

Closing the maintenance tracker is not approval to release 1.5. Development
remains **`1.5.0-SNAPSHOT`**. When a 1.5 release is actually proposed, repeat the
complete then-open issue/PR inventory and satisfy every requirement in
[RELEASING.md](../RELEASING.md), including review of new work, final artifact
validation and explicit release approval. Deferrals and an empty queue alone
do not authorize signing, tagging or publication. This closeout changes public
key archives and documentation only; it does not change artifact inputs,
repository protections or immutable 1.4.1 release material.

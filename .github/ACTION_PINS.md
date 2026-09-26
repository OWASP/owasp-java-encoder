# Reviewed action pins

Resolved release tags through each authoritative repository Git ref (including
annotated-tag dereferencing) on 2026-09-26 UTC. These are source pins, not claims
that runtime tool downloads are made immutable by pinning an action.

| Repository/action | Release | Commit |
| --- | --- | --- |
| `actions/checkout` | [v7.0.1](https://github.com/actions/checkout/releases/tag/v7.0.1) | `3d3c42e5aac5ba805825da76410c181273ba90b1` |
| `actions/setup-java` | [v6.0.1](https://github.com/actions/setup-java/releases/tag/v6.0.1) | `de7274f081f381c8f8158605e0321c36c376e2e6` |
| `actions/upload-artifact` | [v4.6.2](https://github.com/actions/upload-artifact/releases/tag/v4.6.2) | `ea165f8d65b6e75b540449e92b4886f43607fa02` |
| `actions/download-artifact` | [v4.3.0](https://github.com/actions/download-artifact/releases/tag/v4.3.0) | `d3f86a106a0bac45b974a628896c90dbdf5c8093` |
| `github/codeql-action` | [v4.38.2](https://github.com/github/codeql-action/releases/tag/v4.38.2) | `2892aa5e19bbd11bc0cff5427e3b750a04d9e3c2` |
| `advanced-security/maven-dependency-submission-action` | [v6.0.1](https://github.com/advanced-security/maven-dependency-submission-action/releases/tag/v6.0.1) | `a64327a7329c9939cf675e458452febe1894a70c` |

CodeQL uses the `init` and `analyze` subactions at the same commit. No other
external actions or reusable workflows are referenced. Upload/download stay on
the existing v4 major versions; upgrading their packaging protocol is separate
work. All actions are JavaScript actions, not composite actions with hidden
`uses` references. Their metadata, entrypoints and relevant credential/download
paths were reviewed, along with the release changes.

CodeQL's release tag is annotated: the Git ref points to tag object
`88585263c0627ee42c0e1c5143a112c8d6f4aa18`, which must be dereferenced to the
commit in the table. Do not copy an annotated tag object's SHA into a commit
pin. Verify the object's type and follow tag targets until it is a commit
(equivalently, inspect the peeled `refs/tags/<version>^{}` Git ref).

- Checkout uses Git and the supplied token for retrieval; persistence is disabled.
- Setup Java runs its bundled JavaScript, downloads Temurin from the upstream
  service when absent from the hosted toolcache and verifies the distribution.
  Dependency caching is disabled. JDK version lines are intentionally updated
  within their supported major versions; a commit pin does not pin the JDK bytes.
- Artifact actions use bundled clients and GitHub's artifact service. Download
  is restricted to the same workflow run; no privileged cross-run reuse occurs.
- CodeQL runs bundled JavaScript and obtains its corresponding CodeQL tools/query
  bundle. No repository token with content-write permission or custom scanner
  secret is available to the analysis jobs.
- Maven submission v6.0.1 runs the checkout's Maven command with the pinned
  `com.github.ferstl:depgraph-maven-plugin:4.0.3`, then submits with GitHub's bundled
  dependency toolkit. It has no composite action references. Its source logs
  the snapshot. Maven plugins and their resolved transitives remain a separate
  supply-chain boundary, covered by build graph submission.

Working allowlist (GitHub-owned/verified blanket allowances are disabled):

```text
actions/checkout@*
actions/setup-java@*
actions/upload-artifact@*
actions/download-artifact@*
github/codeql-action/init@*
github/codeql-action/analyze@*
advanced-security/maven-dependency-submission-action@*
```

GitHub's full-SHA policy separately enforces the immutable action reference.
Dependabot proposes SHA updates weekly; reviewers must repeat source/release and
transitive behavior review, run actionlint, and wait for the full packaging gates.
Future release actions in #95 must satisfy the same inventory and review policy.

# Contributing

Ordinary bugs, context questions, scoped proposals, tests and documentation fixes
are welcome. Read the [context contracts](docs/contexts.md), [compatibility
matrix](compatibility/README.md) and [Code of Conduct](CODE_OF_CONDUCT.md).
Report suspected vulnerabilities **privately** through [SECURITY.md](SECURITY.md),
not public issues, reproduction links or pull requests.

## Build and check a change

Use JDK 17, the committed Maven wrapper, and Python 3.8+ for repository checks.
Libraries target Java 8; the build JVM is not Java 8. Docker is needed only for
the optional local browser/WAR fixture, which remains a required CI job.

```sh
./mvnw -B -ntp clean verify
python3 scripts/check-taglib-parity.py
python3 -m unittest discover -s scripts/tests
python3 compatibility/consumers.py prepare
python3 -m unittest discover -s compatibility/tests
python3 compatibility/consumers.py run --runtime 17 --java-home "$JAVA_HOME"
./mvnw -B -ntp clean verify -PtestJakarta
```

Set `JAVA_HOME` to the JDK used for a consumer run. Prepare consumers only after a
successful reactor verify and in an empty `target/compatibility`; `clean` removes
old preparation output. To exercise one ESAPI matrix version:

```sh
./mvnw -B -ntp -pl esapi -am clean verify -Desapi.version=2.7.0.0
```

Other supported versions and their upstream security status are separate in
[esapi/README.md](esapi/README.md). See [BUILDING.md](BUILDING.md) for the verified
wrapper, Checkstyle's actual source scope/Java 17 exception, measured coverage
floors and diagnostics. The legacy Maven Site and benchmark profiles are retired.
Use fresh execution data when checking coverage; do not lower floors just to make
a change pass. Tests and module descriptors have explicit style-check exceptions,
not a claim that all sources are checked. Preserve original license notices.

## Pull requests and review

Keep the change focused and describe the trigger, resulting behavior, compatibility
impact and validation. Output spelling matters: document new or removed escaping,
null/Unicode changes, affected parser contexts and migration examples. Add behavior
or parser regression tests for a contract change, including Writer boundaries where
relevant. Keep javax/Jakarta sources and packaged TLDs in parity. Public API,
bytecode, module/bundle identities and dependency scope changes need explicit review.
Update the Unreleased changelog only for changes being delivered, not open proposals. Read the
[compatibility and scope decisions](docs/compatibility-decisions.md) before proposing
a new API or a compatibility break.

The current rules require one approval, dismiss stale approvals after changes,
require approval of the latest push and resolved conversations, and require the
compatibility/CI gates plus Java, Actions and Python CodeQL checks on an up-to-date
base. Named maintainers have an audit-visible **PR-only review bypass**; required
CI/security checks have no bypass. CODEOWNERS requests review from verified
maintainers; owner approval is not a separate enforced rule. AI review is useful
but is not independent maintainer approval. See [CI/security operations](.github/CI_SECURITY.md).
No DCO/sign-off requirement or new response-time guarantee is introduced here.

Maintainers triage by reproducibility, affected context, security impact and
compatibility cost. They may request a smaller case, consolidate duplicates or
record a scoped deferral; a proposal is not implementation approval. This is a
volunteer project without a public-issue response SLA. Discussions remains disabled;
use the context-question form. Blank ordinary issues remain enabled for reports
that do not fit a form. None of these public routes is for vulnerability evidence.

## Funding

The existing [funding authorization](.well-known/funding-manifest-urls), merged in
[#157](https://github.com/OWASP/owasp-java-encoder/pull/157), points to
[Jim Manico's maintenance funding manifest](https://manicode.com/funding.json).
That manifest describes maintainer support through Manicode Security and its
contact/payment channels; it is not an OWASP Foundation donation page. Separately,
you can [donate to the OWASP Foundation](https://owasp.org/donate). Check the chosen
recipient and current terms directly. Contributions of tests, reviews and docs
are also valuable; funding does not determine review or security decisions.

Release signing, access/custody and publication use [RELEASING.md](RELEASING.md)
and [MAINTAINERS.md](MAINTAINERS.md), not the normal contributor build.

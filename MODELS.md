# Model-assisted development and review

Use this workflow to develop and review changes to OWASP Java Encoder.
Model-assisted review supplements independent maintainer review.
Use the full workflow for substantial encoding/security changes and focused
review for small corrections. Follow [CONTRIBUTING.md](CONTRIBUTING.md) and the
existing repository controls; this document does not authorize publication,
merging, releases, or changes to those controls.

## Lineup

Use the lineup below, verifying current availability through the installed tools and
official provider documentation. Record the actual model ID and tool version;
report unavailable slots and substitutions explicitly. Do not claim a review
ran merely because a CLI accepted a model name.

| Step | Model | Java Encoder responsibility |
| --- | --- | --- |
| 1. Build | Astra (`gpt-6-astra`, through Codex) | Implements one scoped issue or fix on a clean branch based on `main`, reads the context and compatibility contracts, adds regression coverage, and prepares the change for review. |
| 2. Second build | Opus 5.5 (`claude-opus-5-5`, through Claude Code) | Checks the implementation and tests, corrects gaps within scope, and simplifies code without weakening parser protection or changing contracts accidentally. |
| 3. Independent review | Sol 6.1 (`gpt-6.1-sol`, through Codex) | Reviews Java correctness, streaming behavior, exact output, public API and packaged-consumer compatibility. |
| 3. Independent review | Daybreak Blue access program, when available | Uses an eligible current model to examine parser-boundary attacks, context confusion and resource exhaustion. Record both the resolved model and access program; ordinary model access does not establish program eligibility. |
| 3. Independent review | Mythos 5.1 (`claude-mythos-5-1`, through Claude Code) | Challenges security assumptions with primary specifications and adversarial parser tests, including trusted/untrusted fragment composition. |
| 3. Independent review | Sonnet 5.5 (`claude-sonnet-5-5`, through Claude Code) | Checks Javadocs, examples and migration notes for accurate context selection, complete surrounding syntax, concision and plain US English. |
| 4. Resolve and deliver | Astra (`gpt-6-astra`, through Codex) | Resolves findings with evidence, validates the final revision, and reports the verdict, remaining blockers and results to the operator. |

Use an eligible current model for the Daybreak slot; do not substitute a
deprecated alias or describe an unresolved alias as a verified model identity.
An unavailable reviewer is a disclosed coverage gap, not a passing review.

## Repository and security contracts

Read the changed implementation and its tests alongside the
[context guide](docs/contexts.md), [usage examples](docs/usage.md),
[compatibility decisions](docs/compatibility-decisions.md), and
[runtime matrix](compatibility/README.md). Distinguish released behavior from
the current development tree and proposals; a snapshot version does not authorize
a release or a compatibility break.

- **Contextual output encoding is the scope.** It does not sanitize HTML,
  validate URLs, serialize complete JSON documents, parameterize SQL, or
  decode/canonicalize input. Examples must identify the receiving parser,
  include required quotes and trusted delimiters, and encode raw data at the
  output boundary. Check nested contexts in parser order. JavaScript string
  encoding is not JSON encoding or permission to generate executable expressions.
- **Composition is part of security.** Test trusted prefix + encoded input +
  trusted suffix, including empty input and tokens split across boundaries.
  Respect documented preconditions: encoding cannot repair an already unsafe
  trusted prefix, a continued escape sequence, or unsafe trusted syntax.
  Exercise the relevant HTML tokenizer, JavaScript/JSON, XML or CSS parser;
  checking an escaped string or forbidden substring alone is insufficient.
- **Exact output is observable behavior.** Review added and removed escapes,
  null handling, invalid Unicode replacements and normalization. Preserve the
  supported 1.x contracts unless an explicitly reviewed security correction
  requires a change. More escaping is not automatically patch-compatible.
  Record affected contexts, output-size changes and migration needs.
- **Streaming must preserve the contract.** Check String and Writer facades,
  `EncodedWriter`, and low-level buffers where affected. Cover split surrogate
  pairs, lone surrogates, pending input, chunk boundaries, explicit flush/close,
  `endOfInput`, small output buffers and repeated overflow/underflow. Verify
  buffer positions and progress without losing or duplicating characters.
  Preserve stateless, thread-safe encoder singletons; caller-owned buffers and
  writer state must not leak into shared encoders. Compare chunked and whole
  results according to the documented flush exceptions.
- **Resource bounds matter.** Review expansion bounds, integer overflow,
  allocation growth and termination on adversarial input. Keep
  `maxEncodedLength`, fast-path scanning and encoding loops consistent. A
  performance improvement needs measurements and security regression coverage;
  there is no maintained Maven benchmark profile.
- **Packaging is a supported interface.** Preserve Java 8 library APIs/bytecode,
  public signatures, module/bundle identities, dependency scopes and the core's
  lack of runtime dependencies. Keep javax/Jakarta implementations, tags, EL
  functions and packaged TLDs in parity. The two adapters share packages and
  must not be combined on one classpath/module path. Do not revive the retired
  ESAPI adapter or remove retained deprecated 1.x APIs as incidental cleanup.
- **Vulnerability evidence stays private.** Follow [SECURITY.md](SECURITY.md)
  for suspected vulnerabilities. Do not put undisclosed reproductions in public
  issues, PRs, comments or CI logs. Use only authorized tools and destinations
  for sensitive material; never include credentials or signing material in
  model prompts or review records.

## Review contract

Give independent reviewers the same immutable commit SHA, task intent, complete
diff, full changed files, relevant callers/tests and repository guidance. Each
reviewer returns findings before seeing the others' conclusions. Reviewers do
not edit the shared branch; the coordinator applies accepted fixes and sends
affected changes back for review. Earlier reviews do not automatically cover
later edits.

Require a **MERGE**, **REQUEST_CHANGES**, or **CLOSE** recommendation with severity,
exact file/line locations, the failing context or contract, evidence, and a
concrete fix. Separate introduced regressions from inherited issues and clearly
label hypotheses. A MERGE recommendation is advisory, not permission to merge.

For security claims, read the primary specification or implementation and verify
that it supports the claim. Identify the parser and version used by a test and
what the result proves. Add a regression that fails for the original defect and
passes with the fix; avoid tests that merely restate the implementation. Model
agreement, high coverage and green CI do not establish security correctness.

Keep documentation examples small but complete enough to show safe placement.
Check local links and anchors after edits. For substantial prose rewrites,
report word and code-block line counts before/after, without treating shorter
text as more important than a necessary security precondition.

## Validation

Use JDK 17 and the committed Maven wrapper; libraries still target Java 8.
Python repository checks require Python 3.8+. Read [BUILDING.md](BUILDING.md)
for source policy, coverage and toolchain details. For code changes, run the
relevant focused checks during development and the clean reactor after the last
change:

```sh
./mvnw -B -ntp clean verify
python3 scripts/check-taglib-parity.py
python3 -m unittest discover -s scripts/tests
python3 -m unittest discover -s compatibility/tests
```

For packaged compatibility validation, run after a successful reactor verify,
with an empty `target/compatibility` and `JAVA_HOME` pointing to the runtime
being tested:

```sh
python3 compatibility/consumers.py prepare
python3 compatibility/consumers.py run --runtime 17 --java-home "$JAVA_HOME"
```

The browser/WAR fixture requires Docker and remains required in CI even when
it cannot run locally:

```sh
./mvnw -B -ntp clean verify -PtestJakarta
```

`clean` removes prepared consumer artifacts; prepare them again if needed after
another clean build. Release-baseline tests require reachable release tags;
fetch the necessary history for shallow clones. Never bypass a failed check by
lowering coverage floors, deleting baseline tests or skipping security controls.

For documentation-only changes, verify claims against code and repository
contracts, check links and run `git diff --check`; explain when runtime tests
were unnecessary. For build, dependency, workflow or Python changes, also run
the affected policy/tool tests and follow [CI/security operations](.github/CI_SECURITY.md)
and [dependency decisions](.github/DEPENDENCY_DECISIONS.md).

Before merging, inspect final-commit results for `Java CI gate`,
`Packaged consumer gate`, and the required Java, Actions and Python CodeQL checks.
The consumer matrix includes Java 8/11/17/21/25, classpath, JPMS and OSGi coverage;
a local Java 17 run does not substitute for that matrix. Report failed,
unavailable and unrun checks explicitly. Do not claim local checks satisfy
release gates in [RELEASING.md](RELEASING.md).

## Decide and deliver

- Keep one topic per branch/PR and preserve existing license notices. Update
  Javadocs, context/usage guidance and the Unreleased changelog when a delivered
  behavior change warrants it; proposals are not shipped features.
- Record actual reviewer models/tools, reviewed SHA, findings, accepted fixes
  and reasons for rejecting findings. Resolve disagreements through code,
  specifications and reproducible tests. Disclose material AI assistance in the
  PR description without copying private evidence or secrets.
- Use the existing [PR template](.github/PULL_REQUEST_TEMPLATE.md): explain the
  problem, resulting behavior, compatibility impact, validation and limitations.
  Follow the repository’s documented review and CI controls.
- Confirm current required checks and human approvals on the final PR revision.
  AI reviews do not count as independent maintainer approval. Do not weaken
  rulesets or CI to deliver a change. Posting reviews, merging, closing, signing
  or publishing requires authorization for that action.
- Give the operator the verdict, unresolved findings, compatibility/migration
  consequences and validation results. Release signing, tags and publication
  follow [RELEASING.md](RELEASING.md) and [MAINTAINERS.md](MAINTAINERS.md).

# Model-assisted development and review

Use this workflow to develop and review changes to OWASP Java Encoder.
Model-assisted review supplements independent maintainer review.
Use the full workflow for substantial encoding/security changes and focused
review for small corrections. Follow [CONTRIBUTING.md](CONTRIBUTING.md) and the
existing repository controls; this document does not authorize publication,
merging, releases, or changes to those controls.

## Lineup

Use these exact model IDs and CLI routes. This is the repository's selected
lineup, not a request to discover the latest models. Start with the local
commands below; do not repeat broad web searches to map model names to CLIs.
Check installed CLI versions and actual authenticated execution. Consult the
linked official documentation only when a command, access failure, deprecation,
or requested lineup change needs investigation. Record requested IDs, identities
reported by the tools, and tool versions separately. CLI acceptance of a name
does not establish that a review ran.

| Step | Model | Java Encoder responsibility |
| --- | --- | --- |
| 1. Build | Astra (`gpt-6-astra`, through Codex) | Implements one scoped issue or fix on a clean branch based on `main`, reads the context and compatibility contracts, adds regression coverage, and prepares the change for review. |
| 2. Second build | Opus 5.5 (`claude-opus-5-5`, through Claude Code) | Checks the implementation and tests, corrects gaps within scope, and simplifies code without weakening parser protection or changing contracts accidentally. |
| 3. Independent review | Sol 6.1 (`gpt-6.1-sol`, through Codex) | Reviews Java correctness, streaming behavior, exact output, public API and packaged-consumer compatibility. |
| 3. Independent review | Daybreak Blue (the Codex model-picker entry; `gpt-daybreak-blue-latest`) | Examines parser-boundary attacks, context confusion and resource exhaustion. Explicitly select Blue access and run the preflight below. Record the requested alias, any returned model identity, and access program separately. |
| 3. Independent review | Mythos 5.1 (`claude-mythos-5-1`, through Claude Code) | Challenges security assumptions with primary specifications and adversarial parser tests, including trusted/untrusted fragment composition. |
| 3. Independent review | Sonnet 5.5 (`claude-sonnet-5-5`, through Claude Code) | Checks Javadocs, examples and migration notes for accurate context selection, complete surrounding syntax, concision and plain US English. |
| 4. Resolve and deliver | Astra (`gpt-6-astra`, through Codex) | Resolves findings with evidence, validates the final revision, and reports the verdict, remaining blockers and results to the operator. |

### Start here: scope and orchestration

The coordinating Codex agent owns the task and accepted edits. This workflow
explicitly permits independent model reviews in parallel. Use the installed
`codex` executable for OpenAI models and `claude` for Anthropic models; a Codex
subagent cannot select a Claude model. Do not recursively launch this lineup
from each reviewer.

For implementation, run Astra's build and Opus's second build sequentially,
then freeze the revision for independent reviews. Use an isolated branch/checkout
when the user's working tree has unrelated changes. Build agents may edit only
their assigned checkout under the session's existing permissions and repository
controls. Never bypass approval or sandbox controls for either CLI. The commands
below are for reviews, not for granting build agents write access.

For an issue or proposed behavior review, skip the implementation steps and
give the reviewers the same current SHA, proposal, and relevant source. State
that no implementation diff exists. Opus can review feasibility and test gaps
without making a second build. Do not create a fix, publish a comment, or promise
a release merely because a review was requested.

1. Read this file and the repository guidance; record `git status --short` and
   `git rev-parse HEAD`. Preserve the user's uncommitted work.
2. Prepare one immutable review packet outside the working tree. Include the
   reviewed SHA, task intent, complete diff (or explicit proposal), full relevant
   files with line numbers, callers/tests, repository guidance, and relevant
   primary-specification excerpts with retrieval dates and source links. Retain
   the packet and its SHA-256. If reviewing uncommitted changes, identify the SHA
   as the baseline and hash the captured diff too; do not call it a committed
   revision. Do not include credentials or unrelated private files.
3. Record `codex --version` and `claude --version`. Check Codex's authenticated
   catalog once per review session using the `model/list` procedure below.
   Reuse that response for all Codex slots. Perform the Blue preflight once before
   its actual review. For Claude, a successful review result is also the access
   check; a separate greeting probe is unnecessary.
4. Run independent reviews against that same packet. Keep each slot's role
   instruction separate from the shared evidence, then combine them into a
   retained per-slot input file. Require findings before other reviewers'
   conclusions are shared. Tell reviewers not to edit, publish, delegate, or run
   other models. The coordinator runs additional tests or retrieves specifications
   requested by any reviewer, then supplies that evidence for follow-up.
5. Retain stdout, stderr, exit status, reported identity, and verdict per slot.
   Resolve findings against code, specifications, and tests. Send material fixes
   back to affected reviewers at the new SHA.
6. Deliver the verdict and evidence with any unfilled slots and unrun checks.
   A review after a release is supplemental, never a retroactive release gate.

### CLI recipes

These routes were checked with **Codex CLI 0.163.0** and **Claude Code 2.1.304**
on **2026-10-20**. Recheck local `--help` when the installed version changes or
a flag fails; do not silently change models. The commands assume absolute
`review_dir`, `review_packet`, and `review_role_file` paths. The packet contains
shared evidence; the role file contains the slot's instructions. Use a fresh
directory and one output basename per run, including follow-up reviews.

```sh
codex --version
claude --version
mkdir -p "$review_dir/empty"
```

After selecting `review_model` and its role file, prepare the per-slot input:

```sh
review_model=gpt-6.1-sol
review_input="$review_dir/$review_model.input.txt"
cat "$review_role_file" "$review_packet" > "$review_input"
shasum -a 256 "$review_packet" "$review_input"
```

Use `gpt-6.1-sol` for independent review or `gpt-6-astra` for proposal analysis
and resolution. Feed the input through stdin rather than embedding it in a shell
argument. The final report and JSONL event log are separate artifacts:

```sh
codex exec --ignore-user-config --ephemeral --skip-git-repo-check \
  -C "$review_dir/empty" -s read-only \
  -m "$review_model" -c 'model_reasoning_effort="high"' \
  --json -o "$review_dir/$review_model.txt" \
  < "$review_input" > "$review_dir/$review_model.jsonl" \
  2> "$review_dir/$review_model.stderr"
review_status=$?
printf '%s\n' "$review_status" > "$review_dir/$review_model.exit"
```

For Claude, use exactly `claude-opus-5-5`, `claude-mythos-5-1`, or
`claude-sonnet-5-5`. This packet-only recipe disables tools and MCP servers;
provide source/specification/test evidence in the packet. Select the Claude ID
and prepare its per-slot input as above before running:

```sh
(
cd "$review_dir/empty" || exit 1
claude -p --model "$review_model" --effort high \
  --output-format json --tools "" \
  --strict-mcp-config --mcp-config '{"mcpServers":{}}' \
  --disable-slash-commands --no-session-persistence \
  < "$review_input" > "$review_dir/$review_model.json" \
  2> "$review_dir/$review_model.stderr"
review_status=$?
printf '%s\n' "$review_status" > "$review_dir/$review_model.exit"
exit "$review_status"
)
```

Tool disabling is not full process isolation: installed settings, hooks and
managed policies can still apply. Preserve required controls and record any
behavior that adds context or affects the review. Do not use `--bare` as an isolation
shortcut: in the verified version it disables OAuth/keychain authentication.
Do not add `--fallback-model` or permission-bypass flags. `Opus 5.5 Cyber` is
not a separate model ID in this lineup; use `claude-opus-5-5` and report any
provider access program only if the tool actually attests it.

For every command, capture the exit code immediately. For Codex require a
completed turn and a substantive final verdict; an empty output file or a
`turn.failed` event is not a review. For Claude inspect `is_error`, `result`,
and the `modelUsage` keys in the JSON response. Record the identity actually
reported, including any unexpected model. A model's own claim about its name
is not identity evidence. Codex CLI 0.163.0's observed JSONL turns did not expose
an underlying backend model identity: retain the catalog match and requested ID,
and record the backend as unreported. Do not invent an identity field. If a tool
reports a different primary reviewer, mark the requested slot unfilled and
describe the actual run; record auxiliary model usage separately. Do not claim
tests ran based on an analysis-only response. No automatic model substitution
is allowed.

Official references for troubleshooting:
[Codex CLI](https://developers.openai.com/codex/cli/reference),
[Codex app-server](https://developers.openai.com/codex/app-server),
[Astra](https://developers.openai.com/api/docs/models/gpt-6-astra),
[Sol 6.1](https://developers.openai.com/api/docs/models/gpt-6.1-sol),
[Claude models](https://platform.claude.com/docs/en/models/overview),
[Mythos 5.1](https://platform.claude.com/docs/en/models/mythos-5-1/overview),
and [Claude CLI](https://code.claude.com/docs/en/cli-reference).

### Daybreak Blue preflight

This slot means the **Daybreak Blue entry in the Codex model picker**. Do not
silently replace it with GPT-6 Sol plus Blue access: those are different model
selections. The [API model catalog](https://developers.openai.com/api/docs/models/gpt-daybreak-blue-latest)
marks the alias deprecated, but this workflow selects the installed Codex picker
entry with Blue access. When the user invokes this workflow, attempt this slot
and report any failure; do not treat it as an optional substitute for Sol.
Check its availability for the current account instead of
inferring access from environment variables, configuration keys or API deprecation.

Record `codex --version` and inspect the authenticated Codex app-server
`model/list` result: match the display name, model ID and
`availableAccessPrograms.cyber`. Use `codex app-server --stdio` with newline
delimited JSON-RPC. Send `initialize`, wait for its response, send `initialized`,
then request `model/list`:

```json
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"clientInfo":{"name":"encoder-review","version":"1.0"},"capabilities":{"experimentalApi":true}}}
{"jsonrpc":"2.0","method":"initialized"}
{"jsonrpc":"2.0","id":2,"method":"model/list","params":{"includeHidden":true}}
```

Retain the catalog response and follow `nextCursor` if it is non-null. Match
`displayName: "Daybreak Blue"`, `model: "gpt-daybreak-blue-latest"` and cyber
access `daybreakBlue`. The CLI spelling is `daybreak_blue`; the app-server
spelling is `daybreakBlue`. Close the catalog process after collecting results.
Do not dump auth files or infer access from environment variables.

Then use an empty temporary directory for this harmless probe
(replace `/path/to/empty-directory`):

```sh
codex exec --ignore-user-config --ephemeral --skip-git-repo-check \
  -C /path/to/empty-directory -s read-only \
  -m gpt-daybreak-blue-latest --cyber-access-program daybreak_blue \
  --json 'Reply exactly READY. Do not use tools or read files.'
```

Require a completed turn with `READY`, not just CLI acceptance of the name.
For the actual review, use the same explicit model and program with the immutable
review packet and a Blue role instruction; a preflight is not a code review.
Prepare `review_input` for this slot as above:

```sh
codex exec --ignore-user-config --ephemeral --skip-git-repo-check \
  -C "$review_dir/empty" -s read-only \
  -m gpt-daybreak-blue-latest --cyber-access-program daybreak_blue \
  -c 'model_reasoning_effort="high"' \
  --json -o "$review_dir/daybreak-blue.txt" \
  < "$review_input" > "$review_dir/daybreak-blue.jsonl" \
  2> "$review_dir/daybreak-blue.stderr"
review_status=$?
printf '%s\n' "$review_status" > "$review_dir/daybreak-blue.exit"
```

Keep repository
review and approval controls in force. Do not enable model/provider fallback.
If using app-server directly, set `allowProviderModelFallback=false` on
`thread/start` and `cyberAccessProgram=daybreakBlue` on `turn/start`.

Retain the command, tool version, dated catalog/turn evidence, reviewed SHA and
verdict. Record an alias as an alias if the tool does not expose its underlying
model; neither a model's self-description nor requested configuration attests
that identity. Never claim an unreported backend model was verified.

Distinguish **not tested**, **probe failed** (record the actual error),
**access verified**, and **review completed**. Missing local configuration is
not evidence of denied access. On a failure, check the authenticated account,
current catalog and [provider access guidance](https://developers.openai.com/api/docs/guides/daybreak).
Do not change account entitlements or silently substitute another model. Report
an unfilled slot as a coverage gap, not a passing review.

On 2026-10-18, Codex 0.163.0 advertised the exact picker entry with Blue access,
and its explicit probe completed successfully. The catalog and explicit probe
were verified again on 2026-10-20 with the same CLI version. The earlier #243 review had
not attempted that probe; its unavailable label did not establish an access
failure. This dated result does not guarantee future access. Any review added
after publication must be labeled supplemental, not retroactively counted as a
pre-release gate.

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
label hypotheses. All model recommendations, including Astra's final synthesis,
are advisory; none authorize merging, closing, or publication.

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

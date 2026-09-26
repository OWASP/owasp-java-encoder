# Compatibility and scope decisions — 2026-09-26

This record resolves the decision queue in [#142](https://github.com/OWASP/owasp-java-encoder/issues/142)
and [#149](https://github.com/OWASP/owasp-java-encoder/issues/149). It approves no
breaking implementation and promises no 2.0 release or removal date. Existing
closed/not-planned proposals are not reopened. The full release gate in
[RELEASING.md](../RELEASING.md) remains separate.

## Compatibility decisions (#142)

| Item | Decision | Evidence, rationale and migration cost |
| --- | --- | --- |
| Java runtime baseline | **Keep Java 8 for 1.x; defer a future-major increase.** | Original JAR consumers pass on 8/11/17/21/25, with a separate forked Java 8 unit job. Build JDK 17 is a different requirement. A baseline increase would exclude Java 8 deployments; no adoption census or accepted minimum for a future major exists. |
| Explicit versus automatic module names | **Keep both published identities; defer unification to a separate future-major proposal.** | Explicit names such as `owasp.encoder` and fallback names such as `org.owasp.encoder` are independently tested on the same original JARs. Renaming would break `requires`, launcher/module-path configuration and downstream packaging for little immediate benefit. |
| Jakarta tag package and split packages | **Keep `org.owasp.encoder.tag` in both separate adapters for 1.x; defer a rename.** | Both namespaces pass packaged JSP engines and parity checks. They cannot coexist on one classpath/module path. A rename could allow coexistence but would break direct class imports, reflective/TLD references and OSGi package wiring; it needs an explicit migration design before acceptance. |
| Deprecated `forUri` entry points | **Keep through all 1.x; defer any removal.** | `Encode.forUri` was already deprecated; 1.5 extends notices/annotations to registry and tags. Existing call sites retain behavior. Migrate raw components to `forUriComponent`, validate complete URLs, then encode the enclosing context. Removal would break source, binary and view-template callers and has no approved date. |
| Legacy javax JSP adapter | **Keep in 1.x; defer end-of-life or removal.** | Maintained javax Jasper fixtures and original-JAR consumers exercise it. Removing the artifact would strand applications whose container/framework migration is independent of this library. Passing fixtures are evidence of functionality, not a count of downstream users or a promise to support every old container. |
| ESAPI adapter and dependency scope | **Keep the adapter and compile-scoped ESAPI dependency in 1.x; defer a scope/lifecycle change.** | Public APIs expose ESAPI types; its supported-version matrix and packaged consumers pass. Making ESAPI optional/provided would move dependency management to callers and could break compilation or runtime linkage. Upstream security support/advisories remain a separate review, not a blanket exemption. |
| Multi-release JARs versus one classfile level | **Keep Java 8 classes plus Java 9 descriptors in 1.x; defer single-release packaging.** | Original JARs pass Java 8 and explicit/automatic JPMS tests. A future runtime baseline of 9+ might permit one release level, but that depends on a baseline decision and all packaging/OSGi/JPMS evidence, not just compiler convenience. |
| Exact encoded output and contexts | **Keep explicit versioning/migration review; accept no blanket “more escaping is patch-safe” rule.** | 1.5's ESAPI URL-component change alters delimiters; JavaScript escape additions preserve interpreted strings but change bytes. Output affects snapshots, signatures and protocol consumers. Security patches may correct unsafe behavior with an advisory; other changes need parser tests, release notes and a justified version. |

Evidence: [runtime matrix, package guards and identities](../compatibility/README.md),
[required browser/WAR fixture](../jakarta-test/README.md), [JSP engine checks](../compatibility/jsp-engine/README.md),
[ESAPI contracts](../esapi/README.md), [migration guide](../README.md#migrating-from-foruri)
and [merged-change history](../CHANGELOG.md). These are real consumer fixtures,
not an ecosystem usage census; a few code-search hits cannot establish that a
breaking change has no downstream users.

No breaking proposal above is accepted for implementation. Therefore no speculative
implementation tickets or removals are created. To reconsider an item, maintainers
must review a focused proposal with affected public surfaces, measured consumer
impact, migration examples, version choice and alternatives. An accepted break
must be announced in a published migration/design note and release notes **before**
implementation, with a documented notice period appropriate to the affected
consumers. Deprecation alone is not sufficient notice of a removal date. Create
implementation issues only after that decision, and retain the old 1.x contract.

JSON string-content encoding and ordinary untagged JavaScript template support
are already merged 1.5 features; they are not deferred or rejected by this record.
Tagged templates, arbitrary executable expressions and complete JSON serialization
remain outside those contracts.

## Base64url disposition (#149)

**Reject adding a Base64Url class, `Encode.forBase64Url`, or tag/EL bindings to this
library; document use of the JDK codec instead.** No submitted implementation is
being rejected as vulnerable: the decision is about scope. Transport encoding and
canonical decoding require byte/protocol contracts that do not fit contextual
output encoding. In particular, a decoder broadens the existing non-goals, and
byte grouping does not fit the char-to-char `Encoder`/`EncodedWriter` abstraction.
Encoding-only view tags would add another surface without a demonstrated need;
no decoder tags are accepted. Existing ESAPI delegates stay unchanged.

For a protocol that explicitly requires **unpadded** base64url, use already-defined
bytes with the [JDK 8+ Base64 API](https://docs.oracle.com/javase/8/docs/api/java/util/Base64.html):

```java
byte[] bytes = { 0, 1, 2 }; // protocol-defined bytes
String transport = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
// transport is "AAEC"
```

Choose padding according to the actual protocol. Base64url is not XSS prevention,
input validation, authentication, encryption or token-signature verification. The
application must validate the protocol and still respect the enclosing HTML/URL/
script context. Do not add an Encoder escape call simply because a value is
base64url, or assume decoding verifies the data's origin.

The [JDK decoder](https://docs.oracle.com/javase/8/docs/api/java/util/Base64.Decoder.html)
accepts both correctly padded and unpadded final units; a successful decode is not
proof of a canonical spelling. A protocol requiring canonical data needs its own
reviewed alphabet/whitespace, length, padding and trailing-bit rules and bounded
input/allocation policy. Do not echo secret input in error messages. If text is
part of that protocol, specify strict Unicode behavior: ordinary Java UTF-8
conversion can replace malformed UTF-16/UTF-8 rather than reject it. The byte-only
example above deliberately does not define a String conversion or strict decoder.
Use a protocol-specific library when those rules are security-critical.

This closes the proposal as **not planned**, with a standard-library alternative.
Reconsideration would need a new, narrowly justified product decision and complete
byte/String/null/error/Unicode/canonicalization/resource-limit specification before
public API or view bindings are designed. It is not a postponed 1.5 feature.

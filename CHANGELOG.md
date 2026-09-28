# Changelog

Released entries are grounded in the linked immutable tags, GitHub release notes
and retained README announcements. Dates below are GitHub publication dates in
UTC where a release record exists; older announcement/tag dates are labeled.
An open proposal is not a release. Historical tags, assets and signatures remain
unchanged. [1.5.0 is available from Central](releases/1.5.0-central-publication.md).

## Unreleased

- Resume development at `1.5.1-SNAPSHOT`, with the immutable 1.5.0 public-API
  baseline and its matching support APIs. Minimum-consumer fixtures are unchanged.
- Discover release tags independently of commit ancestry so squash merges cannot
  leave the compatibility baseline stale; add isolated Git regressions.
- Record verified 1.5.0 publication and replace pending-availability notices.

## 1.5.0 — 2026-09-28 UTC

This is a security release for
[GHSA-g8p6-7r8f-qrpv](https://github.com/OWASP/owasp-java-encoder/security/advisories/GHSA-g8p6-7r8f-qrpv).
Signed artifact availability and independent verification are tracked in the
[1.5.0 release record](releases/1.5.0.md).

* build: stop Dependabot from recreating already-reviewed incompatible API,
  servlet-engine and build-tool version proposals. The ignores are limited to
  routine version updates in the rejected SemVer classes; security updates remain
  eligible. Mixed historical/current coordinates require manual version review
  because Dependabot classifies them from their lowest occurrence.
* removed: retire the optional `encoder-esapi` adapter. Version 1.4.1 is its final published release and is no longer supported; no `encoder-esapi:1.5.0` artifact will be published. Consumers must remove the adapter and [migrate Java Encoder-backed calls to the direct context APIs](docs/encoder-esapi-retirement.md). Historical Maven artifacts remain immutable.
* build: remove advisory-affected dependencies from active Maven plugin realms, including the separately invoked compatibility-fixture downloader; invoke the same japicmp engine without its obsolete reporting wrapper; and submit only actually invoked build plugins to GitHub's dependency graph. Shared inherited tooling is recorded once, and no Dependabot alert is dismissed or suppressed.
* build/compatibility: update the published JSP provided API to 2.3.3 and the Jakarta test classpath to Servlet 6.1.0 and EL 6.0.1, while retaining independent JSP 2.2.1 and Java 8-compatible Jakarta minimum-consumer fixtures. Japicmp now resolves distinct old/new support classpaths so the 1.4.1 comparison remains complete. Dependabot scans the root Maven reactor once, rather than opening duplicate module proposals, and continues to scan the standalone compatibility-fixture project separately.
* feat: all four `forJavaScript*` methods encode dollar sign (`$`) as `\x24`, backtick as `\x60`, and opening brace (`{`) as `\x7b` [#129](https://github.com/OWASP/owasp-java-encoder/issues/129). Escaping `{` prevents input after a trusted `$` from completing `${...}`. Encoded output now supports literal text in ordinary (untagged) template literals as well as single- and double-quoted strings. This changes the encoded output while preserving its decoded JavaScript string value. Tagged templates (including `String.raw`), `${...}` expression bodies, JSON, and script URLs are unsupported; each method's HTML context restrictions still apply.
* fix: all four `forJavaScript*` methods escape unpaired UTF-16 surrogates as `\uXXXX`, preserving their JavaScript string values through UTF-8 serialization [#135](https://github.com/OWASP/owasp-java-encoder/issues/135), and escape DEL/C1 controls (U+007F to U+009F) as `\xNN` [#163](https://github.com/OWASP/owasp-java-encoder/issues/163). Valid surrogate pairs and other non-ASCII text remain unescaped except U+2028/U+2029. These are output-fidelity changes; NEL was already ordinary JavaScript string data.
* fix: the HTML/block JavaScript encoders escape every ASCII character that can contribute to a case-insensitive `</script` end tag or the `<!--` and `-->` script tokens, including the HTML end-tag delimiters, so any nonempty encoded substring cannot complete a delimiter supplied partly by adjacent trusted literal text. `forCDATA` represents every `]` and `>` with close/reopen sequences, preserving parsed text while breaking every nonempty encoded substring of `]]>`; its String facade grows with actual output instead of eagerly reserving the 13× maximum. `forXmlComment` replaces every hyphen with `~`. These are substantial compatibility-visible output changes; see the [migration record](docs/compatibility-decisions.md#15-parser-boundary-output-migration).
* fix: `EncodedWriter` now enforces Writer lifecycle semantics: write, append and flush operations fail after close; repeated close is harmless; the first close finalizes pending input and still attempts the delegate close, preserving simultaneous failures with suppressed exceptions. Array-slice writes now use overflow-safe bounds validation, including `Integer.MAX_VALUE`-shaped ranges.
* build: compare all three 1.5.0 artifacts against the immutable 1.4.1 public-API baseline, and verify that every publishable effective POM inherits repository-root SCM connection, developer connection and URL values without module-name suffixes.
* feat: add `Encode.forJson` String/Writer methods, the `json` encoder context, and `forJson` tags and EL functions in both JSP and Jakarta tag libraries [#145](https://github.com/OWASP/owasp-java-encoder/issues/145). The caller supplies double quotes. Output uses RFC 8259 string escapes and also escapes HTML script delimiters. Java `null` becomes the text `null` (the JSON string `"null"` when quoted); unpaired surrogates use Unicode escapes and may not interoperate with every JSON consumer. Prefer a serializer for complete JSON documents.
* feat: add `forXml11`, `forXml11Content` and `forXml11Attribute` tags and EL functions to the advanced JSP and Jakarta taglibs, and `forXml11` to the basic taglibs [#131](https://github.com/OWASP/owasp-java-encoder/issues/131).
* deprecation: `Encoders.URI` and both `ForUriTag` classes are now deprecated like `Encode.forUri`, whose Javadoc now says what to use instead; the `forUri` TLD descriptions warn about double encoding, the adapter builds show deprecation call sites, and the README has a [forUri migration section](README.md#migrating-from-foruri) [#130](https://github.com/OWASP/owasp-java-encoder/issues/130).
* fix: the JSP and Jakarta bundles now declare the core version they need (`[1.5,2)`, because the tags call `Encode.forJson`) and the JSP API ranges they support, instead of unversioned imports that could wire to an older core and fail when a tag ran. Bundle symbolic names are now declared explicitly and unchanged [#137](https://github.com/OWASP/owasp-java-encoder/issues/137).
* fix: `forHtmlUnquotedAttribute` now replaces U+0085 (NEL) with a hyphen like the other C1 control characters, instead of emitting `&#133;`, which HTML5 parsers decode as U+2026 [#136](https://github.com/OWASP/owasp-java-encoder/issues/136).
* fix: the XML 1.1 encoders (`forXml11`, `forXml11Content`, `forXml11Attribute`) now encode U+0085 (NEL) as `&#x85;` and U+2028 (line separator) as `&#x2028;`, so they are not normalized to a line feed [#136](https://github.com/OWASP/owasp-java-encoder/issues/136).
* maintenance: clarify output-context contracts and expand XML 1.1 tests, fix clean reactor compilation, and remove the obsolete benchmark profile.

### Build, compatibility and maintenance

- Preserve original-JAR consumers on Java 8/11/17/21/25, explicit/automatic JPMS
  and Felix R6/R8; add final TLD-surface/Writer contract checks (#162, #167).
- Test packaged javax/Jakarta TLDs through isolated Tomcat/Jasper engines; retain
  required real-browser and executable-WAR checks with the modernized optional
  Boot 4.1.1 fixture (#179, #180).
- Pin and guard Actions, add CodeQL/dependency submissions/Dependabot, isolate
  Maven caches, and preserve required CI/security gates (#173, #177).
- Include Java 9 descriptors in source attachments; normalize source metadata
  and retain attribution (#184).
- Retire the dormant Maven Site/OSS parent, adopt verified Maven 3.9.16 wrapper
  and JDK 17 build policy, Checkstyle and measured unit coverage floors; isolate
  signing/publishing tools, verify local bundles and measure reproducibility
  (#185, #187). This does not change the Java 8 library runtime baseline.
- Add release verification, historical key evidence, and maintainer custody
  guidance (#164, #171, #185). Historical signing-key
  authorization records (#110) now distinguish retrospective maintainer
  authentication from historical GitHub/project records; see the
  [key verification record](releases/historical-key-authentication.md).
  Central publication and the reported completion of maintainer access/custody
  work (#111) are recorded in the [publication follow-up](releases/1.4.1-central-publication.md).

The [maintenance tracker](https://github.com/OWASP/owasp-java-encoder/issues/169)
and [closeout record](releases/maintenance-closeout.md) record the corresponding
PRs, tests and dispositions; they are not approval to publish 1.5.

## 1.4.1 — 2026-09-26 UTC

[Signed GitHub release](https://github.com/OWASP/owasp-java-encoder/releases/tag/v1.4.1)
([tag created 2026-09-25 in America/Los_Angeles](https://github.com/OWASP/owasp-java-encoder/tree/v1.4.1)).
**Available from Maven Central. Upgrade all four Java Encoder artifacts;
versions through 1.4.0 are affected.**
Central publication was verified on 2026-09-27 UTC (2026-09-26 in
America/Los_Angeles); [all artifacts and signatures match the retained release](releases/1.4.1-central-publication.md).

- Fix `EncodedWriter` context corruption during buffer overflow
  ([GHSA-57jg-769q-93vh](https://github.com/OWASP/owasp-java-encoder/security/advisories/GHSA-57jg-769q-93vh)).
- Fix insufficient-lookahead infinite loops in `EncodedWriter`
  ([GHSA-q6jj-5396-8mq2](https://github.com/OWASP/owasp-java-encoder/security/advisories/GHSA-q6jj-5396-8mq2)).
- Fix CSS String API maximum-output sizing for long U+2028/U+2029 runs
  ([GHSA-p9ff-j89j-9xhx](https://github.com/OWASP/owasp-java-encoder/security/advisories/GHSA-p9ff-j89j-9xhx)).
- Preserve Java 8 runtime, public method signatures, Maven and JPMS identities;
  make adapter API dependencies transitively readable in module descriptors.
- Pin the ESAPI adapter's default to 2.7.0.0 rather than a Maven version range.

See the [full release record](releases/1.4.1.md) for affected entry points,
coordinates, verification and publication status. Later 1.5 changes do not alter
these retained artifacts.

## 1.4.0 — 2025-11-17

[Release and tag](https://github.com/OWASP/owasp-java-encoder/releases/tag/v1.4.0).
Add XML 1.1 core APIs (#88); update tests and security documentation (#83, #86, #87).
The ESAPI POM uses `[2.5.1.0,3)`; a temporary dependency pin controls resolution but
**does not fix Java Encoder's security issues**. See the
[historical adapter guidance from the reviewed 1.5 candidate](https://github.com/OWASP/owasp-java-encoder/blob/07d8e58f6b9fd834089dc86895e01a1be018cbc0/esapi/README.md#temporary-esapi-pin-for-140-consumers).

## 1.3.1 — 2024-08-20

[Release and tag](https://github.com/OWASP/owasp-java-encoder/releases/tag/v1.3.1).
Fix Java 8 `NoSuchMethodError` (#80) and add OSGi manifest entries (#82).

## 1.3.0 — 2024-08-02

[Release and tag](https://github.com/OWASP/owasp-java-encoder/releases/tag/v1.3.0).
Raise the library minimum to Java 8; build with JDK 17 for test dependencies.
Add historical automatic module names (#45), explicit Java 9 module descriptors
via multi-release JARs (#77), and a separate Jakarta Servlet 5 adapter (#75).
Update the ESAPI adapter (#76) and Javadocs. Javax and Jakarta remain separate
artifacts with the same tag implementation package.

## 1.2.3 — 2020-11-08

[Release and tag](https://github.com/OWASP/owasp-java-encoder/releases/tag/v1.2.3).
Make the manifest OSGi-compliant (#39) and support ESAPI 2.2+ (#37).
This and earlier libraries retained the Java 5–7 runtime baseline, unlike 1.3+.

## 1.2.2 — 2018

[Tag](https://github.com/OWASP/owasp-java-encoder/tree/v1.2.2) created 2018-09-03;
the retained README announcement is dated 2018-09-14. Documentation and licensing
fixes. No separate GitHub release record exists at the 2026-09-26 inventory.

## 1.2.1 — 2017-02-19 (tag/announcement)

[Tag](https://github.com/OWASP/owasp-java-encoder/tree/v1.2.1).
CDATA output no longer emits intermediate characters between adjacent CDATA
sections; improve the Pages documentation. No separate GitHub release record
exists at the inventory date.

## 1.2 — 2015-04-12 (tag/announcement)

[Tag](https://github.com/OWASP/owasp-java-encoder/tree/v1.2).
Move the project to GitHub; the retained wiki also records removal of ThreadLocal
use from development in February 2015. No separate GitHub release record exists
at the inventory date.

## Earlier announcements

The [retained README](https://github.com/OWASP/owasp-java-encoder/blob/3bd86250a9c9cd48577c3bf7fb9c46dbe91c1c90/README.md#news)
records 1.1.1 on 2014-01-30 (bug fix and ESAPI integration) and 1.1 on 2013-02-14
(encoding refinements and JSP tags/functions). These are announcement dates,
not independently established release timestamps; the old wiki dates the 1.1.1
announcement differently. No corresponding GitHub tags/releases are present.
The March 2014 documentation update is not a software release. Original Central
artifacts and observed signatures are catalogued separately in [VERIFYING.md](VERIFYING.md).

## Historical GitHub metadata disposition

Checked 2026-09-26: GitHub releases exist for 1.2.3 through 1.4.1; titles vary
between `Version 1.2.3` and `v...`, and 1.4.1 identifies itself as a security
release. Keep those titles, dates and original text: they are unambiguous, and
backfilling older entries would require inventing publication timestamps. This
changelog supplies consistent navigation without rewriting history. Preserve
1.4.0's dated upgrade supplement and 1.4.1's security notice. The 1.4.1
pending-publication notice was replaced after Central publication and exact
artifact verification on 2026-09-27 UTC.

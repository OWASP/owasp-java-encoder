# Output contexts and boundaries

This guide describes the prepared **1.6.0** release, including the JavaScript
refinement for #243. Published 1.5.0 and 1.5.1 retain their broader letter escapes.
See the [1.6.0 release record](../releases/1.6.0.md) for publication status.
The [Encode Javadoc source](../core/src/main/java/org/owasp/encoder/Encode.java) is
the detailed per-method contract; each method has a String-returning and a
`(Writer out, String input)` overload. `Encoders` exposes shared stateless encoders;
`EncodedWriter` supports chunked input and must be closed to finish pending input.
Use the facade Writer overload when encoding one String directly.

## Encode for the parser that receives the value

- HTML text: `forHtmlContent`; ordinary quoted text attributes:
  `forHtmlAttribute`. `forHtml` covers both with additional escapes.
  Always quote attributes. `forHtmlUnquotedAttribute` exists for legacy use;
  prefer fixing the template to quote its value. None of these validate attribute
  names, event-handler code or URL schemes. XML comment escaping is not HTML
  comment escaping; do not insert untrusted data into HTML comments.
- JavaScript: supply the string delimiters yourself. `forJavaScript` covers
  string data in script blocks, script attributes and JavaScript source.
  `forJavaScriptBlock` is not for HTML attributes; `forJavaScriptAttribute` targets
  quoted event-handler attributes; `forJavaScriptSource` is for a standalone
  JavaScript resource, not HTML. Avoid dynamic event handlers when possible.
  Never put the result in an unquoted expression, identifier, `${...}` expression
  body, regular expression, or `javascript:`/`data:` URL.
  Insert complete encoded results at a JavaScript literal-text boundary, never
  where they can continue or extend an escape sequence. This includes an
  unmatched backslash, an incomplete hex escape, or `\0` before an input digit. Quote
  HTML event-attribute values as well as the JavaScript strings inside them.
  An event-attribute insertion must not continue an HTML character reference
  begun by trusted text: `&` + encoded `#39;` can become a quote before
  JavaScript parses it. Encode the complete string value, including trusted
  string text, when possible.
- CSS: `forCssString` encodes data **inside a quoted CSS string**.
  `forCssUrl` encodes a URL value inside CSS `url(...)`; allow-list its scheme
  and destination first. Neither accepts arbitrary selectors, property names,
  numeric values or declarations. Prefer a separate stylesheet or safe DOM API;
  a CSS fragment placed in an HTML attribute must also satisfy that attribute's
  parsing context. Do not copy the retired wiki's unquoted `width` example.
- XML: select XML 1.0 (`forXml`, `forXmlContent`, `forXmlAttribute`) or XML 1.1
  (`forXml11`, `forXml11Content`, `forXml11Attribute`, core since **1.4.0**)
  deliberately. XML 1.1 requires an XML 1.1 document declaration and compatible
  parser. Some XML 1.1 control-character references (such as `&#x01;`) are invalid
  in XML 1.0; do not use XML encoders for HTML.
  `forCDATA` and `forXmlComment` implement XML 1.0 contexts; supply their delimiters.
  To remain safe next to trusted literal text, CDATA represents every `]` and
  `>` with close/reopen sequences, and XML comments replace every input hyphen
  with `~`.
  Invalid-character replacement and XML line-ending normalization can change data.
- Java source: `forJava` encodes string-literal content for a code generator,
  not JavaScript or JSON. Supply Java quotes; malformed surrogate input is not
  guaranteed to produce compilable source. This API has no JSP tag/function.

Encode raw values once at each actual parser boundary, from the inner language
to the enclosing one. Do not apply an arbitrary chain of encoders, or run an
HTML autoescaping template over an already HTML-encoded value. If the application
needs user-authored markup, choose a policy-based HTML sanitizer; output encoding
displays markup as text. Follow the [OWASP XSS Prevention Cheat Sheet][xss].

## JavaScript templates — new in 1.5

All four `forJavaScript*` methods now escape backticks, dollar signs and opening
braces. This protects literal text in **ordinary, untagged** template literals,
including data immediately after a trusted `$`. JavaScript string values remain
the same even though the emitted escape spelling changes. The same methods
continue to support single- and double-quoted strings in their documented contexts.

This does **not** support tagged templates such as `String.raw`, or insertion
inside a `${...}` expression. Tagged templates can observe raw escape text.
Do not use a 1.4.1 JavaScript encoder for template-literal text: this support is
introduced in 1.5.0. The old IE grave-accent/`innerHTML` workaround is a separate
historical browser issue, not a substitute for this contract. The
[wiki archive](archive/wiki-2019/README.md) records why that advice was retired.

In 1.5, DEL/C1 controls use hex escapes and unpaired UTF-16 surrogates use Unicode
escapes, preserving JavaScript string values through UTF-8 output. Valid surrogate
pairs remain intact. This does not promise that every downstream system accepts
unpaired surrogates.

The general and block encoders prevent a nonempty encoded value from completing
recognized `</script` or `<script` tokens (case-insensitive, followed by an HTML
name delimiter), `<!--`, or `-->` with adjacent trusted literal text. They escape
space, `!`, hyphen (`\x2d`), `<`, `>`, slash, and relevant controls throughout
the input. In 1.6.0, letters from `script` (either case) are escaped
only when they are the final input character. Interior letters remain readable;
a longer name such as `</scripture>` is not a script end tag. This preserves
HTML tokenizer containment while allowing harmless raw `</script` substrings
inside longer names. It refines the stronger substring prohibition documented
for 1.5.0 and 1.5.1.

Escaping `<` prevents an HTML script delimiter when the entire string value is
encoded, alongside the required JavaScript string escapes. It does not protect
fragments when trusted text supplies that character: `</scr` + encoded `i` +
`pt>` still needs the letter escape. This is unrelated to whitespace inside a
tag name. The terminal-letter rule is conservative: both `script` and `csript`
end with `t`, so both receive a final `\x74` escape even though `csript` is not
a script tag name.

JavaScript interprets the escapes as the original string value, but encoded bytes,
snapshots, signatures and cache keys change. `forJavaScriptAttribute` and
`forJavaScriptSource` do not apply the HTML raw-text rule because their output is
not for an HTML script element. A streaming encoder may retain a final script
letter until more input or end-of-input. `EncodedWriter.flush()` safely emits a
pending script letter as a hex escape or a pending high surrogate as a Unicode
escape. A later low surrogate is escaped separately. Explicit flushes can add
escapes without changing the JavaScript UTF-16 value. Other encoding contexts
retain their existing incomplete-input handling.
In general/block JavaScript modes, flush the `EncodedWriter` before writing
directly to its wrapped writer.
Use the complete-value Writer facade when trusted output must follow.

The opening `<script` token matters after `<!--` has put the HTML tokenizer
in its escaped script-data state. Completing that token can enter the
double-escaped state, causing the intended closing tag and the rest of the page
to be consumed as script text.

This protection assumes the surrounding template keeps the insertion in the
intended JavaScript and HTML contexts. It cannot repair an already recognized
HTML end-tag name followed by whitespace or `/`, such as `</script `, before
the insertion. Empty input also produces empty output: it cannot separate
dangerous trusted text on its two sides. These are requirements on the template,
not additional input characters the encoder can fix.

For example, `forJavaScript("http://google.com")` and `forJavaScriptBlock` produce
`http:\/\/google.com` (19 characters for a 17-character input).
JavaScript reads this as the original URL. `forJavaScriptSource` and
`forJavaScriptAttribute` leave this example unchanged; use them only in their
documented contexts. Source spelling and output size can change substantially
even when the evaluated value is unchanged.

CDATA has the same fragment-composition guarantee for `]]>` and preserves the
XML parser's text value, subject to the existing invalid-character replacement
and line-ending normalization policies. Its output can expand to thirteen
characters for each input `]` or `>` and may be reported as multiple CDATA/text
events. XML-comment encoding is deliberately lossy: every input hyphen becomes
`~`. Trusted text on either side must itself be legal in the selected context;
the guarantee prevents the encoded fragment from completing a parser token.

## JSON string content — new in 1.5

`Encode.forJson` encodes **one JSON string's content**. The caller supplies the
surrounding double quotes. It uses JSON escapes rather than JavaScript `\xNN`
escapes and also escapes HTML script delimiters, allowing the quoted string in an
HTML script data block. A complete JSON document should come from a JSON serializer;
choose one configured for safe HTML embedding if it is put inside a `<script>`
element. Ordinary JSON serialization alone need not protect an HTML end tag.

For HTML embedding, encode the complete contents of each JSON string value and
member name, including its trusted text, before supplying the double quotes.
Do not insert output into an unfinished JSON escape. Unlike the JavaScript
general/block fragment protection, `forJson` does not protect arbitrary fragments
joined to unencoded string text: `</scr` + `forJson("i")` + `pt>` still forms
`</script>`. The rest of the document and template must also be safe for HTML.

Java `null` is encoded as the text `null`: with the caller's quotes this is the
JSON **string** `"null"`, not the JSON null value. The facade generally renders null
as text; JSP EL conversion has its own contract. `forJson` preserves unpaired
surrogates as `\uXXXX`, which not every
JSON consumer interoperates with. JSON strings do not belong in HTML attributes
without the enclosing attribute encoding, and HTML entity escaping is not JSON
serialization.

## URLs and non-goals

Use `forUriComponent` on one raw path segment, query name/value or fragment.
Assemble a URL with trusted delimiters, then validate its scheme and any relevant
host/port/path restrictions. Allow relative URLs only if the application intends
to. Parsing with `URI` is not validation. `.` and `..` need application-specific
path handling; component encoding does not remove them. The result must also be
encoded for a surrounding quoted HTML attribute. See the [URL example](usage.md#urls).

`forUriComponent` uses UTF-8 percent encoding and `%20` spaces. Form encoding
(`java.net.URLEncoder`) has a different contract. Already encoded input is encoded
again; unpaired surrogates are replaced with `-`. Deprecated `forUri` preserves
whole-URI delimiters and cannot make a dangerous scheme safe.

Java Encoder does not perform input validation, HTML sanitization, URL authorization,
SQL parameterization, canonicalization or decoding. Its JSON and JavaScript
support does not change those boundaries. The [OWASP Java library guide][libraries]
describes encoding and sanitization as separate controls; no comparative speed or
security ranking is implied. Base64url is transport encoding; see the
[scope decision and JDK example](compatibility-decisions.md#base64url-disposition-149)
for its separate protocol and canonicalization requirements.

[xss]: https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html
[libraries]: https://devguide.owasp.org/en/05-implementation/03-secure-libraries/04-java-secure-libs/

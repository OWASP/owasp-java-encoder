# Output contexts and boundaries

This guide describes current `main` (unreleased **1.5**); feature introductions
are marked below. For production use, follow the [1.4.1 distribution and security
notice](../README.md). The [Encode Javadoc source](../core/src/main/java/org/owasp/encoder/Encode.java)
is the detailed per-method contract; each method has a String-returning and a
`(Writer out, String input)` overload. `Encoders` exposes shared stateless
encoders; `EncodedWriter` supports chunked input and must be closed to finish
pending input. Use the facade Writer overload when encoding one String directly.

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
unreleased 1.5 behavior. The old IE grave-accent/`innerHTML` workaround is a
separate historical browser issue, not a substitute for this contract. The
[wiki archive](archive/wiki-2019/README.md) records why that advice was retired.

In 1.5, DEL/C1 controls use hex escapes and unpaired UTF-16 surrogates use Unicode
escapes, preserving JavaScript string values through UTF-8 output. Valid surrogate
pairs remain intact. This does not promise that every downstream system accepts
unpaired surrogates.

## JSON string content — new in 1.5

`Encode.forJson` encodes **one JSON string's content**. The caller supplies the
surrounding double quotes. It uses JSON escapes rather than JavaScript `\xNN`
escapes and also escapes HTML script delimiters, allowing the quoted string in an
HTML script data block. A complete JSON document should come from a JSON serializer;
choose one configured for safe HTML embedding if it is put inside a `<script>`
element. Ordinary JSON serialization alone need not protect an HTML end tag.

Java `null` is encoded as the text `null`: with the caller's quotes this is the
JSON **string** `"null"`, not the JSON null value. The facade generally renders null
as text; JSP EL conversion and the ESAPI-delegated JSON method have separate
contracts. `forJson` preserves unpaired surrogates as `\uXXXX`, which not every
JSON consumer interoperates with. JSON strings do not belong in HTML attributes
without the enclosing attribute encoding, and HTML entity escaping is not JSON
serialization. The ESAPI adapter keeps its existing upstream JSON delegation.

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
whole-URI delimiters and cannot make a dangerous scheme safe. Read the
[unreleased ESAPI URL migration](../esapi/README.md#url-encoding-migration-in-15-unreleased).

Java Encoder does not perform input validation, HTML sanitization, URL authorization,
SQL parameterization, canonicalization or decoding. Its JSON and JavaScript
support does not change those boundaries. The [OWASP Java library guide][libraries]
describes encoding and sanitization as separate controls; no comparative speed or
security ranking is implied. Base64url is transport encoding; see the
[scope decision and JDK example](compatibility-decisions.md#base64url-disposition-149)
for its separate protocol and canonicalization requirements.

[xss]: https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html
[libraries]: https://devguide.owasp.org/en/05-implementation/03-secure-libraries/04-java-secure-libs/

# Retirement of `encoder-esapi`

The optional `org.owasp.encoder:encoder-esapi` adapter is retired. Version
**1.4.1 is its final published release**. Starting with Java Encoder 1.5.0, this
repository does not build, test, publish, or support an ESAPI adapter.

The immutable 1.4.1 artifact remains available from Maven Central and its source
remains in the [`v1.4.1` tag][historical-source]. It will not receive the security,
correctness, or output-context fixes made in Java Encoder 1.5.0. In particular,
consumers must not treat the historical adapter as a way to obtain the 1.5 parser-
boundary fixes. Mixing `encoder-esapi:1.4.1` with a newer core is not a supported
migration path.

## Migrating

Remove the `encoder-esapi` dependency and call the context-specific Java Encoder
API directly for output encoding:

| Historical adapter operation | Direct Java Encoder operation |
| --- | --- |
| HTML text | `Encode.forHtmlContent` |
| Quoted HTML attribute | `Encode.forHtmlAttribute` |
| Quoted CSS string | `Encode.forCssString` |
| JavaScript string or ordinary untagged template text | `Encode.forJavaScript` |
| One raw URL component | `Encode.forUriComponent` |

Choose the method from the destination parser context; these are not blanket
byte-for-byte replacements for every historical adapter output. Follow the
[context guide](contexts.md) and [usage examples](usage.md), especially the URL
assembly and validation requirements.

The former adapter also implemented ESAPI operations outside Java Encoder's
scope, including canonicalization, decoding, Base64, SQL, operating-system,
LDAP, DN, XPath, VBScript, and JSON delegation. Applications needing those
operations must select and maintain an appropriate upstream implementation.
Java Encoder does not provide replacements for them and no longer supplies an
`ESAPI.Encoder` implementation. Update any ESAPI configuration that names
`org.owasp.encoder.esapi.ESAPIEncoder` according to ESAPI's own documentation.

## Why it was retired

The adapter exposed another project's API as its public contract and brought that
project's dependency graph into every adapter consumer. The newest stable ESAPI
release still depends on legacy Commons libraries for which no compatible patched
line exists; newer Commons generations use different packages and APIs and cannot
be substituted by changing version numbers. Maintaining a private ESAPI fork is
outside Java Encoder's contextual-output-encoding scope.

This retirement removes only the optional adapter coordinate. Java Encoder 1.5.0
continues to publish and support `encoder`, `encoder-jsp`, and
`encoder-jakarta-jsp`. Previously published Maven coordinates are immutable and
will not be deleted or replaced.

[historical-source]: https://github.com/OWASP/owasp-java-encoder/tree/v1.4.1/esapi

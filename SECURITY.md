# Security Policy

## Supported Versions

Only the latest 1.x release receives security fixes. Fixes ship in a new release;
older release lines are not patched.

| Maven coordinate                        | Supported | Not supported |
| --------------------------------------- | --------- | ------------- |
| `org.owasp.encoder:encoder`             | 1.4.0     | < 1.4.0       |
| `org.owasp.encoder:encoder-jsp`         | 1.4.0     | < 1.4.0       |
| `org.owasp.encoder:encoder-jakarta-jsp` | 1.4.0     | < 1.4.0       |
| `org.owasp.encoder:encoder-esapi`       | 1.4.0     | < 1.4.0       |

Upgrading the core `encoder` artifact to the latest 1.x release needs no code changes:
no public API was removed between 1.2.3 and 1.4.0. It does need Java 8 or later;
1.2.3 and earlier also ran on Java 5 through 7.

## Reporting a Vulnerability

Please report suspected vulnerabilities privately, not in public issues or pull requests.

Use the **Report a vulnerability** button on the repository's
[Security Advisories](https://github.com/OWASP/owasp-java-encoder/security/advisories)
page. The form requires a GitHub account. If you cannot use it, email the project
leaders at jeremy.long@owasp.org and jim@owasp.org.

A useful report includes:

- the affected artifact and version
- the `Encode` method, tag, or EL function involved
- the input, the encoded output, and the output you expected
- where the output was used (for example, an HTML attribute or a JavaScript string)
  and the browser or parser that interpreted it
- a minimal reproduction, if you have one

## Scope

In scope:

- input that passes through a documented `Encode.forXxx` method (or the matching tag
  or EL function), is used in the context that method documents, and is then
  interpreted as code or markup by a current mainstream parser for that context
- a crash, or unbounded CPU or memory use, on any input
- output that contradicts the encoder's documented contract

Out of scope:

- using an encoder in a context it does not document (for example, `forHtml` output
  placed in a JavaScript string)
- canonicalization, decoding, or input validation, which this library does not perform
- vulnerabilities in ESAPI, Spring, or servlet containers; report ESAPI issues through
  the [ESAPI security policy](https://github.com/ESAPI/esapi-java-legacy/security)
- the unpublished `jakarta-test` application

## Response

These are goals for a volunteer-run project, not guarantees:

- acknowledge a report within about a week
- coordinate disclosure with the reporter, publishing a fix and advisory within
  90 days where possible

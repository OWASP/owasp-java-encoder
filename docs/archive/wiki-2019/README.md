# Historical wiki archive — not current usage advice

These four `.mediawiki` files preserve the exact latest pages from
`https://github.com/OWASP/owasp-java-encoder.wiki.git`, commit
`a10dea70c502c02d072d3d9d2dc520d7c042587a` (2019-11-26), archived 2026-09-26.
`SHA256SUMS` records their bytes. Original author attribution and content are
retained verbatim; old versions, unsafe examples and obsolete advice are not
endorsed. Use the [current guide](../../contexts.md) instead.

The signed-in repository settings showed unrestricted wiki editing on 2026-09-26;
editing was restricted to users with push access before migration. Decision:
hide the wiki after these reviewed replacements merge. Hiding preserves the wiki
repository; do not delete its pages/history. A full local Git mirror/bundle was
also retained for recovery. This tracked snapshot independently preserves all
four latest pages. Re-enable the wiki setting to restore its UI if needed; review
its advice before making it public again.

| Historical page | Disposition and current destination |
| --- | --- |
| Home | Java 1.5/version 1.2 claims replaced by [README](../../../README.md); original-author credit retained. News goes to [CHANGELOG](../../../CHANGELOG.md). |
| Deploy | Old download links replaced by signed 1.4.1 verification/install instructions; Central pending is explicit. No old artifact is recommended. |
| Use | HTML text/attribute, URL, JSP and Writer examples migrated to [usage](../../usage.md) and [contexts](../../contexts.md). Corrected unquoted CSS examples and the URL example that validated one variable but emitted another. |
| Grave Accent Issue | Historical unpatched-IE `innerHTML` round-trip bug; do not migrate the replacement/filter workaround into current library behavior. Prefer safe DOM text APIs and supported browsers. It is distinct from 1.5's ordinary JavaScript template-literal support. |

Current browser coverage is the required Chrome/Selenium suite, not a guarantee
for obsolete IE parsing or arbitrary `innerHTML` transformations. The archive
preserves the old workaround for historical analysis only. External OWASP project
pages are maintained separately; changing this repository does not update them.

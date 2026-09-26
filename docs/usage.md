# Java and JSP examples

Use the [verified signed 1.4.1 distribution](../README.md#start-using-the-owasp-java-encoders)
for production. The examples here use APIs available in that release unless
explicitly marked **1.5**. New features on `main` remain unreleased.

## HTML and Writer output

```java
import org.owasp.encoder.Encode;

out.write("<p>");
Encode.forHtmlContent(out, userText);
out.write("</p><input value=\"");
Encode.forHtmlAttribute(out, userText);
out.write("\">");
```

`out` is a `java.io.Writer` owned by the caller; handle its `IOException` normally.
String-returning calls such as `Encode.forHtmlAttribute(userText)` have the same
context contract. Do not reuse this result for JavaScript, CSS or a URL component.
Avoid double escaping with frameworks that already escape HTML output.

## URLs

For a same-origin search URL with a fixed trusted path and one raw query value:

```java
String url = "/search?q=" + Encode.forUriComponent(query) + "&page=1";
out.write("<a href=\"");
Encode.forHtmlAttribute(out, url);
out.write("\">Search</a>");
```

For an entire untrusted URL, apply application-specific validation **before**
`forHtmlAttribute`: parse with `java.net.URI`, allow-list schemes (often `https`
and `http`), and enforce destination/path restrictions. Parsing alone is not
validation; rejecting or allowing relative URLs is an application decision. Do
not pass a whole URL to `forUriComponent`, or assume deprecated `forUri` validates
one. A reusable validator cannot be inferred without the application's rules.

## JSP tags and functions

Choose the adapter matching the container's `javax` or `jakarta` namespace;
never install both taglib JARs together. For Jakarta:

```jsp
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" %>
<%@ taglib prefix="e" uri="owasp.encoder.jakarta" %>
<p>Function: ${e:forHtmlContent(param.message)}</p>
<p>Tag: <e:forHtmlContent value="${param.message}" /></p>
<input value="${e:forHtmlAttribute(param.message)}">
```

For a javax container, replace the taglib directive with:

```jsp
<%@ taglib prefix="e" uri="https://www.owasp.org/index.php/OWASP_Java_Encoder_Project" %>
```

### Tag bindings and EL evaluation

The basic identifiers above and their advanced equivalents are stable lookup
identifiers; a browser need not be able to fetch them. Advanced identifiers:
`owasp.encoder.jakarta.advanced` and
`https://www.owasp.org/index.php/OWASP_Java_Encoder_Project#advanced`.
Use only the basic or advanced declaration for a given prefix.

On **1.5**, both basic taglibs contain these tags and same-named EL functions:
`forCDATA`, `forHtml`, `forHtmlContent`, `forHtmlAttribute`,
`forHtmlUnquotedAttribute`, `forJavaScript`, `forJson`, `forCssString`, `forCssUrl`,
`forUri`, `forUriComponent`, `forXml`, `forXmlContent`, `forXmlAttribute`, `forXml11`.
Advanced adds `forJavaScriptAttribute`, `forJavaScriptBlock`, `forJavaScriptSource`,
`forXmlComment`, `forXml11Content` and `forXml11Attribute`. `forJava` is intentionally
absent. Through **1.4.1**, neither taglib contains `forJson` or any `forXml11*`
binding, even though XML 1.1 already exists in the core API. XML 1.1 bindings must
output an actual XML 1.1 document, not an HTML page.

All tags require the `value` attribute and an empty body. The container evaluates
the attribute as a String before the tag calls its Writer encoder; a missing EL
value can therefore become an empty String, unlike the facade's null-to-`"null"`
behavior. Plain `${param.message}` is not automatically HTML-escaped by JSP.
Do not evaluate returned strings as EL again, including via framework helpers
that perform a second expression evaluation.

EL availability and defaults depend on the container and deployment descriptor;
legacy descriptors or `isELIgnored="true"` can disable it. Configure a compatible
JSP/EL version and enable EL deliberately, as in the example. Do not treat raw
`${...}` appearing on a page as successful encoding. See the
[Jakarta Pages specification](https://jakarta.ee/specifications/pages/3.0/)
and the [packaged JSP engine checks](../compatibility/jsp-engine/README.md).

## Java modules

The explicit module name for core is `owasp.encoder`. After verifying and obtaining
`encoder-1.4.1.jar`, put that unchanged JAR in `lib/`, then create:

`src/example.app/module-info.java`:

```java
module example.app {
    requires owasp.encoder;
}
```

`src/example.app/example/Main.java`:

```java
package example;
import org.owasp.encoder.Encode;
public class Main {
    public static void main(String[] args) {
        System.out.println(Encode.forHtmlContent("<hello>"));
    }
}
```

With JDK 17:

```sh
javac --module-path lib --module-source-path src -d out -m example.app
java --module-path lib:out -m example.app/example.Main
```

Expected output: `&lt;hello&gt;`. On Windows use `lib;out` for the runtime module
path. Keep unrelated JARs out of `lib/`. Adapter modules additionally require their
API modules; follow the [module/API and OSGi tables](../compatibility/README.md#published-identities-and-development-import-ranges).
Automatic fallback names differ deliberately and are not aliases of the explicit
names. The [consumer harness](../compatibility/README.md#local-reproduction) exercises
both discovery modes on original JARs.

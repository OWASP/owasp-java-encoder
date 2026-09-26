# Basic Java and JSP usage

The OWASP Java Encoder Project is a collection of high-performance low-overhead
contextual encoders, that when utilized correctly, is an effective tool in
preventing Web Application security vulnerabilities such as Cross-Site
Scripting (XSS).

Please see the [OWASP XSS Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html)
for more information on preventing XSS.

See the [README](../README.md) for the current distribution status, Jakarta coordinates, and full context guidance. Version 1.4.1 is retained as signed release assets while Central publication is pending; use those exact artifacts until Central availability is confirmed.

### Usage

In addition to the usage guidance below, more examples can be found on the [OWASP Java Encoder project page](https://owasp.org/www-project-java-encoder/).

Before using these coordinates, [download and verify the signed 1.4.1 release](../releases/1.4.1.md#verification) and install its artifacts in your local or organizational repository, as described in the [README](../README.md#start-using-the-owasp-java-encoders). These examples depend on that installation while Central publication is pending.

```xml
<dependency>
    <groupId>org.owasp.encoder</groupId>
    <artifactId>encoder</artifactId>
    <version>1.4.1</version>
</dependency>
```

Utilize the encoder:

```java
import org.owasp.encoder.Encode;

//...

PrintWriter out = ....;
out.println("<textarea>" + Encode.forHtml(userData) + "</textarea>");
```

### JSP Usage

The JSP Encoder makes the use of the Java Encoder within JSP simple via a TLD that
includes tags and a set of JSP EL functions:

```xml
<dependency>
    <groupId>org.owasp.encoder</groupId>
    <artifactId>encoder-jsp</artifactId>
    <version>1.4.1</version>
</dependency>
```

```JSP
<%@taglib prefix="e" uri="https://www.owasp.org/index.php/OWASP_Java_Encoder_Project" %>

<%-- ... --%>

<p>Dynamic data via EL: ${e:forHtml(param.value)}</p>
<p>Dynamic data via tag: <e:forHtml value="${param.value}" /></p>
```

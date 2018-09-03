## OWASP JSP

The OWASP JSP Encoder is a collection of high-performance low-overhead
contextual encoders that, when utilized correctly, is an effective tool in
preventing Web Application security vulnerabilities such as Cross-Site
Scripting (XSS).

Please see the [OWASP XSS Prevention Cheat Sheet](https://www.owasp.org/index.php/XSS_%28Cross_Site_Scripting%29_Prevention_Cheat_Sheet)
for more information on preventing XSS.

### JSP Usage

The JSP Encoder makes the use of the Java Encoder within JSP simple via a TLD that
includes tags and a set of JSP EL functions:

```xml
<dependency>
    <groupId>org.owasp.encoder</groupId>
    <artifactId>encoder-jsp</artifactId>
    <version>1.2.2</version>
</dependency>
```

```JSP
<%@taglib prefix="e" uri="https://www.owasp.org/index.php/OWASP_Java_Encoder_Project" %>

<%-- ... --%>

<p>Dynamic data via EL: ${e:forHtml(param.value)}</p>
<p>Dynamic data via tag: <e:forHtml value="${param.value}" /></p>
```

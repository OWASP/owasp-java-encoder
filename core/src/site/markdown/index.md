## OWASP Java Encoder

The OWASP Java Encoder is a collection of high-performance low-overhead
contextual encoders that, when utilized correctly, is an effective tool in
preventing Web Application security vulnerabilities such as Cross-Site
Scripting (XSS).

Please see the [OWASP XSS Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html)
for more information on preventing XSS.

For use within JSP pages consider using the [JSP Encoder](../encoder-jsp/index.html) as it
provides a TLD to make the use of the core encoders easier.

### Usage

The JAR can be found in [Maven Central](https://central.sonatype.com/artifact/org.owasp.encoder/encoder).

```xml
<dependency>
    <groupId>org.owasp.encoder</groupId>
    <artifactId>encoder</artifactId>
    <version>1.4.0</version>
</dependency>
```

Utilize the encoder:

```java
import org.owasp.encoder.Encode;

//...

PrintWriter out = ....;
out.println("<textarea>" + Encode.forHtml(userData) + "</textarea>");
```

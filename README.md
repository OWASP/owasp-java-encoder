OWASP Java Encoder Project
==========================

[![Build Status](https://travis-ci.org/OWASP/owasp-java-encoder.svg?branch=master)](https://travis-ci.org/OWASP/owasp-java-encoder) [![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)

Contextual Output Encoding is a computer programming technique necessary to stop
Cross-Site Scripting. This project is a Java 1.5+ simple-to-use drop-in high-performance
encoder class with little baggage.

For more information on how to use this project, please see the [OWASP wiki](https://www.owasp.org/index.php/OWASP_Java_Encoder_Project#tab=Use_the_Java_Encoder_Project).

Start using the OWASP Java Encoders
-----------------------------------
You can download a JAR from [Maven Central](https://search.maven.org/#search|ga|1|g%3A%22org.owasp.encoder%22%20a%3A%22encoder%22).

JSP tags and EL functions are available in the encoder-jsp, also available in [Central](http://search.maven.org/remotecontent?filepath=org/owasp/encoder/encoder-jsp/1.2/encoder-jsp-1.2.jar).

The jars are also available in Maven:

```xml
<dependency>
    <groupId>org.owasp.encoder</groupId>
    <artifactId>encoder</artifactId>
    <version>1.2.2</version>
</dependency>

<dependency>
    <groupId>org.owasp.encoder</groupId>
    <artifactId>encoder-jsp</artifactId>
    <version>1.2.2</version>
</dependency>
```

Quick Overview
--------------
The OWASP Java Encoder library is intended for quick contextual encoding with very little
overhead, either in performance or usage. To get started, simply add the encoder-1.2.jar,
import org.owasp.encoder.Encode and start using.

Example usage:

```java
    PrintWriter out = ....;
    out.println("<textarea>"+Encode.forHtml(userData)+"</textarea>");
```

Please look at the javadoc for Encode to see the variety of contexts for which you can encode.

Happy Encoding!

News
----
### 2018-09-14 - 1.2.2 Release
The team is happy to announce that version 1.2.2 has been released! 
* This is a minor release fixing documentation and licensing issues.

### 2017-02-19 - 1.2.1 Release
The team is happy to announce that version 1.2.1 has been released! 
* The CDATA Encoder was modified so that it does not emit intermediate characters between adjacent CDATA sections.
* The documentation on [gh-pages](http://owasp.github.io/owasp-java-encoder/) has been improved.

### 2015-04-12 - 1.2 Release on GitHub
OWASP Java Encoder has been moved to GitHub. Version 1.2 was also released!

### 2014-03-31 - Documentation updated
Please visit https://www.owasp.org/index.php/OWASP_Java_Encoder_Project#tab=Use_the_Java_Encoder_Project to see detailed documentation and examples on each API use!

### 2014-01-30 - Version 1.1.1 released
We're happy to announce that version 1.1.1 has been released. Along with a important bug fix, we added ESAPI integration to replace the legacy ESAPI encoders with the OWASP Java Encoder.

### 2013-02-14 - Version 1.1 released
We're happy to announce that version 1.1 has been released. Along with a few minor encoding enhancements, we improved performance, and added a JSP tag and function library.

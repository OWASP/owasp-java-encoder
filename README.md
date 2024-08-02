OWASP Java Encoder Project
==========================

![Build Status](https://github.com/OWASP/owasp-java-encoder/actions/workflows/build.yaml/badge.svg?branch=main) [![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause) [![javadoc](https://javadoc.io/badge2/org.owasp.encoder/encoder/javadoc.svg)](https://javadoc.io/doc/org.owasp.encoder/encoder)

Contextual Output Encoding is a computer programming technique necessary to stop
Cross-Site Scripting. This project is a Java 1.8+ simple-to-use drop-in high-performance
encoder class with little baggage.

For more detailed documentation on the OWASP Javca Encoder please visit https://owasp.org/www-project-java-encoder/.

Start using the OWASP Java Encoders
-----------------------------------
You can download a JAR from [Maven Central](https://search.maven.org/#search|ga|1|g%3A%22org.owasp.encoder%22%20a%3A%22encoder%22).

JSP tags and EL functions are available in the encoder-jsp, also available:
- [encoder-jakarta-jsp](http://search.maven.org/remotecontent?filepath=org/owasp/encoder/encoder-jakarta-jsp/1.2.3/encoder-jakarta-jsp-1.2.3.jar) - Servlet Spec 5.0
- [encoder-jsp](http://search.maven.org/remotecontent?filepath=org/owasp/encoder/encoder-jsp/1.2.3/encoder-jsp-1.2.3.jar) - Servlet Spec 3.0

The jars are also available in Central:

```xml
<dependency>
    <groupId>org.owasp.encoder</groupId>
    <artifactId>encoder</artifactId>
    <version>1.3.0</version>
</dependency>

<!-- using Servlet Spec 5 in the jakarta.servlet package use: -->
<dependency>
    <groupId>org.owasp.encoder</groupId>
    <artifactId>encoder-jakarta-jsp</artifactId>
    <version>1.3.0</version>
</dependency>

<!-- using the Legacy Servlet Spec in the javax.servlet package use: -->
<dependency>
    <groupId>org.owasp.encoder</groupId>
    <artifactId>encoder-jsp</artifactId>
    <version>1.3.0</version>
</dependency>
```

Quick Overview
--------------
The OWASP Java Encoder library is intended for quick contextual encoding with very little
overhead, either in performance or usage. To get started, simply add the encoder-1.2.3.jar,
import org.owasp.encoder.Encode and start using.

Example usage:

```java
    PrintWriter out = ....;
    out.println("<textarea>"+Encode.forHtml(userData)+"</textarea>");
```

Please look at the javadoc for Encode to see the variety of contexts for which you can encode.

Happy Encoding!

Building
--------

Due to test cases for the `encoder-jakarta-jsp` project Java 17 is required to package and test
the project. Simply run:

```shell
mvn package
```

To run the Jakarta JSP intgration test, to validate that the JSP Tags and EL work correctly run:

```shell
mvn verify -PtestJakarta
```

* Note that the above test may fail on modern Apple silicon.

Java 9+ Module Names
--------------------

| JAR                 | Module Name           |
|---------------------|-----------------------|
| encoder             | owasp.encoder         |
| encoder-jakarta-jsp | owasp.encoder.jakarta |
| encoder-jsp         | owasp.encoder.jsp     |
| encoder-espai       | owasp.encoder.esapi   |


TagLib
--------------------

| Lib                 | TagLib                                                                                        |
|---------------------|-----------------------------------------------------------------------------------------------|
| encoder-jakarta-jsp | &lt;%@taglib prefix="e" uri="owasp.encoder.jakarta"%&gt;                                      |
| encoder-jsp         | &lt;%@taglib prefix="e" uri="https://www.owasp.org/index.php/OWASP_Java_Encoder_Project"%&gt; |


News
----
### 2024-08-02 - 1.3.0 Release
The team is happy to announce that version 1.3.0 has been released!
* Minimum JDK Requirement is now Java 8
  - Requires Java 17 to build due to test case dependencies.
* Adds Java 9 Module name via Multi-Release Jars (#77).
* Fixed compilation errors with the ESAPI Thunk (#76).
* Adds support for Servlet Spec 5 using the `jakarta.servlet.*` (#75).
  - taglib : &lt;%@taglib prefix="e" uri="owasp.encoder.jakarta"%&gt;

### 2020-11-08 - 1.2.3 Release
The team is happy to announce that version 1.2.3 has been released! 
* Update to  make the manifest OSGi-compliant (#39).
* Update to support ESAPI 2.2 and later (#37).

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

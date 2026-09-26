// Copyright (c) 2026 OWASP
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//
//     * Redistributions of source code must retain the above
//       copyright notice, this list of conditions and the following
//       disclaimer.
//
//     * Redistributions in binary form must reproduce the above
//       copyright notice, this list of conditions and the following
//       disclaimer in the documentation and/or other materials
//       provided with the distribution.
//
//     * Neither the name of the OWASP nor the names of its
//       contributors may be used to endorse or promote products
//       derived from this software without specific prior written
//       permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package org.owasp.encoder.tag;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilderFactory;
import junit.framework.TestCase;
import org.owasp.encoder.Encode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Checks the tags and EL functions declared in the basic and advanced TLDs.
 */
public class TaglibDescriptorTest extends TestCase {

    private static final String BASIC = "META-INF/java-encoder.tld";
    private static final String ADVANCED = "META-INF/java-encoder-advanced.tld";

    public TaglibDescriptorTest(String testName) {
        super(testName);
    }

    /**
     * The tags and functions of one TLD, keyed by name.
     */
    static final class Taglib {
        final Map<String, String> tagClasses = new LinkedHashMap<String, String>();
        final Map<String, String> functionSignatures = new LinkedHashMap<String, String>();
    }

    static Taglib load(String resource) throws Exception {
        InputStream in = TaglibDescriptorTest.class.getClassLoader().getResourceAsStream(resource);
        assertNotNull(resource, in);
        Document doc;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            doc = factory.newDocumentBuilder().parse(in);
        } finally {
            in.close();
        }

        Taglib taglib = new Taglib();
        for (Node n = doc.getDocumentElement().getFirstChild(); n != null; n = n.getNextSibling()) {
            if (!(n instanceof Element)) {
                continue;
            }
            Element e = (Element) n;
            if ("tag".equals(e.getLocalName())) {
                String name = child(e, "name");
                assertNull(resource + ": duplicate tag " + name,
                    taglib.tagClasses.put(name, child(e, "tag-class")));
            } else if ("function".equals(e.getLocalName())) {
                String name = child(e, "name");
                assertEquals(resource + ": " + name, Encode.class.getName(), child(e, "function-class"));
                assertNull(resource + ": duplicate function " + name,
                    taglib.functionSignatures.put(name, child(e, "function-signature")));
            }
        }
        return taglib;
    }

    /**
     * Java source generation is not a JSP context. XML 1.1 tags are tracked
     * separately in issue #131. Deprecated forUri remains for compatibility;
     * deprecation alone must not silently remove a deployed tag/function.
     */
    public void testAdvancedMatchesSupportedFacade() throws Exception {
        Set<String> unsupported = new TreeSet<String>(Arrays.asList(
            "forJava", "forXml11", "forXml11Content", "forXml11Attribute"));
        Set<String> expected = new TreeSet<String>();
        for (Method method : Encode.class.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && method.getName().startsWith("for")
                    && method.getReturnType() == String.class
                    && Arrays.equals(new Class<?>[] {String.class}, method.getParameterTypes())) {
                expected.add(method.getName());
            }
        }
        assertTrue("exclusions must name real facade methods", expected.containsAll(unsupported));
        expected.removeAll(unsupported);
        Taglib advanced = load(ADVANCED);
        assertEquals("advanced tags", expected, advanced.tagClasses.keySet());
        assertEquals("advanced functions", expected, advanced.functionSignatures.keySet());
    }

    /**
     * Returns the trimmed text of the first direct child element with the
     * given local name.
     */
    private static String child(Element parent, String localName) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element && localName.equals(n.getLocalName())) {
                return n.getTextContent().trim();
            }
        }
        fail("missing <" + localName + ">");
        return null;
    }

    private static void assertExposesForJson(String resource) throws Exception {
        Taglib taglib = load(resource);
        assertEquals(resource, ForJsonTag.class.getName(), taglib.tagClasses.get("forJson"));
        assertEquals(resource, "java.lang.String forJson(java.lang.String)",
            taglib.functionSignatures.get("forJson"));
    }

    public void testBasicTldExposesForJson() throws Exception {
        assertExposesForJson(BASIC);
    }

    public void testAdvancedTldExposesForJson() throws Exception {
        assertExposesForJson(ADVANCED);
    }

    /**
     * Every tag class must be an EncodingTag and every function must be a
     * static String method of Encode taking a single String.
     */
    public void testDeclarationsResolve() throws Exception {
        for (String resource : new String[] {BASIC, ADVANCED}) {
            Taglib taglib = load(resource);
            for (Map.Entry<String, String> tag : taglib.tagClasses.entrySet()) {
                Class<?> tagClass = Class.forName(tag.getValue());
                assertTrue(resource + ": " + tag.getKey(),
                    EncodingTag.class.isAssignableFrom(tagClass));
                assertEquals(resource + ": tag wired to the wrong context",
                    "org.owasp.encoder.tag.F" + tag.getKey().substring(1) + "Tag", tagClass.getName());
                assertFalse(resource + ": abstract tag", Modifier.isAbstract(tagClass.getModifiers()));
                tagClass.getConstructor();
            }
            for (Map.Entry<String, String> function : taglib.functionSignatures.entrySet()) {
                String name = function.getKey();
                assertEquals(resource + ": " + name,
                    "java.lang.String " + name + "(java.lang.String)", function.getValue());
                Method method = Encode.class.getMethod(name, String.class);
                assertTrue(resource + ": " + name, Modifier.isStatic(method.getModifiers()));
                assertEquals(resource + ": " + name, String.class, method.getReturnType());
            }
            assertEquals(resource + ": every context has a tag and a function",
                taglib.tagClasses.keySet(), taglib.functionSignatures.keySet());
        }
    }

    /**
     * The basic TLD is a subset of the advanced TLD with identical
     * declarations.
     */
    public void testBasicIsSubsetOfAdvanced() throws Exception {
        Taglib basic = load(BASIC);
        Taglib advanced = load(ADVANCED);
        for (Map.Entry<String, String> tag : basic.tagClasses.entrySet()) {
            assertEquals(tag.getKey(), tag.getValue(), advanced.tagClasses.get(tag.getKey()));
        }
        for (Map.Entry<String, String> function : basic.functionSignatures.entrySet()) {
            assertEquals(function.getKey(), function.getValue(),
                advanced.functionSignatures.get(function.getKey()));
        }
    }
}

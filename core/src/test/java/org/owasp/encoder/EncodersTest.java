// Copyright (c) 2012 Jeff Ichnowski
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

package org.owasp.encoder;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.io.StringWriter;

/**
 * EncodersTest -- Tests for the Encoders class.
 *
 * @author Jeff Ichnowski
 */
public class EncodersTest extends TestCase {

    public void testConcreteContextMappings() {
        for (EncoderContexts context : EncoderContexts.ALL) {
            Encoder actual = Encoders.forName(context.name);
            assertEquals(context.name, context.encoder.getClass(), actual.getClass());
            assertSame(context.name + " singleton", actual, Encoders.forName(context.name));
            // Compare against an independently constructed encoder so a facade and
            // registry wired to the same wrong singleton cannot agree accidentally.
            assertEquals(context.name, Encode.encode(context.encoder, EncoderContexts.probe()),
                Encode.encode(actual, EncoderContexts.probe()));
            if (actual instanceof XMLEncoder || actual instanceof JavaScriptEncoder
                    || actual instanceof CSSEncoder || actual instanceof URIEncoder) {
                assertEquals(context.name + " mode/version", context.encoder.toString(), actual.toString());
            }
        }
    }

    public void testNullContext() {
        try {
            Encoders.forName(null);
            fail("null context accepted");
        } catch (NullPointerException expected) {
            // Required by the factory contract.
        }
    }

    public void testUnknownAndCaseSensitiveContexts() {
        for (String name : new String[] {"nope", "", "HTML"}) {
            try {
                Encoders.forName(name);
                fail("unknown context accepted: " + name);
            } catch (UnsupportedContextException expected) {
                assertEquals(name, expected.getMessage());
            }
        }
    }

    public void testWriterByContextName() throws Exception {
        StringWriter out = new StringWriter();
        EncodedWriter writer = new EncodedWriter(out, "html");
        writer.write("<&\"'>");
        writer.close();
        assertEquals("&lt;&amp;&#34;&#39;&gt;", out.toString());
    }

    public static Test suite() throws Exception {
        return new TestSuite(EncodersTest.class);
    }

    public void testForNameIsNotNull() throws Exception {
        Field[] fields = Encoders.class.getFields();
        int count = 0;
        for (Field field : fields) {
            if (Modifier.isPublic(field.getModifiers()) &&
                Modifier.isStatic(field.getModifiers()) &&
                Modifier.isFinal(field.getModifiers()) &&
                field.getType() == String.class)
            {
                String contextName = (String) field.get(null);
                Encoder encoder = Encoders.forName(contextName);
                assertNotNull("Encoder: "+contextName, encoder);
                count++;
            }
        }

        assertTrue(count > 0);
    }

    /**
     * The "uri" context is deprecated everywhere Encode.forUri is, but stays
     * usable for compatibility.
     */
    public void testUriContextIsDeprecatedButRetained() throws Exception {
        assertTrue(Encoders.class.getField("URI").isAnnotationPresent(Deprecated.class));
        assertTrue(Encode.class.getMethod("forUri", String.class)
            .isAnnotationPresent(Deprecated.class));
        assertTrue(Encode.class.getMethod("forUri", java.io.Writer.class, String.class)
            .isAnnotationPresent(Deprecated.class));
        assertFalse(Encoders.class.getField("URI_COMPONENT").isAnnotationPresent(Deprecated.class));
        assertSame(Encoders.URI_ENCODER, Encoders.forName("uri"));
    }

    public void testJsonContext() throws Exception {
        assertEquals("json", Encoders.JSON);
        Encoder encoder = Encoders.forName(Encoders.JSON);
        assertSame(Encoders.JSON_ENCODER, encoder);
        assertTrue(encoder instanceof JSONEncoder);
        assertNotSame(Encoders.JAVASCRIPT_SOURCE_ENCODER, encoder);
    }

    public void testXML11Names() {
        assertXML11Encoder("xml-1.1", XMLEncoder.Mode.ALL,
            "&#x01;&amp;&lt;&gt;&#34;&#39;");
        assertXML11Encoder("xml-1.1-content", XMLEncoder.Mode.CONTENT,
            "&#x01;&amp;&lt;&gt;\"'");
        assertXML11Encoder("xml-1.1-attribute", XMLEncoder.Mode.ATTRIBUTE,
            "&#x01;&amp;&lt;>&#34;&#39;");
    }

    private void assertXML11Encoder(String name, XMLEncoder.Mode mode, String expected) {
        Encoder encoder = Encoders.forName(name);
        assertTrue(name, encoder instanceof XMLEncoder);
        assertEquals("XMLEncoder(" + mode + ", " + XMLEncoder.Version.XML_1_1 + ")",
            encoder.toString());
        assertEquals(name, expected, Encode.encode(encoder, "\u0001&<>\"'"));
    }
}

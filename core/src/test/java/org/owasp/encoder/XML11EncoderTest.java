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

/**
 * XML11EncoderTest -- test suite for the XML 1.1 encoder.
 *
 * @author Jeff Ichnowski
 */
public class XML11EncoderTest extends TestCase {
    
    public XML11EncoderTest(String testName) {
        super(testName);
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite();
        for (XMLEncoder.Mode mode : XMLEncoder.Mode.values()) {
            XMLEncoder encoder = new XMLEncoder(mode, XMLEncoder.Version.XML_1_1);
            EncoderTestSuiteBuilder builder = new EncoderTestSuiteBuilder(encoder, "-safe-", "-&-")
                .encode("safe", "safe")
                .encode("unencoded &amp; encoded", "unencoded & encoded")
                .encode("valid-surrogate-pair", "\ud800\udc00", "\ud800\udc00")
                .encode("missing-low-surrogate", " ", "\ud800")
                .encode("missing-high-surrogate", " ", "\udc00")
                .encode("valid-upper-char", "\ufffd", "\ufffd")
                .encode("invalid-upper-char", " ", "\uffff")
                
                // XML 1.1 specific: control characters are encoded, not replaced
                .encode("control-char-0x01", "&#x01;", "\u0001")
                .encode("control-char-0x02", "&#x02;", "\u0002")
                .encode("control-char-0x08", "&#x08;", "\u0008")
                .encode("control-char-0x0B", "&#x0b;", "\u000B")
                .encode("control-char-0x0C", "&#x0c;", "\u000C")
                .encode("control-char-0x0E", "&#x0e;", "\u000E")
                .encode("control-char-0x1F", "&#x1f;", "\u001F")
                
                // C1 control characters (0x7F-0x9F) should be encoded in XML 1.1
                .encode("control-char-0x7F", "&#x7f;", "\u007F")
                .encode("control-char-0x80", "&#x80;", "\u0080")
                .encode("control-char-0x9F", "&#x9f;", "\u009F")
                
                // Tab, LF, CR are still passed through unencoded
                .encode("tab-char", "\t", "\t")
                .encode("lf-char", "\n", "\n")
                .encode("cr-char", "\r", "\r")
                
                // NEL (0x85) is valid and unencoded in XML 1.1
                .encode("nel-char", "\u0085", "\u0085")
                
                // Combined test
                .encode("mixed-control-chars", "&#x01;a\t&#x7f;b\nc", "\u0001a\t\u007Fb\nc");

            // Invalid characters: null, non-characters, surrogates should be replaced
            builder.invalid(0x00, 0x00)
                .invalid(Character.MIN_SURROGATE, Character.MAX_SURROGATE)
                .invalid(0xfdd0, 0xfdef)
                .invalid(0xfffe, 0xffff)
                .invalid(0x1fffe, 0x1ffff)
                .invalid(0x2fffe, 0x2ffff)
                .invalid(0x3fffe, 0x3ffff)
                .invalid(0x4fffe, 0x4ffff)
                .invalid(0x5fffe, 0x5ffff)
                .invalid(0x6fffe, 0x6ffff)
                .invalid(0x7fffe, 0x7ffff)
                .invalid(0x8fffe, 0x8ffff)
                .invalid(0x9fffe, 0x9ffff)
                .invalid(0xafffe, 0xaffff)
                .invalid(0xbfffe, 0xbffff)
                .invalid(0xcfffe, 0xcffff)
                .invalid(0xdfffe, 0xdffff)
                .invalid(0xefffe, 0xeffff)
                .invalid(0xffffe, 0xfffff)
                .invalid(0x10fffe, 0x10ffff);
            
            // Mark all characters as valid (they're allowed in XML 1.1, even if they get encoded)
            builder.valid(0x01, Character.MAX_CODE_POINT);

            switch (mode) {
            case ALL:
                builder.encoded("&><\'\"")
                    .encode("&amp;", "&")
                    .encode("&gt;", ">")
                    .encode("&lt;", "<")
                    .encode("&#39;", "\'")
                    .encode("&#34;", "\"");
                break;
            case CONTENT:
                builder.encoded("&><")
                    .encode("&amp;", "&")
                    .encode("&gt;", ">")
                    .encode("&lt;", "<")
                    .encode("\'", "\'")
                    .encode("\"", "\"");
                break;
            case ATTRIBUTE:
                builder.encoded("&<\'\"")
                    .encode("&amp;", "&")
                    .encode(">", ">")
                    .encode("&lt;", "<")
                    .encode("&#39;", "\'")
                    .encode("&#34;", "\"");
                break;
            case SINGLE_QUOTED_ATTRIBUTE:
                builder.encoded("&<\'")
                    .encode("&amp;", "&")
                    .encode(">", ">")
                    .encode("&lt;", "<")
                    .encode("&#39;", "\'")
                    .encode("\"", "\"");
                break;
            case DOUBLE_QUOTED_ATTRIBUTE:
                builder.encoded("&<\"")
                    .encode("&amp;", "&")
                    .encode(">", ">")
                    .encode("&lt;", "<")
                    .encode("\'", "\'")
                    .encode("&#34;", "\"");
                break;
            default:
                throw new AssertionError("untested mode: "+mode);
            }

            suite.addTest(builder
                .invalidSuite(XMLEncoder.INVALID_CHARACTER_REPLACEMENT)
                .encodedSuite()
                .build());
        }
        return suite;
    }
    
    /**
     * Test that the public API methods work correctly for XML 1.1.
     */
    public void testXML11PublicAPI() {
        String input = "test\u0001\u0002&<>";
        
        // Test forXml11
        String result = Encode.forXml11(input);
        assertEquals("test&#x01;&#x02;&amp;&lt;&gt;", result);
        
        // Test forXml11Content
        result = Encode.forXml11Content(input);
        assertEquals("test&#x01;&#x02;&amp;&lt;&gt;", result);
        
        // Test forXml11Attribute
        result = Encode.forXml11Attribute(input);
        assertEquals("test&#x01;&#x02;&amp;&lt;>", result);
    }
    
    /**
     * Test that tab, lf, and cr are not encoded in XML 1.1.
     */
    public void testXML11AllowedControlChars() {
        String input = "a\tb\nc\rd";
        String result = Encode.forXml11(input);
        assertEquals("a\tb\nc\rd", result);
    }
    
    /**
     * Test that C0 control characters (except tab, lf, cr) are encoded in XML 1.1.
     */
    public void testXML11C0ControlChars() {
        // Test each C0 control character
        for (int i = 1; i <= 0x1F; i++) {
            if (i == 0x09 || i == 0x0A || i == 0x0D) {
                // Tab, LF, CR should not be encoded
                continue;
            }
            char ch = (char) i;
            String input = "a" + ch + "b";
            String result = Encode.forXml11(input);
            String expected = "a&#x" + String.format("%02x", i) + ";b";
            assertEquals("C0 control char 0x" + Integer.toHexString(i) + " should be encoded", 
                        expected, result);
        }
    }
    
    /**
     * Test that C1 control characters (except NEL) are encoded in XML 1.1.
     */
    public void testXML11C1ControlChars() {
        // Test DEL and C1 control characters
        for (int i = 0x7F; i <= 0x9F; i++) {
            if (i == 0x85) {
                // NEL should not be encoded
                continue;
            }
            char ch = (char) i;
            String input = "a" + ch + "b";
            String result = Encode.forXml11(input);
            String expected = "a&#x" + String.format("%02x", i) + ";b";
            assertEquals("C1 control char 0x" + String.format("%02x", i) + " should be encoded", 
                        expected, result);
        }
    }
    
    /**
     * Test that NEL (0x85) is not encoded in XML 1.1.
     */
    public void testXML11NEL() {
        String input = "a\u0085b";
        String result = Encode.forXml11(input);
        assertEquals("a\u0085b", result);
    }
}

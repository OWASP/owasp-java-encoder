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
 * URIEncoderTest -- test suite for the URIEncoder.
 *
 * @author Jeff Ichnowski
 */
public class URIEncoderTest extends TestCase {
    public static Test suite() {
        TestSuite suite = new TestSuite();
        for (URIEncoder.Mode mode : URIEncoder.Mode.values()) {
            EncoderTestSuiteBuilder builder = new EncoderTestSuiteBuilder(new URIEncoder(mode), "-safe-", "<>")
                .encoded(0, Character.MAX_CODE_POINT)
                .invalid(Character.MIN_SURROGATE, Character.MAX_SURROGATE)

                // Characters safe in all modes
                .valid("-._~")
                .valid('a', 'z')
                .valid('A', 'Z')
                .valid('0', '9');

            switch (mode) {
            case COMPONENT:
                // reserved gen-delims
                builder.encode("%3A", ":");
                builder.encode("%2F", "/");
                builder.encode("%3F", "?");
                builder.encode("%23", "#");
                builder.encode("%5B", "[");
                builder.encode("%5D", "]");
                builder.encode("%40", "@");

                // reserved sub-delims
                builder.encode("%21", "!");
                builder.encode("%24", "$");
                builder.encode("%26", "&");
                builder.encode("%27", "'");
                builder.encode("%28", "(");
                builder.encode("%29", ")");
                builder.encode("%2A", "*");
                builder.encode("%2B", "+");
                builder.encode("%2C", ",");
                builder.encode("%3B", ";");
                builder.encode("%3D", "=");
                break;
            case FULL_URI:
                builder.encode(
                    "full-url",
                    "http://www.owasp.org/index.php?foo=bar&baz#fragment",
                    "http://www.owasp.org/index.php?foo=bar&baz#fragment");
                builder.valid(":/?#[]@!$&'()*+,;=");
                break;
            default:
                throw new AssertionError("untested mode: "+mode);
            }
            suite.addTest(builder
                // simple test of some unencoded characters
                .encode("abcABC123", "abcABC123")

                // ASCII characters encoded in all modes
                .encode("%20", " ")
                .encode("%22", "\"")
                .encode("%25", "%")
                .encode("%3C", "<")
                .encode("%3E", ">")
                .encode("%5C", "\\")
                .encode("%5E", "^")
                .encode("%60", "`")
                .encode("%7B", "{")
                .encode("%7C", "|")
                .encode("%7D", "}")

                // UTF-8 multi-byte handling
                .encode("2-byte-utf-8", "%C2%A0", "\u00a0")
                .encode("3-byte-utf-8", "%E0%A0%80", "\u0800")
                    // TODO: double check that this is how the surrogate pair should
                    // be encoded
                .encode("4-byte-utf-8", "%F0%90%80%80",
                    new StringBuilder().appendCodePoint(0x10000).toString())

                .encode("missing-low-surrogate", "-", "\ud800")
                .encode("missing-high-surrogate", "-", "\udc00")

                .validSuite()
                .invalidSuite('-')
                .encodedSuite()

                .build());
        }
        return suite;
    }
}

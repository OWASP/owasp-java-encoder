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
import org.owasp.encoder.JavaScriptEncoder.Mode;

/**
 * JavaScriptEncoderTest -- test suite for the JavaScriptEncoder.
 *
 * @author Jeff Ichnowski
 */
public class JavaScriptEncoderTest extends TestCase {
    public static Test suite() {
        TestSuite suite = new TestSuite();
        for (int asciiOnly = 0 ; asciiOnly <= 1 ; ++asciiOnly) {
            for (JavaScriptEncoder.Mode mode : JavaScriptEncoder.Mode.values()) {
//                if (!(mode == JavaScriptEncoder.Mode.HTML_CONTENT && asciiOnly == 0)) continue;
                EncoderTestSuiteBuilder builder = new EncoderTestSuiteBuilder(new JavaScriptEncoder(mode, asciiOnly==1), "(safe)", "(\\)")
                    .encoded(0, 0x1f)
                    .valid(' ', '~')
                    .encoded("\\\'\"");

                switch (mode) {
                case SOURCE:
                case BLOCK:
                    builder
                        .encode("\\\"", "\"")
                        .encode("\\\'", "\'");
                    break;
                case HTML:
                case ATTRIBUTE:
                    builder
                        .encode("\\x22", "\"")
                        .encode("\\x27", "\'");
                    break;
                default:
                    throw new AssertionError("unexpected mode: "+mode);
                }

                switch (mode) {
                case BLOCK:
                case HTML:
                    builder
                        .encode("\\/", "/")
                        .encode("\\-", "-")
                        .encoded("/-");
                    break;
                default:
                    builder.encode("/", "/");
                    break;
                }
                if (mode != Mode.SOURCE) {
                    builder.encoded("&");
                }

                builder
                    .encode("\\\\", "\\")
                    .encode("backspace", "\\b", "\b")
                    .encode("tab", "\\t", "\t")
                    .encode("LF", "\\n", "\n")
                    .encode("vtab", "\\x0b", "\u000b")
                    .encode("FF", "\\f", "\f")
                    .encode("CR", "\\r", "\r")
                    .encode("NUL", "\\x00", "\0")
                    .encode("Line Separator", "\\u2028", "\u2028")
                    .encode("Paragraph Separator", "\\u2029", "\u2029")
                    .encode("abc", "abc")
                    .encode("ABC", "ABC");

                if (asciiOnly == 0) {
                    builder
                        .encode("unicode", "\u1234", "\u1234")
                        .encode("high-ascii", "\u00ff", "\u00ff")
                        .valid(0x7f, Character.MAX_CODE_POINT)
                        .encoded("\u2028\u2029");
                } else {
                    builder
                        .encode("unicode", "\\u1234", "\u1234")
                        .encode("high-ascii", "\\xff", "\u00ff")
                        .encoded(0x7f, Character.MAX_CODE_POINT);
                }

                suite.addTest(builder
                    .validSuite()
                    .encodedSuite()
                    .build());
            }
        }
        return suite;
    }
}

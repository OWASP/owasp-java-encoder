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

import java.io.StringWriter;
import java.io.Writer;
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
        TestSuite suite = new TestSuite(JavaScriptEncoderTest.class);
        for (int asciiOnly = 0 ; asciiOnly <= 1 ; ++asciiOnly) {
            for (JavaScriptEncoder.Mode mode : JavaScriptEncoder.Mode.values()) {
//                if (!(mode == JavaScriptEncoder.Mode.HTML_CONTENT && asciiOnly == 0)) continue;
                EncoderTestSuiteBuilder builder = new EncoderTestSuiteBuilder(new JavaScriptEncoder(mode, asciiOnly==1), "(safe)", "(\\)")
                    .encoded(0, 0x1f)
                    .valid(' ', '~')
                    .encoded("\\\'\"`${");

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
                    .encode("backtick", "\\x60", "`")
                    .encode("dollar", "\\x24", "$")
                    .encode("opening brace", "\\x7b", "{")
                    .encode("template start", "\\x24\\x7b", "${")
                    .encode("trusted dollar boundary", "\\x7bexecuted=true}", "{executed=true}")
                    .encode("trailing dollar", "end\\x24", "end$")
                    .encode("escaped-looking interpolation", "\\\\\\x24\\x7bvalue}", "\\${value}")
                    .encode("escaped-looking backtick", "\\\\\\x60", "\\`")
                    .encode("template expression", "\\x24\\x7balert(1)}", "${alert(1)}")
                    .encode("template breakout", "hell\\x60;alert(1);\\x60o", "hell`;alert(1);`o")
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

    public void testTemplateCharactersThroughPublicFacadesAndRegistry() throws Exception {
        String input = "price=$5;`${value}`;\\${escaped}";
        String expected = "price=\\x245;\\x60\\x24\\x7bvalue}\\x60;\\\\\\x24\\x7bescaped}";
        String[] methods = {"forJavaScript", "forJavaScriptAttribute",
            "forJavaScriptBlock", "forJavaScriptSource"};
        String[] contexts = {Encoders.JAVASCRIPT, Encoders.JAVASCRIPT_ATTRIBUTE,
            Encoders.JAVASCRIPT_BLOCK, Encoders.JAVASCRIPT_SOURCE};
        for (int i = 0; i < methods.length; i++) {
            assertEquals(methods[i], expected,
                Encode.class.getMethod(methods[i], String.class).invoke(null, input));
            StringWriter out = new StringWriter();
            Encode.class.getMethod(methods[i], Writer.class, String.class).invoke(null, out, input);
            assertEquals(methods[i], expected, out.toString());
            assertEquals(contexts[i], expected, Encode.encode(Encoders.forName(contexts[i]), input));
            assertEquals(methods[i], "\\x7bexecuted=true}",
                Encode.class.getMethod(methods[i], String.class).invoke(null, "{executed=true}"));
        }
    }

    public void testTemplateCharactersAcrossWriterBuffers() throws Exception {
        // Place '$' at an input-buffer boundary and make output exceed its buffer,
        // then write each character separately to exercise chunk-independent escapes.
        StringBuilder input = new StringBuilder();
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < Encode.Buffer.INPUT_BUFFER_SIZE - 1; i++) {
            input.append('a');
            expected.append('a');
        }
        for (int i = 0; i < Encode.Buffer.OUTPUT_BUFFER_SIZE; i++) {
            input.append("${`}");
            expected.append("\\x24\\x7b\\x60}");
        }
        String[] methods = {"forJavaScript", "forJavaScriptAttribute",
            "forJavaScriptBlock", "forJavaScriptSource"};
        String[] contexts = {Encoders.JAVASCRIPT, Encoders.JAVASCRIPT_ATTRIBUTE,
            Encoders.JAVASCRIPT_BLOCK, Encoders.JAVASCRIPT_SOURCE};
        for (int i = 0; i < methods.length; i++) {
            assertEquals(methods[i], expected.toString(),
                Encode.class.getMethod(methods[i], String.class).invoke(null, input.toString()));
            StringWriter out = new StringWriter();
            Encode.class.getMethod(methods[i], Writer.class, String.class)
                .invoke(null, out, input.toString());
            assertEquals(methods[i], expected.toString(), out.toString());
            out = new StringWriter();
            EncodedWriter writer = new EncodedWriter(out, contexts[i]);
            for (int j = 0; j < input.length(); j++) {
                writer.write(input.charAt(j));
            }
            writer.close();
            assertEquals(contexts[i], expected.toString(), out.toString());
        }
    }
}

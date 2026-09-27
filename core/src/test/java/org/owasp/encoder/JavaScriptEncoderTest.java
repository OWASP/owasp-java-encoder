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
                boolean htmlBlock = mode == Mode.BLOCK || mode == Mode.HTML;
                String trustedDollarExpected = htmlBlock
                    ? "\\x7bexe\\x63u\\x74ed=\\x74\\x72ue}"
                    : "\\x7bexecuted=true}";
                String templateExpressionExpected = htmlBlock
                    ? "\\x24\\x7bale\\x72\\x74(1)}"
                    : "\\x24\\x7balert(1)}";
                String templateBreakoutExpected = htmlBlock
                    ? "hell\\x60;ale\\x72\\x74(1);\\x60o"
                    : "hell\\x60;alert(1);\\x60o";
                EncoderTestSuiteBuilder builder = new EncoderTestSuiteBuilder(
                    new JavaScriptEncoder(mode, asciiOnly==1), "(xyz)", "(\\)")
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
                        .encode("\\x20", " ")
                        .encode("\\x21", "!")
                        .encode("\\/", "/")
                        .encode("\\x2d", "-")
                        .encode("\\x3c", "<")
                        .encode("\\x3e", ">")
                        .encode("script delimiter alphabet",
                            "\\x43\\x49\\x50\\x52\\x53\\x54"
                                + "\\x63\\x69\\x70\\x72\\x73\\x74",
                            "CIPRSTciprst")
                        .encoded(" !-/<>CIPRSTciprst");
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
                    .encode("lone high surrogate", "\\ud800", "\ud800")
                    .encode("lone low surrogate", "\\udfff", "\udfff")
                    .encode("reversed pair", "\\udc00\\ud800", "\udc00\ud800")
                    .encode("backtick", "\\x60", "`")
                    .encode("dollar", "\\x24", "$")
                    .encode("opening brace", "\\x7b", "{")
                    .encode("template start", "\\x24\\x7b", "${")
                    .encode("trusted dollar boundary", trustedDollarExpected,
                        "{executed=true}")
                    .encode("trailing dollar", "end\\x24", "end$")
                    .encode("escaped-looking interpolation", "\\\\\\x24\\x7bvalue}", "\\${value}")
                    .encode("escaped-looking backtick", "\\\\\\x60", "\\`")
                    .encode("template expression", templateExpressionExpected,
                        "${alert(1)}")
                    .encode("template breakout", templateBreakoutExpected,
                        "hell`;alert(1);`o")
                    .encode("abc", htmlBlock ? "ab\\x63" : "abc", "abc")
                    .encode("ABC", htmlBlock ? "AB\\x43" : "ABC", "ABC")
                    // DEL and the C1 controls are hex encoded in every mode
                    .encode("DEL", "\\x7f", "\u007f")
                    .encode("U+0080", "\\x80", "\u0080")
                    .encode("NEL", "\\x85", "\u0085")
                    .encode("CSI", "\\x9b", "\u009b")
                    .encode("U+009F", "\\x9f", "\u009f")
                    .encode("NEL in text", "a\\x85b", "a\u0085b");

                if (asciiOnly == 0) {
                    builder
                        .encode("unicode", "\u1234", "\u1234")
                        .encode("high-ascii", "\u00ff", "\u00ff")
                        .encode("non-breaking space", "\u00a0", "\u00a0")
                        .encode("surrogate pair", "\ud83d\ude00", "\ud83d\ude00")
                        .valid(0xa0, Character.MAX_CODE_POINT)
                        .encoded(0x7f, Unicode.MAX_C1_CTRL_CHAR)
                        .encoded(Character.MIN_SURROGATE, Character.MAX_SURROGATE)
                        .encoded("\u2028\u2029");
                } else {
                    builder
                        .encode("unicode", "\\u1234", "\u1234")
                        .encode("high-ascii", "\\xff", "\u00ff")
                        .encode("non-breaking space", "\\xa0", "\u00a0")
                        .encode("surrogate pair", "\\ud83d\\ude00", "\ud83d\ude00")
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
        String htmlBlockExpected = "\\x70\\x72\\x69\\x63e=\\x245;"
            + "\\x60\\x24\\x7bvalue}\\x60;\\\\\\x24\\x7be\\x73\\x63a\\x70ed}";
        String trustedDollarExpected = "\\x7bexe\\x63u\\x74ed=\\x74\\x72ue}";
        String[] methods = {"forJavaScript", "forJavaScriptAttribute",
            "forJavaScriptBlock", "forJavaScriptSource"};
        String[] contexts = {Encoders.JAVASCRIPT, Encoders.JAVASCRIPT_ATTRIBUTE,
            Encoders.JAVASCRIPT_BLOCK, Encoders.JAVASCRIPT_SOURCE};
        for (int i = 0; i < methods.length; i++) {
            String methodExpected = (i == 0 || i == 2) ? htmlBlockExpected : expected;
            assertEquals(methods[i], methodExpected,
                Encode.class.getMethod(methods[i], String.class).invoke(null, input));
            StringWriter out = new StringWriter();
            Encode.class.getMethod(methods[i], Writer.class, String.class).invoke(null, out, input);
            assertEquals(methods[i], methodExpected, out.toString());
            assertEquals(contexts[i], methodExpected,
                Encode.encode(Encoders.forName(contexts[i]), input));
            assertEquals(methods[i], (i == 0 || i == 2)
                    ? trustedDollarExpected : "\\x7bexecuted=true}",
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

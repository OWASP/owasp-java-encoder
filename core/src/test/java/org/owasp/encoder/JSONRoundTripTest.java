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

package org.owasp.encoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.nio.charset.Charset;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Checks {@link Encode#forJson(String)} output against an independent JSON
 * parser: the encoded value is wrapped in quotes, converted to UTF-8 bytes,
 * and parsed back from those bytes.
 */
public class JSONRoundTripTest extends TestCase {
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    /**
     * Number of code points encoded per round trip in the full-range test.
     */
    private static final int CHUNK = 4096;

    private final ObjectMapper _mapper = new ObjectMapper();

    public static Test suite() {
        return new TestSuite(JSONRoundTripTest.class);
    }

    public void testJavaScriptEscapesAreNotJson() throws IOException {
        for (String encoded : new String[] {Encode.forJavaScriptSource("'"),
                Encode.forJavaScriptSource("\0"), Encode.forJavaScript("\"&")}) {
            try {
                _mapper.readValue(("\"" + encoded + "\"").getBytes(UTF_8), String.class);
                fail("JavaScript output must not be mistaken for JSON: " + encoded);
            } catch (JsonProcessingException expected) {
                // RFC 8259 does not allow JavaScript's apostrophe or hex escapes.
            }
        }
    }

    /**
     * Encodes {@code input}, parses the UTF-8 bytes of the resulting JSON
     * string and asserts the parsed value equals {@code input}.
     */
    private void assertRoundTrip(String input) throws IOException {
        String encoded = Encode.forJson(input);
        assertOnlyJsonEscapes(input, encoded);

        byte[] json = ("\"" + encoded + "\"").getBytes(UTF_8);
        String parsed = _mapper.readValue(json, String.class);
        if (!input.equals(parsed)) {
            assertEquals(EncoderTestSuiteBuilder.debugEncode(input),
                EncoderTestSuiteBuilder.debugEncode(input),
                EncoderTestSuiteBuilder.debugEncode(parsed));
        }
    }

    /**
     * Asserts the output only uses the escapes RFC 8259 defines, and never
     * contains characters that could end an HTML script element or a
     * pre-ES2019 JavaScript line.
     */
    private static void assertOnlyJsonEscapes(String input, String encoded) {
        String context = EncoderTestSuiteBuilder.debugEncode(input);
        for (int i = 0, n = encoded.length(); i < n; ++i) {
            char ch = encoded.charAt(i);
            if (ch == '\\') {
                assertTrue(context + ": dangling backslash", i + 1 < n);
                char esc = encoded.charAt(++i);
                if (esc == 'u') {
                    assertTrue(context, i + 4 < n);
                    for (int k = 1; k <= 4; ++k) {
                        char hex = encoded.charAt(i + k);
                        assertTrue(context + ": lowercase hex expected",
                            ('0' <= hex && hex <= '9') || ('a' <= hex && hex <= 'f'));
                    }
                    i += 4;
                } else {
                    assertTrue(context + ": unexpected escape \\" + esc,
                        "\"\\btnfr".indexOf(esc) >= 0);
                }
            } else {
                assertFalse(context + ": raw character needs escaping",
                    ch < ' ' || ch == '"' || ch == '<' || ch == '>' || ch == '&'
                    || ch == '\u2028' || ch == '\u2029');
                if (Character.isHighSurrogate(ch)) {
                    assertTrue(context + ": unpaired high surrogate",
                        i + 1 < n && Character.isLowSurrogate(encoded.charAt(i + 1)));
                    ++i;
                } else {
                    assertFalse(context + ": unpaired low surrogate",
                        Character.isLowSurrogate(ch));
                }
            }
        }
    }

    public void testAllCodePointsRoundTripThroughUtf8() throws IOException {
        StringBuilder buf = new StringBuilder(CHUNK * 2);
        for (int cp = 0; cp <= Character.MAX_CODE_POINT; ++cp) {
            buf.appendCodePoint(cp);
            if (buf.length() >= CHUNK || cp == Character.MAX_CODE_POINT) {
                assertRoundTrip(buf.toString());
                buf.setLength(0);
            }
        }
    }

    public void testEachCodePointRoundTripsThroughUtf8() throws IOException {
        for (int cp = 0; cp <= Character.MAX_CODE_POINT; ++cp) {
            assertRoundTrip(new String(Character.toChars(cp)));
        }
    }

    public void testUnpairedSurrogatesRoundTripThroughUtf8() throws IOException {
        String[] inputs = {
            "\ud800", "\udbff", "\udc00", "\udfff",
            "a\ud800b", "a\udc00b",
            "\udc00\ud800",
            "\ud800\ud800\udc00",
            "\ud800\udc00\udc00",
            "\ud83d\ude00\ud83d",
            "<\ud800>",
        };
        for (String input : inputs) {
            assertRoundTrip(input);
        }
    }

    /**
     * Shows why unpaired surrogates are escaped: written raw, they do not
     * survive conversion to UTF-8.
     */
    public void testRawUnpairedSurrogateDoesNotSurviveUtf8() throws IOException {
        String raw = "\"\ud800\"";
        String parsed = _mapper.readValue(raw.getBytes(UTF_8), String.class);
        assertFalse("\ud800".equals(parsed));
    }

    public void testNullIsTheStringNull() throws IOException {
        byte[] json = ("\"" + Encode.forJson(null) + "\"").getBytes(UTF_8);
        assertEquals("null", _mapper.readValue(json, String.class));
    }

    public void testParsesInsideJsonObject() throws IOException {
        String input = "</script><!--\"'&\u2028\ud800";
        byte[] json = ("{\"name\":\"" + Encode.forJson(input) + "\"}").getBytes(UTF_8);
        assertEquals(input, _mapper.readTree(json).get("name").asText());
    }
}

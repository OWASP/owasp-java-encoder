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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Random;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Checks the Writer paths of the JSON encoder against the String path:
 * {@link Encode#forJson(java.io.Writer, String)}, which encodes in
 * 1,024-character batches, and {@link EncodedWriter}, which may receive a
 * surrogate pair split across two {@code write} calls.
 */
public class JSONWriterTest extends TestCase {

    /**
     * Characters that exercise every branch of the encoder, including both
     * halves of surrogate pairs.
     */
    private static final String ALPHABET = "a<\"\\\n\0\u00e9\u2028\ud800\udc00\udbff\udfff";

    public static Test suite() {
        return new TestSuite(JSONWriterTest.class);
    }

    private static String forJsonWriter(String input) throws IOException {
        StringWriter out = new StringWriter();
        Encode.forJson(out, input);
        return out.toString();
    }

    private static void assertWriterMatchesString(String message, String input) throws IOException {
        String expected = Encode.forJson(input);
        String actual = forJsonWriter(input);
        if (!expected.equals(actual)) {
            assertEquals(message, EncoderTestSuiteBuilder.debugEncode(expected),
                EncoderTestSuiteBuilder.debugEncode(actual));
        }
    }

    private static String repeat(char ch, int n) {
        StringBuilder buf = new StringBuilder(n);
        for (int i = 0; i < n; ++i) {
            buf.append(ch);
        }
        return buf.toString();
    }

    /**
     * A character the encoder leaves unread at the end of a 1,024-character
     * batch must be encoded exactly once. The leading "&lt;" makes the first
     * batch start at offset 0, and every placement of the tested sequence
     * across the first two batch boundaries is compared with the String
     * result.
     */
    public void testBatchBoundaryMatchesString() throws IOException {
        String[] sequences = {
            "\ud83d\ude00",     // valid pair
            "\ud800",           // lone high
            "\udc00",           // lone low
            "\ud800\ud800\udc00",
            "\ud800<",
            "\udc00\ud800",
        };
        final int batch = Encode.Buffer.INPUT_BUFFER_SIZE;
        for (String seq : sequences) {
            for (int pos = 1; pos < 2 * batch + 8; ++pos) {
                if (pos > 8 && Math.abs(pos - batch) > 8 && Math.abs(pos - 2 * batch) > 8) {
                    continue;
                }
                String input = "<" + repeat('a', pos - 1) + seq + repeat('a', batch);
                assertWriterMatchesString("sequence " + EncoderTestSuiteBuilder.debugEncode(seq)
                    + " at " + pos, input);
            }
        }
    }

    public void testSurrogatePairAtBatchBoundary() throws IOException {
        final int batch = Encode.Buffer.INPUT_BUFFER_SIZE;
        String input = "<" + repeat('a', batch - 2) + "\ud83d\ude00b";
        assertEquals(batch + 2, input.length());
        String expected = "\\u003c" + repeat('a', batch - 2) + "\ud83d\ude00b";
        assertEquals(expected, Encode.forJson(input));
        assertEquals(expected, forJsonWriter(input));
    }

    public void testLoneHighSurrogateAtBatchBoundary() throws IOException {
        final int batch = Encode.Buffer.INPUT_BUFFER_SIZE;
        String input = "<" + repeat('a', batch - 2) + "\ud800b";
        String expected = "\\u003c" + repeat('a', batch - 2) + "\\ud800b";
        assertEquals(expected, Encode.forJson(input));
        assertEquals(expected, forJsonWriter(input));
    }

    public void testRandomInputsMatchString() throws IOException {
        Random random = new Random(0x4a534f4eL);
        for (int t = 0; t < 200; ++t) {
            String input = randomInput(random, random.nextInt(4 * Encode.Buffer.INPUT_BUFFER_SIZE));
            assertWriterMatchesString("trial " + t, input);
            assertEncodedWriterSplitsMatchString(random, input);
        }
    }

    private static String randomInput(Random random, int n) {
        StringBuilder buf = new StringBuilder(n);
        for (int i = 0; i < n; ++i) {
            buf.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return buf.toString();
    }

    private static void assertEncodedWriterSplitsMatchString(Random random, String input) throws IOException {
        StringWriter out = new StringWriter();
        EncodedWriter writer = new EncodedWriter(out, Encoders.JSON);
        char[] chars = input.toCharArray();
        int i = 0;
        while (i < chars.length) {
            int len = Math.min(chars.length - i, random.nextInt(8) == 0 ? 0 : 1 + random.nextInt(2000));
            writer.write(chars, i, len);
            i += len;
        }
        writer.close();
        String expected = Encode.forJson(input);
        String actual = out.toString();
        if (!expected.equals(actual)) {
            assertEquals(EncoderTestSuiteBuilder.debugEncode(expected),
                EncoderTestSuiteBuilder.debugEncode(actual));
        }
    }

    private static String encodeInWrites(String... writes) throws IOException {
        StringWriter out = new StringWriter();
        EncodedWriter writer = new EncodedWriter(out, Encoders.JSON);
        for (String write : writes) {
            writer.write(write);
        }
        writer.close();
        return out.toString();
    }

    public void testSurrogatePairSplitAcrossWrites() throws IOException {
        assertEquals("\ud83d\ude00", encodeInWrites("\ud83d", "\ude00"));
        assertEquals("a\ud83d\ude00b", encodeInWrites("a\ud83d", "\ude00b"));
    }

    public void testSurrogatePairSplitByEmptyWrite() throws IOException {
        assertEquals("\ud83d\ude00", encodeInWrites("\ud83d", "", "\ude00"));
        assertEquals("\ud83d\ude00", encodeInWrites("\ud83d", "", "", "\ude00"));
    }

    /**
     * Writing U+D800, U+D800 and U+DC00 separately also covers the
     * EncodedWriter look-ahead regression: the second write is absorbed
     * into the left over buffer without resolving the look-ahead.
     *
     * @throws IOException not thrown
     */
    public void testUnpairedHighSurrogateSplitAcrossWrites() throws IOException {
        assertEquals("\\ud800a", encodeInWrites("\ud800", "a"));
        assertEquals("\\ud800\\u003c", encodeInWrites("\ud800", "<"));
        assertEquals("\\ud800\ud800\udc00", encodeInWrites("\ud800", "\ud800", "\udc00"));
        assertEquals("\\ud800\ud800\udc00", encodeInWrites("\ud800", "\ud800\udc00"));
    }

    public void testUnpairedHighSurrogateAtClose() throws IOException {
        assertEquals("\\ud800", encodeInWrites("\ud800"));
        assertEquals("a\\ud800", encodeInWrites("a\ud800", ""));
    }

    public void testLowSurrogateAloneInWrite() throws IOException {
        assertEquals("a\\udc00", encodeInWrites("a", "\udc00"));
    }

    /**
     * The left over high surrogate is flushed when the output buffer of the
     * EncodedWriter is full, so encoding it overflows.  The pair must still
     * be written exactly once.
     */
    public void testSplitSurrogateWhenOutputBufferIsFull() throws IOException {
        final int size = EncodedWriter.BUFFER_SIZE;
        for (int fill = size - 8; fill <= size; ++fill) {
            String prefix = repeat('a', fill);
            assertEquals(prefix + "\ud83d\ude00b", encodeInWrites(prefix + "\ud83d", "\ude00b"));
            assertEquals(prefix + "\\ud800b", encodeInWrites(prefix + "\ud800", "b"));
        }
    }
}

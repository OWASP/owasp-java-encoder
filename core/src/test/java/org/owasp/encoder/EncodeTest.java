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

import java.io.IOException;
import java.io.StringWriter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * EncodeTest -- exercises the core encode loops in {@link Encode.Buffer}.
 *
 * @author Jeff Ichnowski
 */
public class EncodeTest extends TestCase {
    public static Test suite() {
        return new TestSuite(EncodeTest.class);
    }

    public void testEncodeToWriter() throws IOException {
        StringWriter out = new StringWriter();
        Encode.forXml(out, "<script>");
        assertEquals("&lt;script&gt;", out.toString());
    }

    /**
     * This test validates the logic for dealing with large encodes that
     * require multiple flushes to the writer because they exceed the
     * internal buffer size of the encode loop.
     *
     * @throws IOException should not be thrown
     */
    public void testBatchedEncodingToWriter() throws IOException {
        String input = "&&&&&&&&&&&&&&&&&&&&"
            .replace("&", "&&&&&&&&&&&&&&&&&&&&")  // 400
            .replace("&", "&&&&&&&&&&&&&&&&&&&&"); // 8000
        String expected = input.replace("&", "&amp;");
        final int[] appendCount = new int[1];
        StringWriter out = new StringWriter() {
            @Override
            public void write(char[] cbuf, int off, int len) {
                super.write(cbuf, off, len);
                appendCount[0]++;
            }
        };
        Encode.forXml(out, input);

        // this assert here is validating that we are encoding with output
        // that is larger than the internal buffers of the encode loop.
        // the only way this should fail is if the buffer in the encode loop
        // is made larger without also updating this test.
        assertTrue(appendCount[0] > 1);

        assertEquals(8000 * 5, out.getBuffer().length());
        assertEquals(expected, out.toString());
    }

    /**
     * This method tests that strings that do not require encoding are passed
     * directly to the writer as a string, without any additional buffer
     * copies.
     *
     * @throws IOException not thrown
     */
    public void testUnencodedStringToWriter() throws IOException {
        final String unencodedString = "safe";
        final boolean[] called = new boolean[1];
        StringWriter out = new StringWriter() {
            @Override
            public void write(String str) {
                called[0] = true;
                assertSame(unencodedString, str);
                super.write(str);
            }
        };
        Encode.forXml(out, unencodedString);
        assertTrue(called[0]);
        assertEquals(unencodedString, out.toString());
    }

    public void testEncodeNullToWriter() throws IOException {
        StringWriter out = new StringWriter();
        Encode.forXml(out, null);
        assertEquals("null", out.toString());
    }

    public void testEncodeString() {
        assertEquals("&lt;script&gt;", Encode.forXml("<script>"));
    }

    public void testEncodeNullToString() {
        assertEquals("null", Encode.forXml(null));
    }

    /**
     * Make sure return the argument when the string is not encoded.
     */
    public void testUnencodedString() {
        String input = "safe";
        assertSame(input, Encode.forXml(input));
    }

    public void testLargeEncodeToString() {
        final String input = "&&&&&&&&&&"
            .replace("&", "&&&&&&&&&&") // 100
            .replace("&", "&&&&&&&&&&"); // 1000

        // this should be smaller than the internal input buffer
        // but cause overflow to the output buffer
        assertEquals(1000, input.length());
        String output = Encode.forXml(input);
        assertEquals(5000, output.length());
        assertEquals(input.replace("&", "&amp;"), output);
    }

    public void testVeryLargeEncodeToString() {
        final String input = "&&&&&&&&&&&&&&&&&&&&"
            .replace("&", "&&&&&&&&&&&&&&&&&&&&") // 400
            .replace("&", "&&&&&&&&&&&&&&&&&&&&"); // 8000

        // this should be larger than the internal input buffer
        assertEquals(8000, input.length());
        String output = Encode.forXml(input);
        assertEquals(40000, output.length());
        assertEquals(input.replace("&", "&amp;"), output);
    }
}

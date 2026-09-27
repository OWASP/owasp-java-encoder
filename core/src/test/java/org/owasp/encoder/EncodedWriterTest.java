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
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Checks that {@link EncodedWriter} produces the same output as encoding the
 * whole text in one call, however the text is split across writes.  The splits
 * cut through sequences that encoders hold back for lookahead, at every fill
 * level of the writer's internal buffer near its capacity.
 */
public class EncodedWriterTest {

    /** Sequences that make at least one encoder wait for the next character. */
    private static final String[] PROBES = {
        "]]>", "]]]>>", "-->", "--->", "-a>", "<<a", "\"\"x", "\u2028 a", "\t0",
        "x\uD83D\uDE00y", "\uD83Dx", "</script>"
    };

    /** Written after each probe, so a context escape would expose markup. */
    private static final String TAIL = "<b>";

    @Test(timeout = 5000)
    public void cdataTerminatorSplitAcrossFullBufferStaysEncoded() throws IOException {
        StringWriter out = new StringWriter();
        EncodedWriter writer = new EncodedWriter(out, Encoders.CDATA);
        writer.write(repeat('a', 1008));
        writer.write("]]");
        writer.write("]]>>");
        writer.write("<script>");
        writer.close();
        assertEquals(Encode.forCDATA(repeat('a', 1008) + "]]]]>><script>"), out.toString());
    }

    @Test(timeout = 5000)
    public void xmlCommentTerminatorSplitAcrossFullBufferStaysEncoded() throws IOException {
        StringWriter out = new StringWriter();
        EncodedWriter writer = new EncodedWriter(out, Encoders.XML_COMMENT);
        writer.write(repeat('a', 1023));
        writer.write("-");
        writer.write("a><script>");
        writer.close();
        assertEquals(Encode.forXmlComment(repeat('a', 1023) + "-a><script>"), out.toString());
    }

    @Test(timeout = 5000)
    public void pendingLookaheadWithoutMoreInputDoesNotHang() throws IOException {
        StringWriter css = new StringWriter();
        EncodedWriter cssWriter = new EncodedWriter(css, Encoders.CSS_STRING);
        cssWriter.write('<');
        cssWriter.write('<');
        cssWriter.write("");
        cssWriter.close();
        assertEquals("\\3c\\3c", css.toString());

        StringWriter html = new StringWriter();
        EncodedWriter htmlWriter = new EncodedWriter(html, Encoders.HTML);
        htmlWriter.write("x\uD83D");
        htmlWriter.write("");
        htmlWriter.write("\uDE00");
        htmlWriter.close();
        assertEquals("x\uD83D\uDE00", html.toString());
    }

    @Test
    public void closeIsIdempotentAndClosedOperationsFail() throws IOException {
        StrictWriter out = new StrictWriter();
        final EncodedWriter writer = new EncodedWriter(out, Encoders.HTML);
        writer.write("a\uD83D");
        writer.close();
        assertEquals("a ", out.toString());
        assertEquals(1, out.closeCount);

        writer.close();
        assertEquals(1, out.closeCount);
        assertIOException(new IOAction() {
            @Override
            public void run() throws IOException {
                writer.write('z');
            }
        });
        assertIOException(new IOAction() {
            @Override
            public void run() throws IOException {
                writer.write(new char[0], 0, 0);
            }
        });
        assertIOException(new IOAction() {
            @Override
            public void run() throws IOException {
                writer.write(new char[] {'z'});
            }
        });
        assertIOException(new IOAction() {
            @Override
            public void run() throws IOException {
                writer.write("z");
            }
        });
        assertIOException(new IOAction() {
            @Override
            public void run() throws IOException {
                writer.write("z", 0, 1);
            }
        });
        assertIOException(new IOAction() {
            @Override
            public void run() throws IOException {
                writer.append('z');
            }
        });
        assertIOException(new IOAction() {
            @Override
            public void run() throws IOException {
                writer.append("z");
            }
        });
        assertIOException(new IOAction() {
            @Override
            public void run() throws IOException {
                writer.append("xyz", 1, 2);
            }
        });
        assertIOException(new IOAction() {
            @Override
            public void run() throws IOException {
                writer.flush();
            }
        });
    }

    @Test
    public void closeStillClosesDelegateWhenFinalWriteFails() throws IOException {
        FailingWriter out = new FailingWriter(true, false);
        EncodedWriter writer = new EncodedWriter(out, Encoders.HTML);
        writer.write('<');

        try {
            writer.close();
            fail("close should propagate the final write failure");
        } catch (IOException expected) {
            assertEquals("write failed", expected.getMessage());
        }
        assertEquals(1, out.closeCount);

        writer.close();
        assertEquals(1, out.closeCount);
    }

    @Test
    public void closePreservesSuppressedDelegateFailure() throws IOException {
        FailingWriter out = new FailingWriter(true, true);
        EncodedWriter writer = new EncodedWriter(out, Encoders.HTML);
        writer.write('<');

        try {
            writer.close();
            fail("close should propagate the final write failure");
        } catch (IOException expected) {
            assertEquals("write failed", expected.getMessage());
            assertEquals(1, expected.getSuppressed().length);
            assertEquals("close failed", expected.getSuppressed()[0].getMessage());
        }
        assertEquals(1, out.closeCount);
    }

    @Test
    public void closePropagatesDelegateCloseFailureAndBecomesIdempotent()
            throws IOException {
        FailingWriter out = new FailingWriter(false, true);
        EncodedWriter writer = new EncodedWriter(out, Encoders.HTML);
        writer.write('a');

        try {
            writer.close();
            fail("close should propagate the delegate close failure");
        } catch (IOException expected) {
            assertEquals("close failed", expected.getMessage());
            assertEquals(0, expected.getSuppressed().length);
        }
        assertEquals(1, out.closeCount);
        writer.close();
        assertEquals(1, out.closeCount);
    }

    @Test
    public void flushDoesNotFinalizePendingInput() throws IOException {
        StringWriter out = new StringWriter();
        EncodedWriter writer = new EncodedWriter(out, Encoders.HTML);
        writer.write("x\uD83D");
        writer.flush();
        assertEquals("x", out.toString());
        writer.write("\uDE00");
        writer.close();
        assertEquals("x\uD83D\uDE00", out.toString());
    }

    @Test
    public void invalidWriteRangesThrowIndexOutOfBoundsException() throws IOException {
        StringWriter out = new StringWriter();
        final EncodedWriter writer = new EncodedWriter(out, Encoders.HTML);
        final char[] chars = {'a'};

        try {
            writer.write((char[]) null, 0, 0);
            fail("expected NullPointerException");
        } catch (NullPointerException expected) {
            // Expected.
        }
        assertIndexOutOfBounds(new IOAction() {
            @Override
            public void run() throws IOException {
                writer.write(chars, -1, 1);
            }
        });
        assertIndexOutOfBounds(new IOAction() {
            @Override
            public void run() throws IOException {
                writer.write(chars, 0, -1);
            }
        });
        assertIndexOutOfBounds(new IOAction() {
            @Override
            public void run() throws IOException {
                writer.write(chars, 1, 1);
            }
        });
        assertIndexOutOfBounds(new IOAction() {
            @Override
            public void run() throws IOException {
                writer.write(chars, Integer.MAX_VALUE, 1);
            }
        });
        assertIndexOutOfBounds(new IOAction() {
            @Override
            public void run() throws IOException {
                writer.write(chars, 1, Integer.MAX_VALUE);
            }
        });
        assertIndexOutOfBounds(new IOAction() {
            @Override
            public void run() throws IOException {
                writer.write(chars, Integer.MAX_VALUE, Integer.MAX_VALUE);
            }
        });
        assertIndexOutOfBounds(new IOAction() {
            @Override
            public void run() throws IOException {
                writer.write(chars, chars.length + 1, 0);
            }
        });

        writer.write(chars, chars.length, 0);
        writer.close();
        assertEquals("", out.toString());
    }

    @Test(timeout = 60000)
    public void splitWritesMatchSingleCallEncoding() throws Exception {
        List<String> names = MaxEncodedLengthTest.contextNames();
        assertTrue(names.size() > 1);
        int checked = 0;
        for (String name : names) {
            Encoder encoder = Encoders.forName(name);
            for (String probe : PROBES) {
                for (int fill = EncodedWriter.BUFFER_SIZE - 20; fill <= EncodedWriter.BUFFER_SIZE; ++fill) {
                    for (List<String> writes : splits(probe)) {
                        assertSameAsSingleCall(name, encoder, repeat('a', fill), writes);
                        ++checked;
                    }
                }
            }
        }
        assertTrue(checked > 10000);
    }

    private static void assertSameAsSingleCall(String name, Encoder encoder, String filler, List<String> writes)
            throws IOException {
        StringBuilder text = new StringBuilder(filler);
        StringWriter out = new StringWriter();
        EncodedWriter writer = new EncodedWriter(out, encoder);
        writer.write(filler);
        for (String write : writes) {
            writer.write(write);
            text.append(write);
        }
        writer.write(TAIL);
        text.append(TAIL);
        writer.close();

        String expected = encodeInOneCall(encoder, text);
        if (!expected.equals(out.toString())) {
            fail(name + " with " + filler.length() + " filler characters, then writes "
                + MaxEncodedLengthTest.escape(writes.toString()) + ": expected ..."
                + MaxEncodedLengthTest.escape(tail(expected)) + " but was ..."
                + MaxEncodedLengthTest.escape(tail(out.toString())));
        }
    }

    /**
     * Returns ways to split {@code probe} into writes: at every position
     * (including empty writes at either end), one character per write, and one
     * character per write with an empty write after each.
     */
    private static List<List<String>> splits(String probe) {
        List<List<String>> splits = new ArrayList<List<String>>();
        for (int i = 0; i <= probe.length(); ++i) {
            splits.add(Arrays.asList(probe.substring(0, i), probe.substring(i)));
        }
        List<String> single = new ArrayList<String>();
        List<String> singleWithEmpty = new ArrayList<String>();
        for (int i = 0; i < probe.length(); ++i) {
            single.add(probe.substring(i, i + 1));
            singleWithEmpty.add(probe.substring(i, i + 1));
            singleWithEmpty.add("");
        }
        splits.add(single);
        splits.add(singleWithEmpty);
        return splits;
    }

    private static String encodeInOneCall(Encoder encoder, CharSequence text) {
        CharBuffer in = CharBuffer.wrap(text.toString().toCharArray());
        CharBuffer out = CharBuffer.allocate(text.length() * 32);
        assertTrue(encoder.encode(in, out, true).isUnderflow());
        return new String(out.array(), 0, out.position());
    }

    private static String tail(String s) {
        return s.substring(Math.max(0, s.length() - 40));
    }

    private static String repeat(char ch, int n) {
        char[] chars = new char[n];
        Arrays.fill(chars, ch);
        return new String(chars);
    }

    private static void assertIOException(IOAction action) {
        try {
            action.run();
            fail("expected IOException");
        } catch (IOException expected) {
            // Expected.
        }
    }

    private static void assertIndexOutOfBounds(IOAction action) throws IOException {
        try {
            action.run();
            fail("expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
            // Expected.
        }
    }

    private interface IOAction {
        void run() throws IOException;
    }

    private static final class StrictWriter extends Writer {

        private final StringBuilder output = new StringBuilder();
        private boolean closed;
        private int closeCount;

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            ensureOpen();
            output.append(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException {
            ensureOpen();
        }

        @Override
        public void close() {
            closed = true;
            ++closeCount;
        }

        @Override
        public String toString() {
            return output.toString();
        }

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("strict writer closed");
            }
        }
    }

    private static final class FailingWriter extends Writer {

        private final boolean failWrite;
        private final boolean failClose;
        private int closeCount;

        FailingWriter(boolean failWrite, boolean failClose) {
            this.failWrite = failWrite;
            this.failClose = failClose;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            if (failWrite) {
                throw new IOException("write failed");
            }
        }

        @Override
        public void flush() {
            // Nothing to do.
        }

        @Override
        public void close() throws IOException {
            ++closeCount;
            if (failClose) {
                throw new IOException("close failed");
            }
        }
    }
}

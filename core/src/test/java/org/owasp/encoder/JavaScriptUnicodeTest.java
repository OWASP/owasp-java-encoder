package org.owasp.encoder;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import junit.framework.TestCase;

/** Unicode fidelity, UTF-8 transport, and streaming regression tests. */
public class JavaScriptUnicodeTest extends TestCase {
    private static final String[] METHODS = {"forJavaScript", "forJavaScriptAttribute",
        "forJavaScriptBlock", "forJavaScriptSource"};

    public void testEveryLoneSurrogateAndControlInEveryMode() {
        for (JavaScriptEncoder.Mode mode : JavaScriptEncoder.Mode.values()) {
            for (boolean asciiOnly : new boolean[]{false, true}) {
                Encoder encoder = new JavaScriptEncoder(mode, asciiOnly);
                for (int ch = Character.MIN_SURROGATE; ch <= Character.MAX_SURROGATE; ch++) {
                    String input = String.valueOf((char) ch);
                    assertEquals(encoder.toString(), unicodeEscape(ch), Encode.encode(encoder, input));
                    assertEquals(0, encoder.firstEncodedOffset(input, 0, 1));
                }
                for (int ch = 0x7f; ch <= 0x9f; ch++) {
                    String input = String.valueOf((char) ch);
                    assertEquals(encoder.toString(), hexEscape(ch), Encode.encode(encoder, input));
                    assertEquals(0, encoder.firstEncodedOffset(input, 0, 1));
                }
            }
        }
    }

    public void testScannerRespectsSliceAndPairBoundaries() {
        String pair = "a\ud83d\ude00z";
        for (JavaScriptEncoder.Mode mode : JavaScriptEncoder.Mode.values()) {
            Encoder encoder = new JavaScriptEncoder(mode, false);
            assertEquals(4, encoder.firstEncodedOffset(pair, 0, 4));
            assertEquals(3, encoder.firstEncodedOffset(pair, 1, 2));
            assertEquals(1, encoder.firstEncodedOffset(pair, 1, 1));
            assertEquals(2, encoder.firstEncodedOffset(pair, 2, 1));
            assertEquals(1, encoder.firstEncodedOffset(pair, 1, 0));
            assertEquals(3, encoder.firstEncodedOffset("a\ud83d\ude00\u0085", 0, 4));
            assertEquals(3, encoder.firstEncodedOffset("a\ud83d\ude00\ud800", 0, 4));
            assertEquals(1, new JavaScriptEncoder(mode, true).firstEncodedOffset(pair, 0, 4));
        }
    }

    public void testSplitPairsAndFinalLoneHighSurrogate() {
        for (JavaScriptEncoder.Mode mode : JavaScriptEncoder.Mode.values()) {
            for (boolean asciiOnly : new boolean[]{false, true}) {
                Encoder encoder = new JavaScriptEncoder(mode, asciiOnly);
                CharBuffer input = CharBuffer.allocate(2);
                CharBuffer output = CharBuffer.allocate(12);
                input.put('\ud83d').flip();
                assertSame(CoderResult.UNDERFLOW, encoder.encode(input, output, false));
                assertEquals(asciiOnly ? 1 : 0, input.position());
                assertEquals(asciiOnly ? 6 : 0, output.position());
                input.compact();
                input.put('\ude00').flip();
                assertSame(CoderResult.UNDERFLOW, encoder.encode(input, output, true));
                assertFalse(input.hasRemaining());
                output.flip();
                assertEquals(asciiOnly ? "\\ud83d\\ude00" : "\ud83d\ude00", output.toString());

                input.clear();
                output.clear();
                input.put('\ud83d').flip();
                assertSame(CoderResult.UNDERFLOW, encoder.encode(input, output, true));
                assertFalse(input.hasRemaining());
                output.flip();
                assertEquals("\\ud83d", output.toString());
            }
        }
    }

    public void testSurrogateOverflowIsAtomicWithSlicedBuffers() {
        for (JavaScriptEncoder.Mode mode : JavaScriptEncoder.Mode.values()) {
            for (boolean asciiOnly : new boolean[]{false, true}) {
                Encoder encoder = new JavaScriptEncoder(mode, asciiOnly);
                for (String value : new String[]{"\ud800", "\udfff", "\ud83d\ude00"}) {
                    CharBuffer input = CharBuffer.wrap(("!" + value + "!").toCharArray());
                    input.position(1).limit(1 + value.length());
                    input = input.slice();
                    int firstLength = !asciiOnly && value.length() == 2 ? 2 : 6;
                    CharBuffer output = CharBuffer.allocate(16);
                    output.position(3).limit(3 + firstLength - 1);
                    output = output.slice();
                    assertSame(CoderResult.OVERFLOW, encoder.encode(input, output, true));
                    assertEquals(0, input.position());
                    assertEquals(0, output.position());

                    output = CharBuffer.allocate(16);
                    output.position(3);
                    output = output.slice();
                    assertSame(CoderResult.UNDERFLOW, encoder.encode(input, output, true));
                    assertFalse(input.hasRemaining());
                    output.flip();
                    String expected = value.length() == 2
                        ? (asciiOnly ? "\\ud83d\\ude00" : value)
                        : unicodeEscape(value.charAt(0));
                    assertEquals(expected, output.toString());
                }
            }
        }
    }

    public void testEncodedWriterLookaheadAcrossFullOutputBuffer() throws Exception {
        String[] inputs = {"\ud800", "\udfff", "\udc00\ud800", "\ud83d\ude00",
            "\ud800\ud83d\ude00", "\ud800x", "\ud800\u0085$"};
        String[] unicode = {"\\ud800", "\\udfff", "\\udc00\\ud800", "\ud83d\ude00",
            "\\ud800\ud83d\ude00", "\\ud800x", "\\ud800\\x85\\x24"};
        for (JavaScriptEncoder.Mode mode : JavaScriptEncoder.Mode.values()) {
            for (boolean asciiOnly : new boolean[]{false, true}) {
                Encoder encoder = new JavaScriptEncoder(mode, asciiOnly);
                for (int length = EncodedWriter.BUFFER_SIZE - 6;
                        length <= EncodedWriter.BUFFER_SIZE + 1; length++) {
                    String prefix = repeat('a', length);
                    for (int c = 0; c < inputs.length; c++) {
                        String expected = prefix + (asciiOnly
                            ? unicode[c].replace("\ud83d\ude00", "\\ud83d\\ude00") : unicode[c]);
                        StringWriter out = new StringWriter();
                        EncodedWriter writer = new EncodedWriter(out, encoder);
                        writer.write(prefix);
                        for (int i = 0; i < inputs[c].length(); i++) {
                            char ch = inputs[c].charAt(i);
                            if (i % 2 == 0) {
                                writer.write(ch);
                            } else {
                                writer.write(new char[]{'!', ch, '!'}, 1, 1);
                            }
                            writer.write("");
                        }
                        writer.close();
                        assertEquals(encoder + ", prefix=" + length + ", case=" + c,
                            expected, out.toString());
                    }
                }
            }
        }
    }

    public void testFlushDoesNotFinalizePendingPair() throws Exception {
        for (JavaScriptEncoder.Mode mode : JavaScriptEncoder.Mode.values()) {
            StringWriter out = new StringWriter();
            EncodedWriter writer = new EncodedWriter(out, new JavaScriptEncoder(mode, false));
            writer.write("a\ud83d");
            writer.flush();
            assertEquals("a", out.toString());
            writer.write("\ude00");
            writer.close();
            assertEquals("a\ud83d\ude00", out.toString());
        }
    }

    public void testPublicStringAndWriterApisThroughUtf8() throws Exception {
        StringBuilder input = new StringBuilder("\u00a0\u00ff\ud83d\ude00");
        StringBuilder expected = new StringBuilder(input);
        for (int ch = 0x7f; ch <= 0x9f; ch++) {
            input.append((char) ch);
            expected.append(hexEscape(ch));
        }
        for (int ch = Character.MIN_SURROGATE; ch <= Character.MAX_SURROGATE; ch++) {
            // Separators keep every surrogate unpaired, including the range boundary.
            input.append((char) ch).append('|');
            expected.append(unicodeEscape(ch)).append('|');
        }
        String prefix = repeat('a', Encode.Buffer.INPUT_BUFFER_SIZE - 1);
        String value = prefix + input;
        String encoded = prefix + expected;
        for (String method : METHODS) {
            String actual = (String) Encode.class.getMethod(method, String.class).invoke(null, value);
            assertEquals(method, encoded, actual);
            assertEquals(method, encoded, utf8(actual));
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            Writer out = new OutputStreamWriter(bytes, StandardCharsets.UTF_8.newEncoder());
            Encode.class.getMethod(method, Writer.class, String.class).invoke(null, out, value);
            out.close();
            assertEquals(method, encoded, new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            // A valid pair straddling the facade's input-copy boundary must stay raw.
            String copyPrefix = repeat('a', Encode.Buffer.INPUT_BUFFER_SIZE - 2);
            String pair = "\\" + copyPrefix + "\ud83d\ude00\ud800";
            StringWriter pairOut = new StringWriter();
            Encode.class.getMethod(method, Writer.class, String.class).invoke(null, pairOut, pair);
            assertEquals("\\\\" + copyPrefix + "\ud83d\ude00\\ud800", pairOut.toString());
        }
    }

    private static String utf8(String value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Writer out = new OutputStreamWriter(bytes, StandardCharsets.UTF_8.newEncoder());
        out.write(value);
        out.close();
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String unicodeEscape(int ch) {
        return String.format("\\u%04x", ch);
    }

    private static String hexEscape(int ch) {
        return String.format("\\x%02x", ch);
    }

    private static String repeat(char ch, int length) {
        char[] chars = new char[length];
        java.util.Arrays.fill(chars, ch);
        return new String(chars);
    }
}

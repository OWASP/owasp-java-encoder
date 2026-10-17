// Copyright (c) 2026 OWASP.
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
import java.nio.CharBuffer;
import java.nio.charset.CoderResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import junit.framework.TestCase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/** Output-size and streaming regressions for terminal script-name escapes. */
public class JavaScriptBoundaryOptimizationTest extends TestCase {
    private static final String[] CONTEXTS = {
        Encoders.JAVASCRIPT, Encoders.JAVASCRIPT_BLOCK
    };
    private static final Encoder[] ENCODERS = {
        Encoders.JAVASCRIPT_ENCODER, Encoders.JAVASCRIPT_BLOCK_ENCODER
    };

    public void testCommonTextAvoidsNonterminalLetterEscapes() throws Exception {
        String[][] samples = {
            {"http://google.com", "http:\\/\\/google.com"},
            {"scripture", "scripture"},
            {"CIPRSTciprstx", "CIPRSTciprstx"},
            {"script", "scrip\\x74"},
            {"cat", "ca\\x74"},
            {"<script>", "\\x3cscript\\x3e"},
            {"</script>", "\\x3c\\/script\\x3e"},
            {"<!-- -->", "\\x3c\\x21\\x2d\\x2d\\x20\\x2d\\x2d\\x3e"},
            {"", ""},
            {null, "null"}
        };
        for (int mode = 0; mode < ENCODERS.length; mode++) {
            for (String[] sample : samples) {
                assertEquals(CONTEXTS[mode], sample[1], facade(mode, sample[0]));
                StringWriter output = new StringWriter();
                if (mode == 0) {
                    Encode.forJavaScript(output, sample[0]);
                } else {
                    Encode.forJavaScriptBlock(output, sample[0]);
                }
                assertEquals(CONTEXTS[mode], sample[1], output.toString());
                assertEquals(CONTEXTS[mode], sample[1],
                    Encode.encode(Encoders.forName(CONTEXTS[mode]), sample[0]));
            }
        }
        assertEquals("http://google.com", Encode.forJavaScriptSource("http://google.com"));
        assertEquals("http://google.com", Encode.forJavaScriptAttribute("http://google.com"));
    }

    public void testEveryTerminalScriptLetterIsEscapedOnlyInScriptModes() {
        for (char ch : "CIPRSTciprst".toCharArray()) {
            String input = "a" + ch;
            String expected = "a\\x" + Integer.toHexString(ch);
            assertEquals(expected, Encode.forJavaScript(input));
            assertEquals(expected, Encode.forJavaScriptBlock(input));
            assertEquals(input, Encode.forJavaScriptSource(input));
            assertEquals(input, Encode.forJavaScriptAttribute(input));
        }
    }

    public void testFastPathRespectsSubstringEndAndOffsets() {
        for (Encoder encoder : ENCODERS) {
            assertEquals(9, encoder.firstEncodedOffset("scripture", 0, 9));
            assertEquals(5, encoder.firstEncodedOffset("script", 0, 6));
            assertEquals(6, encoder.firstEncodedOffset("xScriptz", 1, 6));
            assertEquals(10, encoder.firstEncodedOffset("xscriptureY", 1, 9));
            assertEquals(1, encoder.firstEncodedOffset("xt!x", 1, 1));
            assertEquals(2, encoder.firstEncodedOffset("xs!x", 1, 2));
            assertEquals(2, encoder.firstEncodedOffset("xx", 2, 0));
        }
        String unchanged = new String("scripture");
        assertSame(unchanged, Encode.forJavaScript(unchanged));
        assertSame(unchanged, Encode.forJavaScriptBlock(unchanged));
    }

    public void testFacadeBuffersDoNotCreateAdditionalTerminalEscapes() throws Exception {
        for (int mode = 0; mode < ENCODERS.length; mode++) {
            for (int size : new int[]{1, 1023, 1024, 1025, 2047, 2048, 2049, 8193}) {
                String prefix = repeat('a', size - 1);
                assertEquals(prefix + "\\x74", facade(mode, prefix + "t"));
                String input = "!" + prefix + "ta";
                String expected = "\\x21" + prefix + "ta";
                assertEquals(expected, facade(mode, input));
                StringWriter output = new StringWriter();
                if (mode == 0) {
                    Encode.forJavaScript(output, input);
                } else {
                    Encode.forJavaScriptBlock(output, input);
                }
                assertEquals(expected, output.toString());
            }
            String outputOverflow = repeat('!', 2048) + "scripture";
            assertEquals(repeatEncoded("\\x21", 2048) + "scripture",
                facade(mode, outputOverflow));
        }
    }

    public void testLowLevelRetainsOneLetterUntilItKnowsTheInputEnds() {
        for (Encoder encoder : ENCODERS) {
            for (char ch : "CIPRSTciprst".toCharArray()) {
                CharBuffer input = CharBuffer.allocate(2);
                input.put(ch).flip();
                CharBuffer output = CharBuffer.allocate(16);
                assertTrue(encoder.encode(input, output, false).isUnderflow());
                assertEquals(0, input.position());
                assertEquals(0, output.position());
                input.compact();
                input.put('a').flip();
                assertTrue(encoder.encode(input, output, false).isUnderflow());
                assertFalse(input.hasRemaining());
                output.flip();
                assertEquals(ch + "a", output.toString());

                input.clear();
                input.put(ch).flip();
                output.clear();
                assertTrue(encoder.encode(input, output, false).isUnderflow());
                assertTrue(encoder.encode(input, output, true).isUnderflow());
                assertFalse(input.hasRemaining());
                output.flip();
                assertEquals("\\x" + Integer.toHexString(ch), output.toString());
            }
        }
    }

    public void testTerminalEscapeOverflowIsAtomic() {
        for (Encoder encoder : ENCODERS) {
            for (int capacity = 0; capacity < 4; capacity++) {
                CharBuffer input = CharBuffer.wrap(new char[]{'^', '^', 's', '^'});
                input.position(2).limit(3);
                char[] guarded = new char[capacity + 4];
                Arrays.fill(guarded, '^');
                CharBuffer output = CharBuffer.wrap(guarded);
                output.position(2).limit(capacity + 2);
                assertTrue(encoder.encode(input, output, true).isOverflow());
                assertEquals(2, input.position());
                assertEquals(2, output.position());
                for (char ch : guarded) {
                    assertEquals('^', ch);
                }
                output = CharBuffer.allocate(4);
                assertTrue(encoder.encode(input, output, true).isUnderflow());
                assertFalse(input.hasRemaining());
                output.flip();
                assertEquals("\\x73", output.toString());
            }
        }
    }

    public void testChunkedSlicesAndOutputOverflowsMatchFacade() {
        for (int mode = 0; mode < ENCODERS.length; mode++) {
            String input = "scripture script><!---->/cCatP";
            for (int chunk = 1; chunk <= input.length(); chunk++) {
                for (int capacity = 4; capacity <= 7; capacity++) {
                    assertEquals("chunk=" + chunk + ", capacity=" + capacity,
                        facade(mode, input), lowLevel(ENCODERS[mode], input, chunk, capacity));
                }
            }
            input = "s\u2028c\ud83d\ude00i";
            for (int chunk = 1; chunk <= input.length(); chunk++) {
                assertEquals(facade(mode, input), lowLevel(ENCODERS[mode], input, chunk, 6));
            }
        }
    }

    public void testEncodedWriterEverySplitMatchesFacade() throws Exception {
        for (int mode = 0; mode < ENCODERS.length; mode++) {
            for (String input : new String[]{"", "s", "script", "scripture",
                    "http://google.com", "</ScRiPt ><!-- -->", "s\u2028c\ud83d\ude00i"}) {
                for (int split = 0; split <= input.length(); split++) {
                    assertWriterSplit(mode, input, split);
                }
            }
            String large = "!" + repeat('a', 1022) + "scripture" + repeat('!', 1024) + "t";
            for (int split : new int[]{0, 1, 1023, 1024, 1025, 1032,
                    large.length() - 1, large.length()}) {
                assertWriterSplit(mode, large, split);
            }
            StringWriter output = new StringWriter();
            EncodedWriter writer = new EncodedWriter(output, CONTEXTS[mode]);
            for (char ch : large.toCharArray()) {
                writer.write(ch);
            }
            writer.close();
            assertEquals(facade(mode, large), output.toString());
        }
    }

    public void testFlushMakesTerminalLetterVisibleWithoutEndingTheStream() throws Exception {
        for (String context : CONTEXTS) {
            StringWriter output = new StringWriter();
            EncodedWriter writer = new EncodedWriter(output, context);
            writer.write("cat");
            writer.flush();
            writer.flush();
            writer.write("");
            writer.flush();
            assertEquals("ca\\x74", output.toString());
            writer.write("a");
            writer.flush();
            assertEquals("ca\\x74a", output.toString());
            writer.close();
            assertEquals("ca\\x74a", output.toString());

            output = new StringWriter();
            writer = new EncodedWriter(output, context);
            writer.write("cat");
            writer.flush();
            assertEquals("ca\\x74", output.toString());
            writer.close();
            assertEquals("ca\\x74", output.toString());
        }
    }

    public void testFlushEscapesAllScriptLettersAndLeavesOtherModesUnchanged() throws Exception {
        for (String context : CONTEXTS) {
            for (char ch : "CIPRSTciprst".toCharArray()) {
                StringWriter output = new StringWriter();
                EncodedWriter writer = new EncodedWriter(output, context);
                writer.write(ch);
                writer.flush();
                assertEquals("\\x" + Integer.toHexString(ch), output.toString());
                writer.write('a');
                writer.close();
                assertEquals("\\x" + Integer.toHexString(ch) + "a", output.toString());
            }
        }
        for (String context : new String[]{Encoders.JAVASCRIPT_ATTRIBUTE, Encoders.JAVASCRIPT_SOURCE}) {
            StringWriter output = new StringWriter();
            EncodedWriter writer = new EncodedWriter(output, context);
            writer.write("script");
            writer.flush();
            assertEquals("script", output.toString());
            writer.close();
            assertEquals("script", output.toString());
        }
    }

    public void testHtmlScriptFlushRepresentsSurrogatesWithoutChangingTheirValue() throws Exception {
        for (String context : CONTEXTS) {
            for (String pair : new String[]{"\ud800\udc00", "\ud83d\ude00", "\udbff\udfff"}) {
                String highEscape = "\\u" + Integer.toHexString(pair.charAt(0));
                String pairEscape = highEscape + "\\u" + Integer.toHexString(pair.charAt(1));
                StringWriter output = new StringWriter();
                EncodedWriter writer = new EncodedWriter(output, context);
                writer.write(pair.charAt(0));
                writer.flush();
                writer.flush();
                assertEquals(context, highEscape, output.toString());
                writer.write(pair.charAt(1));
                writer.close();
                assertEquals(context, pairEscape, output.toString());

                output = new StringWriter();
                writer = new EncodedWriter(output, context);
                writer.write(pair.charAt(0));
                writer.write(pair.charAt(1));
                writer.close();
                assertEquals("no explicit flush keeps raw pair: " + context, pair, output.toString());
            }
        }
    }

    public void testFlushRetainsSurrogateLookaheadInOtherContexts() throws Exception {
        for (String context : new String[]{Encoders.JAVASCRIPT_ATTRIBUTE, Encoders.JAVASCRIPT_SOURCE,
                Encoders.CDATA, Encoders.XML_COMMENT}) {
            StringWriter output = new StringWriter();
            EncodedWriter writer = new EncodedWriter(output, context);
            writer.write('\ud83d');
            writer.flush();
            writer.flush();
            assertEquals(context, "", output.toString());
            writer.write('\ude00');
            writer.close();
            assertEquals(context, "\ud83d\ude00", output.toString());
        }
    }

    public void testFlushHandlesInsufficientSpaceForTheCompletePendingEscape() throws Exception {
        for (String context : CONTEXTS) {
            for (String[] unit : new String[][]{{"s", "\\x73"}, {"\ud83d", "\\ud83d"}}) {
                for (int available = 0; available < unit[1].length(); available++) {
                    String prefix = repeat('a', EncodedWriter.BUFFER_SIZE - available);
                    StringWriter output = new StringWriter();
                    EncodedWriter writer = new EncodedWriter(output, context);
                    writer.write(prefix + unit[0]);
                    writer.flush();
                    assertEquals(prefix + unit[1], output.toString());
                    writer.write('a');
                    writer.close();
                    assertEquals(prefix + unit[1] + "a", output.toString());
                }
            }
        }
    }

    public void testFlushBeforeDirectTrustedSuffixCannotReorderALetterIntoAnEndTag() throws Exception {
        String prefix = "<script>var value=\"</scr";
        String suffix = "ipt><img id=pwn src=x>\";</script><p id=after>after</p>";
        Document intended = Jsoup.parse(prefix + "i" + suffix);
        assertNull("unencoded intended value has a different tag name", intended.getElementById("pwn"));
        Document reordered = Jsoup.parse(prefix + suffix);
        assertNotNull("negative control: omitting the pending letter closes script",
            reordered.getElementById("pwn"));
        for (String context : CONTEXTS) {
            StringWriter output = new StringWriter();
            output.write(prefix);
            EncodedWriter writer = new EncodedWriter(output, context);
            writer.write('i');
            writer.flush();
            output.write(suffix);
            writer.close();
            Document parsed = Jsoup.parse(output.toString());
            assertNull(context, parsed.getElementById("pwn"));
            assertEquals(context, 1, parsed.select("script").size());
            assertEquals(context, "var value=\"</scr\\x69ipt><img id=pwn src=x>\";",
                parsed.selectFirst("script").data());
            assertEquals(context, "after", parsed.getElementById("after").text());
        }
    }

    public void testFlushCannotWithholdASurrogateBarrierAfterRawScriptLetters() throws Exception {
        String prefix = "<script>var value=\"</scr";
        String suffix = "pt><img id=pwn src=x>\";</script><p id=after>after</p>";
        Document intended = Jsoup.parse(prefix + "i\ud800" + suffix);
        assertNull("surrogate prevents the intended value from matching script",
            intended.getElementById("pwn"));
        Document reordered = Jsoup.parse(prefix + "i" + suffix);
        assertNotNull("negative control: withholding the surrogate closes script",
            reordered.getElementById("pwn"));
        for (String context : CONTEXTS) {
            StringWriter output = new StringWriter();
            output.write(prefix);
            EncodedWriter writer = new EncodedWriter(output, context);
            writer.write("i\ud800");
            writer.flush();
            output.write(suffix);
            writer.close();
            Document parsed = Jsoup.parse(output.toString());
            assertNull(context, parsed.getElementById("pwn"));
            assertEquals(context, 1, parsed.select("script").size());
            assertEquals(context, "var value=\"</scri\\ud800pt><img id=pwn src=x>\";",
                parsed.selectFirst("script").data());
            assertEquals(context, "after", parsed.getElementById("after").text());
        }
    }

    public void testRealisticTagNameValuesAcrossEveryEncodingPathAndExplicitFlush() throws Exception {
        String[][] samples = {
            {"script", "scrip\\x74"},
            {"SCRIPT", "SCRIP\\x54"},
            {"scripts", "script\\x73"},
            {"scripture", "scripture"},
            {"script/", "script\\/"},
            {"script\t", "script\\t"}
        };
        for (int mode = 0; mode < ENCODERS.length; mode++) {
            for (String prefix : new String[]{"</", "<!--<"}) {
                for (String[] sample : samples) {
                    String label = CONTEXTS[mode] + " " + prefix + " " + sample[0];
                    String expected = prefix + sample[1] + ">";
                    assertEquals(label, sample[1], facade(mode, sample[0]));
                    assertScriptSpellingContained(label + " String", prefix + facade(mode, sample[0]) + ">");
                    assertEquals(label, sample[1],
                        Encode.encode(Encoders.forName(CONTEXTS[mode]), sample[0]));
                    assertScriptSpellingContained(label + " registry", prefix
                        + Encode.encode(Encoders.forName(CONTEXTS[mode]), sample[0]) + ">");
                    StringWriter output = new StringWriter();
                    output.write(prefix);
                    if (mode == 0) {
                        Encode.forJavaScript(output, sample[0]);
                    } else {
                        Encode.forJavaScriptBlock(output, sample[0]);
                    }
                    output.write('>');
                    assertEquals(label, expected, output.toString());
                    assertScriptSpellingContained(label + " Writer facade", output.toString());

                    String lowLevel = lowLevel(ENCODERS[mode], sample[0], 1, 6);
                    assertEquals(label, sample[1], lowLevel);
                    assertScriptSpellingContained(label + " low level", prefix + lowLevel + ">");

                    output = new StringWriter();
                    output.write(prefix);
                    EncodedWriter writer = new EncodedWriter(output, CONTEXTS[mode]);
                    for (char ch : sample[0].toCharArray()) {
                        writer.write(ch);
                    }
                    writer.flush();
                    assertEquals(label + " flush must expose the complete value",
                        prefix + sample[1], output.toString());
                    output.write('>');
                    assertEquals(label, expected, output.toString());
                    assertScriptSpellingContained(label + " flush before direct suffix", output.toString());
                    writer.close();
                    assertEquals(label + " close must not append a delayed letter", expected, output.toString());
                }
            }
        }
    }

    public void testExplicitFlushMayConservativelyEscapeInteriorLetters() throws Exception {
        String[][] fixtures = {
            {"scr", "ipture", "sc\\x72ipture"},
            {"cat", "apult", "ca\\x74apul\\x74"},
            {"http", "://google.com", "htt\\x70:\\/\\/google.com"},
            {"i\ud83d", "\ude00a", "i\\ud83d\\ude00a"}
        };
        for (String context : CONTEXTS) {
            for (String[] fixture : fixtures) {
                StringWriter output = new StringWriter();
                EncodedWriter writer = new EncodedWriter(output, context);
                writer.write(fixture[0]);
                writer.flush();
                writer.write(fixture[1]);
                writer.close();
                assertEquals(context, fixture[2], output.toString());
            }
        }
    }

    public void testSharedEncodersDoNotLeakPendingLettersBetweenThreads() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Void>> results = new ArrayList<Future<Void>>();
            for (int worker = 0; worker < 8; worker++) {
                results.add(pool.submit(new Callable<Void>() {
                    public Void call() {
                        for (int pass = 0; pass < 100; pass++) {
                            for (int mode = 0; mode < ENCODERS.length; mode++) {
                                CharBuffer input = CharBuffer.wrap(new char[]{'s'});
                                CharBuffer output = CharBuffer.allocate(4);
                                assertTrue(ENCODERS[mode].encode(input, output, false).isUnderflow());
                                assertEquals("scripture", facade(mode, "scripture"));
                                assertEquals("\\x69", facade(mode, "i"));
                                assertTrue(ENCODERS[mode].encode(input, output, true).isUnderflow());
                                output.flip();
                                assertEquals("\\x73", output.toString());
                            }
                        }
                        return null;
                    }
                }));
            }
            for (Future<Void> result : results) {
                result.get();
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static String facade(int mode, String input) {
        return mode == 0 ? Encode.forJavaScript(input) : Encode.forJavaScriptBlock(input);
    }

    private static void assertScriptSpellingContained(String label, String value) {
        String source = "var value=\"" + value + "\";";
        Document parsed = Jsoup.parse("<script>" + source + "</script><p id=after>after</p>");
        assertEquals(label, 1, parsed.select("script").size());
        assertEquals(label, source, parsed.selectFirst("script").data());
        assertNotNull(label, parsed.getElementById("after"));
        assertEquals(label, "after", parsed.getElementById("after").text());
    }

    private static String repeat(char ch, int count) {
        char[] value = new char[count];
        Arrays.fill(value, ch);
        return new String(value);
    }

    private static String repeatEncoded(String value, int count) {
        StringBuilder result = new StringBuilder(value.length() * count);
        for (int i = 0; i < count; i++) {
            result.append(value);
        }
        return result.toString();
    }

    private static void assertWriterSplit(int mode, String input, int split) throws Exception {
        StringWriter output = new StringWriter();
        EncodedWriter writer = new EncodedWriter(output, CONTEXTS[mode]);
        writer.write(input.substring(0, split));
        writer.write(input.substring(split));
        writer.close();
        assertEquals("split=" + split, facade(mode, input), output.toString());
    }

    private static String lowLevel(Encoder encoder, String value, int chunk, int capacity) {
        char[] guardedInput = new char[value.length() + 4];
        Arrays.fill(guardedInput, '^');
        value.getChars(0, value.length(), guardedInput, 2);
        CharBuffer parent = CharBuffer.wrap(guardedInput);
        parent.position(1).limit(value.length() + 3);
        CharBuffer input = parent.slice();
        input.position(1).limit(1);
        StringBuilder encoded = new StringBuilder();
        int end = 1;
        while (true) {
            end = Math.min(end + chunk, value.length() + 1);
            input.limit(end);
            boolean last = end == value.length() + 1;
            while (true) {
                char[] guardedOutput = new char[capacity + 4];
                Arrays.fill(guardedOutput, '^');
                parent = CharBuffer.wrap(guardedOutput);
                parent.position(1).limit(capacity + 3);
                CharBuffer output = parent.slice();
                output.position(1).limit(capacity + 1);
                CoderResult result = encoder.encode(input, output, last);
                encoded.append(guardedOutput, 2, output.position() - 1);
                assertEquals('^', guardedOutput[0]);
                assertEquals('^', guardedOutput[1]);
                for (int i = output.position() + 1; i < guardedOutput.length; i++) {
                    assertEquals('^', guardedOutput[i]);
                }
                if (result.isUnderflow()) {
                    break;
                }
                assertTrue(result.isOverflow());
                assertTrue(output.position() > 1);
            }
            if (last) {
                assertFalse(input.hasRemaining());
                assertEquals('^', guardedInput[0]);
                assertEquals('^', guardedInput[1]);
                assertEquals('^', guardedInput[value.length() + 2]);
                assertEquals('^', guardedInput[value.length() + 3]);
                return encoded.toString();
            }
        }
    }
}

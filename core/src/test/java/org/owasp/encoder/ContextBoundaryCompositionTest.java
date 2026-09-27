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

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.nio.charset.CoderResult;
import java.util.Arrays;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import junit.framework.TestCase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/** Parser-oracle tests for delimiters split at a trusted/encoded boundary. */
public class ContextBoundaryCompositionTest extends TestCase {

    public void testJavaScriptEndTagPrefixesRemainScriptData() {
        String[] delimiters = {">", " ", "\t", "\n", "\f", "\r", "/"};
        for (String sentinel : scriptEndTagCaseVariants()) {
            for (String delimiter : delimiters) {
                String token = sentinel + delimiter
                    + (">".equals(delimiter) ? "" : ">");
                for (int start = 0; start < token.length(); start++) {
                    for (int end = start + 1; end <= token.length(); end++) {
                        String trusted = token.substring(0, start);
                        String attacker = token.substring(start, end);
                        String trustedSuffix = token.substring(end)
                            + "<img id=pwn src=x>";
                        String position = sentinel + " " + delimiter + " "
                            + start + ":" + end;
                        assertScriptContained("general " + position, trusted,
                            Encode.forJavaScript(attacker), trustedSuffix);
                        assertScriptContained("block " + position, trusted,
                            Encode.forJavaScriptBlock(attacker), trustedSuffix);
                    }
                }

                Document negative = Jsoup.parse("<script>var value=\"" + token
                    + "<img id=pwn src=x>\";</script>");
                assertEquals("negative control " + sentinel + " " + delimiter,
                    1, negative.select("img#pwn").size());
            }
        }
    }

    public void testJavaScriptCommentOpenPrefixesAreBroken() {
        String token = "<!--";
        for (int start = 0; start < token.length(); start++) {
            for (int end = start + 1; end <= token.length(); end++) {
                String general = token.substring(0, start)
                    + Encode.forJavaScript(token.substring(start, end))
                    + token.substring(end);
                String block = token.substring(0, start)
                    + Encode.forJavaScriptBlock(token.substring(start, end))
                    + token.substring(end);
                assertFalse("general " + start + ":" + end,
                    general.contains(token));
                assertFalse("block " + start + ":" + end,
                    block.contains(token));
            }
        }
        assertTrue("negative control", token.contains("<!--"));
    }

    public void testJavaScriptCommentClosePrefixesRemainScriptData() {
        String token = "-->";
        for (int start = 0; start < token.length(); start++) {
            for (int end = start + 1; end <= token.length(); end++) {
                String trusted = "<!--<script " + token.substring(0, start);
                String trustedSuffix = token.substring(end)
                    + "</script><img id=pwn src=x>";
                String position = start + ":" + end;
                assertScriptContained("general comment-close " + position,
                    trusted, Encode.forJavaScript(token.substring(start, end)),
                    trustedSuffix);
                assertScriptContained("block comment-close " + position,
                    trusted,
                    Encode.forJavaScriptBlock(token.substring(start, end)),
                    trustedSuffix);
            }
        }

        Document negative = Jsoup.parse("<script>var value=\"<!--<script "
            + token + "</script><img id=pwn src=x>\";</script>");
        assertEquals("negative control", 1, negative.select("img#pwn").size());
    }

    public void testJavaScriptBoundaryWriterAndRegistryPaths() throws Exception {
        String input = "script><img id=pwn src=x>";
        assertEquals(Encode.forJavaScript(input), writerJavaScript(input, false));
        assertEquals(Encode.forJavaScriptBlock(input), writerJavaScript(input, true));
        assertEquals(Encode.forJavaScript(input),
            Encode.encode(Encoders.forName(Encoders.JAVASCRIPT), input));
        assertEquals(Encode.forJavaScriptBlock(input),
            Encode.encode(Encoders.forName(Encoders.JAVASCRIPT_BLOCK), input));
        assertScriptContained("general writer", "</", writerJavaScript(input, false), "");
        assertScriptContained("block writer", "</", writerJavaScript(input, true), "");
    }

    public void testBoundaryContextsThroughLowLevelEncoderWindows() {
        String javascript = "</ScRiPt ><!-- -->";
        assertEquals(Encode.forJavaScript(javascript),
            lowLevelEncode(Encoders.JAVASCRIPT_ENCODER, javascript, 4));
        assertEquals(Encode.forJavaScriptBlock(javascript),
            lowLevelEncode(Encoders.JAVASCRIPT_BLOCK_ENCODER, javascript, 4));

        String cdata = "]]>]><safe";
        assertEquals(Encode.forCDATA(cdata),
            lowLevelEncode(Encoders.CDATA_ENCODER, cdata, 13));

        String comment = "a--->b";
        assertEquals(Encode.forXmlComment(comment),
            lowLevelEncode(Encoders.XML_COMMENT_ENCODER, comment, 1));
    }

    public void testAtomicReplacementCapacitiesAndSentinels() {
        assertAtomicReplacement(Encoders.JAVASCRIPT_ENCODER, 's', "\\x73");
        assertAtomicReplacement(Encoders.JAVASCRIPT_BLOCK_ENCODER, '-', "\\x2d");
        assertAtomicReplacement(Encoders.CDATA_ENCODER, ']', "]]>]<![CDATA[");
        assertAtomicReplacement(Encoders.CDATA_ENCODER, '>', "]]><![CDATA[>");
        assertAtomicReplacement(Encoders.XML_COMMENT_ENCODER, '-', "~");
    }

    public void testEncodedWriterEverySplitAndFlush() throws Exception {
        assertEncodedWriterSplits(Encoders.JAVASCRIPT, "</ScRiPt ><!-- -->",
            Encode.forJavaScript("</ScRiPt ><!-- -->"));
        assertEncodedWriterSplits(Encoders.JAVASCRIPT_BLOCK, "</ScRiPt ><!-- -->",
            Encode.forJavaScriptBlock("</ScRiPt ><!-- -->"));
        assertEncodedWriterSplits(Encoders.CDATA, "]]>]><safe", Encode.forCDATA("]]>]><safe"));
        assertEncodedWriterSplits(Encoders.XML_COMMENT, "a--->b",
            Encode.forXmlComment("a--->b"));
    }

    public void testEncodedWriterFlushDoesNotFinalizeSurrogate() throws Exception {
        String pair = "\ud83d\ude00";
        for (String context : new String[] {Encoders.JAVASCRIPT,
                Encoders.JAVASCRIPT_BLOCK, Encoders.CDATA, Encoders.XML_COMMENT}) {
            StringWriter output = new StringWriter();
            EncodedWriter writer = new EncodedWriter(output, context);
            writer.write(pair.charAt(0));
            writer.flush();
            writer.write(pair.charAt(1));
            writer.close();
            assertEquals(context, Encode.encode(Encoders.forName(context), pair),
                output.toString());
        }
    }

    public void testCdataTrustedPrefixPreservesTextAndStructure() throws Exception {
        String token = "]]>";
        String markup = "<evil id='middle-pwn'/>";
        for (int start = 0; start < token.length(); start++) {
            for (int end = start + 1; end <= token.length(); end++) {
                String composed = token.substring(0, start)
                    + Encode.forCDATA(token.substring(start, end))
                    + token.substring(end) + markup;
                org.w3c.dom.Document splitParsed = parseXml(
                    "<?xml version='1.0'?><r><![CDATA[" + composed + "]]></r>");
                assertEquals("CDATA split " + start + ":" + end, 0,
                    splitParsed.getElementsByTagName("evil").getLength());
                assertEquals("CDATA value " + start + ":" + end, token + markup,
                    splitParsed.getDocumentElement().getTextContent());
            }
        }

        String attacker = "><evil id='pwn'/><![CDATA[";
        String encoded = Encode.forCDATA(attacker);
        org.w3c.dom.Document parsed = parseXml(
            "<?xml version='1.0'?><r><![CDATA[]]" + encoded + "]]></r>");
        assertEquals(0, parsed.getElementsByTagName("evil").getLength());
        assertEquals("]]" + attacker, parsed.getDocumentElement().getTextContent());

        org.w3c.dom.Document raw = parseXml(
            "<?xml version='1.0'?><r><![CDATA[]]>"
                + "<evil id='raw-pwn'/><![CDATA[]]></r>");
        assertEquals("raw CDATA negative control", 1,
            raw.getElementsByTagName("evil").getLength());

        assertEquals(encoded, writerXml(attacker, Encoders.CDATA));
        assertEquals(encoded, Encode.encode(Encoders.forName(Encoders.CDATA), attacker));
    }

    public void testXmlCommentTrustedPrefixCannotCloseComment() throws Exception {
        String attacker = "-><evil id='pwn'/>";
        String encoded = Encode.forXmlComment(attacker);
        org.w3c.dom.Document parsed = parseXml(
            "<?xml version='1.0'?><r><!---" + encoded + "--></r>");
        assertEquals(0, parsed.getElementsByTagName("evil").getLength());

        assertEquals("~x", Encode.forXmlComment("-x"));
        parseXml("<?xml version='1.0'?><r><!---"
            + Encode.forXmlComment("-x") + "--></r>");

        boolean rawFailed = false;
        try {
            parseXml("<?xml version='1.0'?><r><!----x--></r>");
        } catch (SAXParseException expected) {
            rawFailed = true;
        }
        assertTrue("raw trusted-prefix negative control", rawFailed);

        String middle = "<?xml version='1.0'?><r><!---"
            + Encode.forXmlComment("-") + "><evil id='middle-pwn'/>--></r>";
        parsed = parseXml(middle);
        assertEquals(0, parsed.getElementsByTagName("evil").getLength());

        org.w3c.dom.Document rawInjection = parseXml(
            "<?xml version='1.0'?><r><!---->"
                + "<evil id='raw-pwn'/><!----></r>");
        assertEquals("raw comment negative control", 1,
            rawInjection.getElementsByTagName("evil").getLength());

        String[] forbidden = {"--", "-->"};
        for (String token : forbidden) {
            for (int start = 0; start <= 1; start++) {
                for (int end = start + 1; end <= token.length(); end++) {
                    String comment = token.substring(0, start)
                        + Encode.forXmlComment(token.substring(start, end))
                        + token.substring(end) + "<evil id='split-pwn'/>";
                    parsed = parseXml("<?xml version='1.0'?><r><!--"
                        + comment + "--></r>");
                    assertEquals(token + " " + start + ":" + end, 0,
                        parsed.getElementsByTagName("evil").getLength());
                }
            }
        }

        assertEquals(encoded, writerXml(attacker, Encoders.XML_COMMENT));
        assertEquals(encoded,
            Encode.encode(Encoders.forName(Encoders.XML_COMMENT), attacker));
    }

    private static void assertScriptContained(String message, String trusted,
            String encoded, String trustedSuffix) {
        Document document = Jsoup.parse("<script>var value=\"" + trusted + encoded
            + trustedSuffix + "\";</script><p id=after>after</p>");
        assertEquals(message, 0, document.select("img#pwn").size());
        assertEquals(message, 1, document.select("script").size());
        assertEquals(message, "after", document.getElementById("after").text());
    }

    private static String writerJavaScript(String input, boolean block)
            throws Exception {
        StringWriter output = new StringWriter();
        if (block) {
            Encode.forJavaScriptBlock(output, input);
        } else {
            Encode.forJavaScript(output, input);
        }
        return output.toString();
    }

    private static String writerXml(String input, String context) throws Exception {
        StringWriter output = new StringWriter();
        if (Encoders.CDATA.equals(context)) {
            Encode.forCDATA(output, input);
        } else {
            Encode.forXmlComment(output, input);
        }
        return output.toString();
    }

    private static String[] scriptEndTagCaseVariants() {
        String letters = "script";
        String[] variants = new String[1 << letters.length()];
        for (int mask = 0; mask < variants.length; mask++) {
            StringBuilder value = new StringBuilder("</");
            for (int i = 0; i < letters.length(); i++) {
                char ch = letters.charAt(i);
                value.append((mask & (1 << i)) == 0 ? ch : Character.toUpperCase(ch));
            }
            variants[mask] = value.toString();
        }
        return variants;
    }

    private static String lowLevelEncode(Encoder encoder, String value, int outputCapacity) {
        char[] inputArray = new char[value.length() + 4];
        Arrays.fill(inputArray, '^');
        value.getChars(0, value.length(), inputArray, 2);
        CharBuffer inputParent = CharBuffer.wrap(inputArray);
        inputParent.position(1).limit(value.length() + 3);
        CharBuffer input = inputParent.slice();
        input.position(1).limit(value.length() + 1);

        StringBuilder encoded = new StringBuilder();
        while (true) {
            char[] outputArray = new char[outputCapacity + 4];
            Arrays.fill(outputArray, '^');
            CharBuffer outputParent = CharBuffer.wrap(outputArray);
            outputParent.position(1).limit(outputCapacity + 3);
            CharBuffer output = outputParent.slice();
            output.position(1).limit(outputCapacity + 1);
            int previousInputPosition = input.position();

            CoderResult result = encoder.encode(input, output, true);
            encoded.append(outputArray, 2, output.position() - 1);
            assertEquals("leading output sentinel", '^', outputArray[0]);
            assertEquals("leading output sentinel", '^', outputArray[1]);
            assertEquals("trailing output sentinel", '^',
                outputArray[output.position() + 1]);

            if (result.isUnderflow()) {
                assertFalse(input.hasRemaining());
                break;
            }
            assertTrue(result.isOverflow());
            assertTrue("overflow must make progress with an atomic-size buffer",
                input.position() > previousInputPosition);
        }
        assertEquals("leading input sentinel", '^', inputArray[0]);
        assertEquals("leading input sentinel", '^', inputArray[1]);
        assertEquals("trailing input sentinel", '^', inputArray[inputArray.length - 1]);
        return encoded.toString();
    }

    private static void assertAtomicReplacement(Encoder encoder, char value,
            String expected) {
        int[] capacities = {expected.length() - 1, expected.length(),
            expected.length() + 1};
        for (int capacity : capacities) {
            char[] inputArray = {'^', '^', '^', value, '^', '^'};
            CharBuffer inputParent = CharBuffer.wrap(inputArray);
            inputParent.position(2).limit(5);
            CharBuffer input = inputParent.slice();
            input.position(1).limit(2);

            char[] outputArray = new char[capacity + 6];
            Arrays.fill(outputArray, '^');
            CharBuffer outputParent = CharBuffer.wrap(outputArray);
            outputParent.position(2).limit(capacity + 4);
            CharBuffer output = outputParent.slice();
            output.position(1).limit(capacity + 1);

            CoderResult result = encoder.encode(input, output, true);
            if (capacity < expected.length()) {
                assertTrue(encoder.toString(), result.isOverflow());
                assertEquals(1, input.position());
                assertEquals(1, output.position());
            } else {
                assertTrue(encoder.toString(), result.isUnderflow());
                assertEquals(2, input.position());
                assertEquals(expected.length() + 1, output.position());
                assertEquals(expected, new String(outputArray, 3, expected.length()));
            }

            int written = capacity < expected.length() ? 0 : expected.length();
            for (int i = 0; i < outputArray.length; i++) {
                if (i < 3 || i >= 3 + written) {
                    assertEquals("sentinel " + encoder + " capacity " + capacity
                        + " index " + i, '^', outputArray[i]);
                }
            }
        }
    }

    private static void assertEncodedWriterSplits(String context, String input,
            String expected) throws Exception {
        for (int split = 0; split <= input.length(); split++) {
            StringWriter output = new StringWriter();
            EncodedWriter writer = new EncodedWriter(output, context);
            writer.write(input, 0, split);
            writer.flush();
            writer.write(input, split, input.length() - split);
            writer.close();
            assertEquals(context + " split " + split, expected, output.toString());
        }

        StringWriter output = new StringWriter();
        EncodedWriter writer = new EncodedWriter(output, context);
        for (int i = 0; i < input.length(); i++) {
            writer.write(input.charAt(i));
            writer.flush();
        }
        writer.close();
        assertEquals(context + " one-character chunks", expected, output.toString());
    }

    private static org.w3c.dom.Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler() {
            @Override
            public void error(SAXParseException exception) throws SAXParseException {
                throw exception;
            }

            @Override
            public void fatalError(SAXParseException exception)
                    throws SAXParseException {
                throw exception;
            }
        });
        return builder.parse(new InputSource(new StringReader(xml)));
    }
}

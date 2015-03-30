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

import java.io.CharArrayWriter;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.CoderResult;
import java.util.BitSet;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * EncoderTestSuiteBuilder -- builder of test suites for the encoders.
 * Allows fluent construction of a test suite by specifying which
 * code-points are not encoded, which are invalid, and the expected
 * encodings for escaped characters.
 *
 * @author Jeff Ichnowski
 */
public class EncoderTestSuiteBuilder {
    /** This is the test suite that is being built. */
    private TestSuite _suite;
    /**
     * If a test flagged by {@link #mark()}, this is the active suite of
     * marked tests.
     */
    private TestSuite _markedSuite;
    /** The encoder being tested. */
    private Encoder _encoder;
    /**
     * A character sequence that is valid and not escaped by the encoder.
     * It is used to surround (prefix and suffix) test inputs.
     */
    private String _safeAffix;
    /**
     * A character sequence that is escaped by the encoder.
     */
    private String _unsafeAffix;

    /**
     * The set of all valid, un-escaped characters.
     */
    private BitSet _valid = new BitSet();
    /**
     * The set of all invalid characters.
     */
    private BitSet _invalid = new BitSet();
    /**
     * The set of all valid characters requiring escapes.
     */
    private BitSet _encoded = new BitSet();

    /**
     * Creates an builder for the specified encoder.
     *
     * @param encoder the encoder to test
     * @param safeAffix the value for {@link #_safeAffix}
     * @param unsafeAffix the value for {@link #_unsafeAffix}
     */
    public EncoderTestSuiteBuilder(Encoder encoder, String safeAffix, String unsafeAffix) {
        _suite = new TestSuite(encoder.toString());
        _encoder = encoder;
        _safeAffix = safeAffix;
        _unsafeAffix = unsafeAffix;
    }

    /**
     * Like the {@link #EncoderTestSuiteBuilder(Encoder, String, String)},
     * but with a class that has "testXYZ()" methods in it too.  The test
     * methods will be run first.
     *
     * @param suiteClass the test class with the "testXYZ()" methods.
     * @param encoder the encoder to test
     * @param safeAffix the value for {@link #_safeAffix}
     * @param unsafeAffix the value for {@link #_unsafeAffix}
     */
    public EncoderTestSuiteBuilder(Class<? extends TestCase> suiteClass, Encoder encoder, String safeAffix, String unsafeAffix) {
        _suite = new TestSuite(suiteClass);
        _encoder = encoder;
        _safeAffix = safeAffix;
        _unsafeAffix = unsafeAffix;
    }

    /**
     * Adds a single test to the suite.
     *
     * @param test the test to add
     * @return this.
     */
    public EncoderTestSuiteBuilder add(Test test) {
        _suite.addTest(test);
        return this;
    }

    /**
     * Java/JavaScript-style encoder for helping debug unit test results.  We
     * should not rely upon the code being tested for helping--not that it
     * would hurt the testing, only it might effect coverage metrics.
     *
     * @param input the input to encode
     * @return the encoded input
     */
    static String debugEncode(String input) {
        final int n = input.length();
        StringBuilder buf = new StringBuilder(n*2);
        for (int i=0 ; i<n ; ++i) {
            char ch = input.charAt(i);
            switch (ch) {
            case '\\': buf.append("\\\\"); break;
            case '\'': buf.append("\\\'"); break;
            case '\"': buf.append("\\\""); break;
            case '\r': buf.append("\\r"); break;
            case '\n': buf.append("\\n"); break;
            case '\t': buf.append("\\t"); break;
            default:
                if (' ' <= ch && ch <= '~') {
                    buf.append(ch);
                } else {
                    buf.append(String.format("\\u%04x", (int) ch));
                }
                break;
            }
        }
        return buf.toString();
    }

    /**
     * Extended version of {@link junit.framework.Assert#assertEquals(String,String)}.
     * It will try encodings by surrounding the input with safe and unsafe
     * prefixes and suffixes.  It will also check additional assertions about
     * the behavior of the encoders.
     *
     * @param expected the expected encoding
     * @param input the input to encode
     * @throws IOException from the signature of a Writer used internally.
     * Not actually thrown since the writer is an in-memory writer.
     */
    void checkEncode(final String expected, final String input)
        throws IOException
    {
        // Check the .encode call
        String actual = Encode.encode(_encoder, input);

        if (!expected.equals(actual)) {
            Assert.assertEquals("encode(\""+ debugEncode(input) +"\")", expected, actual);
        }

        if (input.equals(actual)) {
            // test that the input string is returned unmodified if
            // the input was not escaped.  This insures that we're
            // not allocating objects unnecessarily.
            Assert.assertSame(input, actual);
        }

        // Check the encodeTo variants (at offset 0)
        TestWriter testWriter = new TestWriter(input);
        EncodedWriter encodedWriter = new EncodedWriter(testWriter, _encoder);
        encodedWriter.write(input);
        encodedWriter.close();
        actual = testWriter.toString();
        if (!expected.equals(actual)) {
            Assert.assertEquals("encodeTo(\""+debugEncode(input)+"\",int,int,Writer)", expected, actual);
        }

        // Check the encodeTo variants (at offset 3)
        String offsetInput = "\0\0\0" + input + "\0\0\0";
        testWriter = new TestWriter(offsetInput);
        encodedWriter = new EncodedWriter(testWriter, _encoder);
        encodedWriter.write(offsetInput.toCharArray(), 3, input.length());
        encodedWriter.close();
        actual = testWriter.toString();
        if (!expected.equals(actual)) {
            Assert.assertEquals("encodeTo([..."+debugEncode(input)+"...],int,int,Writer)", expected, actual);
        }

        // Check boundary conditions on CharBuffer encodes
        checkBoundaryEncodes(expected, input);
    }

    /**
     * Checks boundary conditions of CharBuffer based encodes.
     *
     * @param expected the expected output
     * @param input the input to encode
     */
    private void checkBoundaryEncodes(String expected, String input) {
        final CharBuffer in = CharBuffer.wrap(input.toCharArray());
        final int n = expected.length();
        final CharBuffer out = CharBuffer.allocate(n);
        for (int i=0 ; i<n ; ++i) {
            out.clear();
            out.position(n - i);
            in.clear();

            CoderResult cr = _encoder.encode(in, out, true);
            out.limit(out.position()).position(n - i);
            out.compact();
            if (cr.isOverflow()) {
                CoderResult cr2 = _encoder.encode(in, out, true);
                if (!cr2.isUnderflow()) {
                    Assert.fail("second encode should finish at offset = "+i);
                }
            }
            out.flip();

            String actual = out.toString();
            if (!expected.equals(actual)) {
                Assert.assertEquals("offset = "+i, expected, actual);
            }
        }
    }

    /**
     * Tells the suite builder that for the given input it should expect the
     * given encoded output.  To be used only for input that is escaped by
     * the encoder.
     *
     * @param expected the expected output.
     * @param input the input to encode.
     * @return this.
     */
    public EncoderTestSuiteBuilder encode(final String expected, final String input) {
        return encode("input: "+input, expected, input);
    }

    /**
     * Tells the suite builder that for the given input it should expect the
     * given encoded output.  To be used only for input that is escaped by
     * the encoder.
     *
     * @param name the name of the test (for junit reports)
     * @param expected the expected output
     * @param input the input to encode.
     * @return this.
     */
    public EncoderTestSuiteBuilder encode(String name, final String expected, final String input) {
        return add(new TestCase(name) {
            @Override
            protected void runTest() throws Throwable {
                // test input directly
                checkEncode(expected, input);

                // test input surrounded by safe characters
                checkEncode(_safeAffix + expected, _safeAffix + input);
                checkEncode(expected + _safeAffix, input + _safeAffix);
                checkEncode(_safeAffix + expected + _safeAffix,
                    _safeAffix + input + _safeAffix);

                // test input surrounded by characters needing escape
                String escapedAffix = Encode.encode(_encoder, _unsafeAffix);
                checkEncode(escapedAffix + expected, _unsafeAffix + input);
                checkEncode(expected + escapedAffix, input + _unsafeAffix);
                checkEncode(escapedAffix + expected + escapedAffix,
                    _unsafeAffix + input + _unsafeAffix);
            }
        });
    }

    /**
     * Tells the builder that any character in the input string is "invalid"--
     * and thus is not to appear in the output either encoded or unescaped.
     *
     * @param chars the set of invalid characters.
     */
    public void invalid(String chars) {
        for (int i=0, n=chars.length() ; i<n ; ++i) {
            char ch = chars.charAt(i);
            _invalid.set(ch);
            _valid.clear(ch);
            _encoded.clear(ch);
        }
    }

    /**
     * Tells the builder that a character range is invalid.
     *
     * @param min the minimum code-point (inclusive)
     * @param max the maximum code-point (inclusive)
     * @return this.
     */
    public EncoderTestSuiteBuilder invalid(int min, int max) {
        _invalid.set(min, max+1);
        _valid.clear(min, max+1);
        _encoded.clear(min, max+1);
        return this;
    }

    /**
     * Tells the builder that a character set is valid and unescaped.
     *
     * @param chars the character set of valid, unescaped characters.
     * @return this.
     */
    public EncoderTestSuiteBuilder valid(String chars) {
        for (int i=0, n=chars.length() ; i<n ; ++i) {
            char ch = chars.charAt(i);
            _valid.set(ch);
            _invalid.clear(ch);
            _encoded.clear(ch);
        }
        return this;
    }

    /**
     * Tells the builder that a range of code-points is valid.
     *
     * @param min the minimum (inclusive)
     * @param max the maximum (inclusive)
     * @return this.
     */
    public EncoderTestSuiteBuilder valid(int min, int max) {
        _valid.set(min, max+1);
        _invalid.clear(min, max+1);
        _encoded.clear(min, max+1);
        return this;
    }

    /**
     * Tells the builder that a set of characters is encoded.
     *
     * @param chars the encoded characters
     * @return this
     */
    public EncoderTestSuiteBuilder encoded(String chars) {
        for (int i=0, n=chars.length() ; i<n ; ++i) {
            char ch = chars.charAt(i);
            _encoded.set(ch);
            _valid.clear(ch);
            _invalid.clear(ch);
        }
        return this;
    }

    /**
     * Tells the builder that a range of characters is encoded.
     *
     * @param min the minimum (inclusive)
     * @param max the maximum (inclusive)
     * @return this
     */
    public EncoderTestSuiteBuilder encoded(int min, int max) {
        _encoded.set(min, max+1);
        _valid.clear(min, max+1);
        _invalid.clear(min, max+1);
        return this;
    }

    /**
     * Creates and adds a test suite of valid, unescaped characters, to
     * the test suite.  Must be called after telling the builder which
     * characters are valid, invalid, and encoded.
     *
     * @return this.
     */
    public EncoderTestSuiteBuilder validSuite() {
        int cardinality = _encoded.cardinality() + _invalid.cardinality() + _valid.cardinality();
        if (cardinality != Character.MAX_CODE_POINT + 1) {
            throw new AssertionError("incomplete coverage: "+cardinality+" != "+(Character.MAX_CODE_POINT+1));
        }

        TestSuite suite = new TestSuite("valid");

        int min = _valid.nextSetBit(0);
        while (min != -1) {
            int max = _valid.nextClearBit(min+1);
            if (max == -1) {
                max = Character.MAX_CODE_POINT + 1;
            }
            final int finalMin = min;
            final int finalMax = max;
            suite.addTest(new TestCase(String.format("U+%04X..U+%04X", finalMin, finalMax - 1)) {
                @Override
                protected void runTest() throws Throwable {
                    char[] chars = new char[2];
                    for (int i= finalMin; i<finalMax; ++i) {
                        String input = new String(chars, 0, Character.toChars(i, chars, 0));
                        checkEncode(input, input);
                    }
                }
            });
            min = _valid.nextSetBit(max+1);
        }

        return add(suite);
    }

    /**
     * Creates an adds a test suite for the invalid characters.  Must be
     * called after telling the builder which characters are valid, invalid,
     * and encoded.
     *
     * @param invalidChar the replacement to expect when an invalid character
     * is encountered in the input.
     * @return this
     */
    public EncoderTestSuiteBuilder invalidSuite(char invalidChar) {
        assert _encoded.cardinality() + _invalid.cardinality() + _valid.cardinality() == Character.MAX_CODE_POINT + 1;

        final String invalidString = String.valueOf(invalidChar);

        TestSuite suite = new TestSuite("invalid");
        int min = _invalid.nextSetBit(0);
        while (min != -1) {
            int max = _invalid.nextClearBit(min+1);
            if (max < 0) {
                max = Character.MAX_CODE_POINT + 1;
            }
            final int finalMin = min;
            final int finalMax = max;
            suite.addTest(new TestCase(String.format("U+%04x..U+%04X", finalMin, finalMax - 1)) {
                @Override
                protected void runTest() throws Throwable {
                    char[] chars = new char[2];
                    for (int i = finalMin; i < finalMax; ++i) {
                        String input = new String(chars, 0, Character.toChars(i, chars, 0));
                        String actual = Encode.encode(_encoder, input);
                        if (!invalidString.equals(actual)) {
                            assertEquals("\"" + debugEncode(input) + "\" actual=" + debugEncode(actual), invalidString, actual);
                        }
                        checkBoundaryEncodes(invalidString, input);
                    }
                }
            });
            min = _invalid.nextSetBit(max+1);
        }

        return add(suite);
    }

    /**
     * Creates and adds a test suite for characters that are encoded.  Must
     * be called after telling the builder which characters are valid,
     * invalid, and encoded.  The added suite simply tests that encoded
     * characters are encoded to something other than the input.  The
     * {@link #encode(String, String)} methods should be use to test
     * actual encoded return values.
     *
     * @see #encode(String, String)
     * @see #encode(String, String, String)
     * @return this
     */
    public EncoderTestSuiteBuilder encodedSuite() {
        assert _encoded.cardinality() + _invalid.cardinality() + _valid.cardinality() == Character.MAX_CODE_POINT + 1;

        TestSuite suite = new TestSuite("encoded");
        int min = _encoded.nextSetBit(0);
        while (min != -1) {
            int max = _encoded.nextClearBit(min+1);
            if (max < 0) {
                max = Character.MAX_CODE_POINT+1;
            }
            final int finalMin = min;
            final int finalMax = max;
            suite.addTest(new TestCase(String.format("U+%04X..U+%04X", finalMin, finalMax - 1)) {
                @Override
                protected void runTest() throws Throwable {
                    char[] chars = new char[2];
                    for (int i= finalMin; i< finalMax; ++i) {
                        String input = new String(chars, 0, Character.toChars(i, chars, 0));
                        String actual = Encode.encode(_encoder, input);
                        if (actual.equals(input)) {
                            fail("input="+debugEncode(input));
                        }
                    }
                }
            });
            min = _encoded.nextSetBit(max+1);
        }
        return add(suite);
    }

    /**
     * Returns the suite that was built.  If any tests were flagged with
     * {@link #mark()}, then only those tests will be included in the result.
     *
     * @see #mark()
     * @return the test suite.
     */
    public TestSuite build() {
        return _markedSuite != null ? _markedSuite : _suite;
    }

    /**
     * "Marks" the last added test for running.  If any tests in the suite is
     * marked, then only the marked tests are run.  Marking allows developers
     * to flag a single test during development and may be useful for debugging
     * or simplying focusing on a perticular (set of) test(s).
     *
     * @return this
     */
    public EncoderTestSuiteBuilder mark() {
        if (_markedSuite == null) {
            _markedSuite = new TestSuite();
        }
        _markedSuite.addTest(_suite.testAt(_suite.testCount() - 1));
        return this;
    }

    /**
     * A writer used during testing.  It extends CharArrayWriter to
     * add assertions to the test sequence, while also buffering the
     * result for later assertions.
     */
    static class TestWriter extends CharArrayWriter {
        private String _input;

        public TestWriter(String input) {
            _input = input;
        }

        @Override
        public void write(String str) throws IOException {
            // Make sure that if the write(String...) apis are called, that its
            // only for the case that the input is unchanged.
            Assert.assertSame(_input, str);
            super.write(str);
        }

        @Override
        public void write(String str, int off, int len) {
            // Make sure that if the write(String...) apis are called, that its
            // only for the case that the input is unchanged.
            Assert.assertSame(_input, str);
            super.write(str, off, len);
        }
    }
}

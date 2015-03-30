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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * CSSEncoderTest -- tests the CSSEncoder in all modes for all code-points.
 *
 * @author Jeff IChnowski
 */
public class CSSEncoderTest extends TestCase {
    public static Test suite() {
        TestSuite suite = new TestSuite();

        for (CSSEncoder.Mode mode : CSSEncoder.Mode.values()) {
            EncoderTestSuiteBuilder builder = new EncoderTestSuiteBuilder(new CSSEncoder(mode), "safe", "'")
                .encode("\\27", "'")
                .encode("safe", "safe")
                .encode("required-space-after-encode", "\\27 1", "'1")
                .encode("no-space-required-after-encode", "\\27x", "'x")
                .encode("NUL", "\\0", "\0")
                .encode("DEL", "\\7f", "\u007f")

                .encoded(0, '\237')
                .valid("!#$%")
                .valid('*', '~')
                .encoded("\\")
                .valid('\240', Character.MAX_CODE_POINT)
                .encoded('\u2028', '\u2029')
                .encode("Line Separator", "\\2028", "\u2028")
                .encode("Paragraph Separator", "\\2029", "\u2029")
                .invalid(Character.MIN_SURROGATE, Character.MAX_SURROGATE);

            switch (mode) {
            case STRING:
                builder.valid(' ', '~');
                break;
            case URL:
                builder.encode("-\\20-", "- -");
                break;
            default:
                throw new AssertionError("untested encoding mode: "+mode);
            }

            suite.addTest(builder
                .encoded("\"\'\\<&/>")
                .validSuite()
                .invalidSuite(CSSEncoder.INVALID_REPLACEMENT_CHARACTER)
                .encodedSuite()
                .build());
        }

        return suite;
    }
}

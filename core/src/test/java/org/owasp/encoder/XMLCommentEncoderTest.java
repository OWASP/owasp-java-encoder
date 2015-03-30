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

/**
 * XMLCommentEncoderTest -- Test suites for the XMLCommentEncoder.
 *
 * @author Jeff Ichnowski
 */
public class XMLCommentEncoderTest extends TestCase {
    public static Test suite() {
        return new EncoderTestSuiteBuilder(
            XMLCommentEncoderTest.class, new XMLCommentEncoder(), "(safe)", "--")
            .encode("a - b", "a - b")
            .encode("<\"&\'>", "<\"&\'>") // valid in comments, not in XML

            .invalid(0, 0x1f)
            .valid("\t\r\n")
            .valid(' ', Character.MAX_CODE_POINT)
            .invalid(0x7f, 0x9f)
            .valid("\u0085")
            .encoded("-")
//            .encoded("&><\'\"")
            .invalid(Character.MIN_SURROGATE, Character.MAX_SURROGATE)
            .invalid(0xfdd0, 0xfdef)
            .invalid(0xfffe, 0xffff)
            .invalid(0x1fffe, 0x1ffff)
            .invalid(0x2fffe, 0x2ffff)
            .invalid(0x3fffe, 0x3ffff)
            .invalid(0x4fffe, 0x4ffff)
            .invalid(0x5fffe, 0x5ffff)
            .invalid(0x6fffe, 0x6ffff)
            .invalid(0x7fffe, 0x7ffff)
            .invalid(0x8fffe, 0x8ffff)
            .invalid(0x9fffe, 0x9ffff)
            .invalid(0xafffe, 0xaffff)
            .invalid(0xbfffe, 0xbffff)
            .invalid(0xcfffe, 0xcffff)
            .invalid(0xdfffe, 0xdffff)
            .invalid(0xefffe, 0xeffff)
            .invalid(0xffffe, 0xfffff)
            .invalid(0x10fffe, 0x10ffff)

            .validSuite()
            .invalidSuite(XMLEncoder.INVALID_CHARACTER_REPLACEMENT)

            .build();
    }

    public void testEncodeHyphens() throws Exception {
        XMLCommentEncoder encoder = new XMLCommentEncoder();
        assertEquals("-~", Encode.encode(encoder, "--"));
        assertEquals("ab-~cd", Encode.encode(encoder, "ab--cd"));
    }

    public void testEncodeHyphenAtEnd() throws Exception {
        XMLCommentEncoder encoder = new XMLCommentEncoder();
        assertEquals("~", Encode.encode(encoder, "-"));
        assertEquals("abc~", Encode.encode(encoder, "abc-"));
    }

    public void testEncodeHyphenBar() throws Exception {
        XMLCommentEncoder encoder = new XMLCommentEncoder();
        assertEquals("-~-~-~-~-~~", Encode.encode(encoder, "-----------"));
    }
}

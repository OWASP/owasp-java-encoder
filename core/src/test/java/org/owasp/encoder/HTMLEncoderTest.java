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
 * HTMLEncoderTest -- test suite for HTMLEncoder.
 *
 * @author Jeff Ichnowski
 */
public class HTMLEncoderTest extends TestCase {
    public static Test suite() {
        return new EncoderTestSuiteBuilder(new HTMLEncoder(), "-safe-", "-&-")
            .encode("&amp;", "&")
            .encode("&gt;", ">")
            .encode("&lt;", "<")
            .encode("&#39;", "\'")
            .encode("&#34;", "\"")
            .encode("space", "&#32;", " ")
            .encode("tab", "&#9;", "\t")
            .encode("LF", "&#10;", "\n")
            .encode("FF", "&#12;", "\f")
            .encode("CR", "&#13;", "\r")
            .encode("&#96;", "`")
            .encode("&#47;", "/")
            .encode("&#133;", "\u0085")
            .encode("safe", "safe")
            .encode("unencoded&amp;encoded", "unencoded&encoded")
            .encode("invalid-control-characters", "-b-", "\0b\26")
            .encode("valid-surrogate-pair", "\ud800\udc00", "\ud800\udc00")
            .encode("missing-low-surrogate", "-", "\ud800")
            .encode("missing-high-surrogate", "-", "\udc00")
            .encode("valid-upper-char", "\ufffd", "\ufffd")
            .encode("invalid-upper-char", "-", "\uffff")
            .encode("line-separator", "&#8232;", "\u2028")
            .encode("paragraph-separator", "&#8233;", "\u2029")

            .invalid(0, 0x1f)
            .valid("\t\r\n")
            .valid(' ', Character.MAX_CODE_POINT)
            .invalid(0x7f, 0x9f)
            .valid("\u0085")
            .encoded("&><\'\"/`= \r\n\t\f\u0085\u2028\u2029")
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
            .invalidSuite('-')
            .encodedSuite()

            .build();
    }
}

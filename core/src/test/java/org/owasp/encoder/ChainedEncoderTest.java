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
 * ChainedEncoderTest -- Tests the ChainedEncoder for all code-points.
 *
 * @author Jeff Ichnowski
 */
public class ChainedEncoderTest extends TestCase {
    public static Test suite() {
        return new EncoderTestSuiteBuilder(
            new ChainedEncoder(
                new JavaScriptEncoder(JavaScriptEncoder.Mode.SOURCE, false),
                new XMLEncoder()), "-safe-", "-\\&-")

            // from JavaScriptEncoderTest
            .encode("\\\\", "\\")
            .encode("\\&#34;", "\"")
            .encode("\\&#39;", "\'")
            .encode("backspace", "\\b", "\b")
            .encode("tab", "\\t", "\t")
            .encode("LF", "\\n", "\n")
            .encode("vtab", "\\x0b", "\u000b")
            .encode("FF", "\\f", "\f")
            .encode("CR", "\\r", "\r")
            .encode("NUL", "\\x00", "\0")
            .encode("abc", "abc")
            .encode("ABC", "ABC")

            // from XMLEncoderTest
            .encode("&amp;", "&")
            .encode("&gt;", ">")
            .encode("&lt;", "<")
            .build();
    }
}

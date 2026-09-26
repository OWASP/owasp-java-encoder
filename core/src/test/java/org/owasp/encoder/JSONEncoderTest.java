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

import junit.framework.Test;
import junit.framework.TestCase;

/**
 * JSONEncoderTest -- exact output, per-code-point classification, and
 * Writer / buffer boundary behaviour of {@link JSONEncoder}.
 */
public class JSONEncoderTest extends TestCase {
    public static Test suite() {
        return new EncoderTestSuiteBuilder(new JSONEncoder(), "-safe-", "<")
            .encode("empty", "", "")
            .encode("abc123XYZ", "abc123XYZ")
            .encode("\\\"", "\"")
            .encode("\\\\", "\\")
            .encode("\\b", "\\b", "\b")
            .encode("\\t", "\\t", "\t")
            .encode("\\n", "\\n", "\n")
            .encode("\\f", "\\f", "\f")
            .encode("\\r", "\\r", "\r")
            .encode("U+0000", "\\u0000", "\0")
            .encode("U+000B", "\\u000b", "\u000b")
            .encode("U+001F", "\\u001f", "\u001f")
            .encode("less-than", "\\u003c", "<")
            .encode("greater-than", "\\u003e", ">")
            .encode("ampersand", "\\u0026", "&")
            .encode("U+2028", "\\u2028", "\u2028")
            .encode("U+2029", "\\u2029", "\u2029")
            .encode("</script>", "\\u003c/script\\u003e", "</script>")
            .encode("<!--", "\\u003c!--", "<!--")
            .encode("]]>", "]]\\u003e", "]]>")

            // passed through unchanged
            .encode("slash", "/", "/")
            .encode("apostrophe", "'", "'")
            .encode("U+007F", "\u007f", "\u007f")
            .encode("U+00E9", "\u00e9", "\u00e9")
            .encode("U+FFFF", "\uffff", "\uffff")
            .encode("surrogate-pair", "\ud83d\ude00", "\ud83d\ude00")
            .encode("surrogate-pairs", "\ud800\udc00\udbff\udfff", "\ud800\udc00\udbff\udfff")

            // unpaired surrogates, lowercase hex
            .encode("lone-high", "\\ud800", "\ud800")
            .encode("lone-high-max", "\\udbff", "\udbff")
            .encode("lone-low", "\\udc00", "\udc00")
            .encode("lone-low-max", "\\udfff", "\udfff")
            .encode("high-high-low", "\\ud800\ud800\udc00", "\ud800\ud800\udc00")
            .encode("high-low-low", "\ud800\udc00\\udc00", "\ud800\udc00\udc00")
            .encode("reversed-pair", "\\udc00\\ud800", "\udc00\ud800")
            .encode("high-then-ascii", "\\ud800a", "\ud800a")
            .encode("high-then-escape", "\\ud800\\u003c", "\ud800<")

            .encoded(0, Character.MAX_CODE_POINT)
            .valid(' ', Character.MAX_CODE_POINT)
            .encoded("\"\\<>&\u2028\u2029")
            .encoded(Character.MIN_SURROGATE, Character.MAX_SURROGATE)

            .validSuite()
            .encodedSuite()

            .build();
    }
}

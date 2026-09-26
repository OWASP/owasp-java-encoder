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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Checks that {@link Encoder#maxEncodedLength(int)} is an upper bound for every
 * encoder.  {@link Encode} sizes its fallback buffers from this bound, so an
 * underestimate makes the String-returning methods fail.
 */
public class MaxEncodedLengthTest {

    /** Characters that change how the preceding character is encoded in some context. */
    private static final char[] FOLLOWERS = {
        'a', '0', 'F', ' ', '\t', '\n', '\r', '\f', ']', '>', '-', '\u2028', '\uD83D', '\uDE00'
    };

    @Test
    public void maxEncodedLengthIsAnUpperBoundForEveryCharacter() throws Exception {
        List<String> names = contextNames();
        assertFalse(names.isEmpty());
        for (String name : names) {
            Encoder encoder = Encoders.forName(name);
            for (int c = Character.MIN_VALUE; c <= Character.MAX_VALUE; ++c) {
                char ch = (char) c;
                assertWithinBound(name, encoder, new char[] {ch, ch, ch, ch});
                for (char follower : FOLLOWERS) {
                    assertWithinBound(name, encoder, new char[] {ch, follower, ch, follower});
                }
            }
        }
    }

    private static void assertWithinBound(String name, Encoder encoder, char[] input) {
        CharBuffer in = CharBuffer.wrap(input);
        CharBuffer out = CharBuffer.allocate(input.length * 32);
        assertTrue(encoder.encode(in, out, true).isUnderflow());
        int bound = encoder.maxEncodedLength(input.length);
        if (out.position() > bound) {
            fail(name + ": " + escape(input) + " encoded to " + out.position()
                + " characters but maxEncodedLength(" + input.length + ") is " + bound);
        }
    }

    /**
     * Returns the value of every public String constant in {@link Encoders}.
     *
     * @return the context names
     * @throws IllegalAccessException if a constant cannot be read
     */
    static List<String> contextNames() throws IllegalAccessException {
        List<String> names = new ArrayList<String>();
        for (Field field : Encoders.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                names.add((String) field.get(null));
            }
        }
        return names;
    }

    static String escape(CharSequence s) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
            if (ch >= ' ' && ch <= '~') {
                buf.append(ch);
            } else {
                buf.append(String.format("\\u%04x", (int) ch));
            }
        }
        return buf.toString();
    }

    private static String escape(char[] input) {
        return escape(CharBuffer.wrap(input));
    }
}

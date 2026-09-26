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

package org.owasp.encoder.tag;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.owasp.encoder.Encode;

/** A named JUnit 3 parameterized case for every advanced tag and input. */
public class TagEncodingTest extends EncodingTagTest {
    private final Class<? extends EncodingTag> tagClass;
    private final Method facade;
    private final String input;

    private TagEncodingTest(String name, Class<? extends EncodingTag> tagClass,
            Method facade, String input) {
        super(name);
        this.tagClass = tagClass;
        this.facade = facade;
        this.input = input;
    }

    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite(TagEncodingTest.class.getName());
        TaglibDescriptorTest.Taglib descriptor = TaglibDescriptorTest.load("META-INF/java-encoder-advanced.tld");
        for (Map.Entry<String, String> tag : descriptor.tagClasses.entrySet()) {
            Class<? extends EncodingTag> type = Class.forName(tag.getValue()).asSubclass(EncodingTag.class);
            Method method = Encode.class.getMethod(tag.getKey(), String.class);
            add(suite, type, method, "null", null);
            add(suite, type, method, "empty", "");
            add(suite, type, method, "plain", "plain text");
            String hostile = "\"'<>&/\\`$={}() :;?#%+--]]>\0\t\r\n\u007f\u0085\u2028\u2029"
                + "\u00e9\ud83d\ude00\ud800X\udc00\uffff\ud800";
            add(suite, type, method, "hostile-unicode", hostile);
            for (int size : new int[] {1023, 1024, 1025, 2047, 2048, 2049}) {
                char[] padding = new char[size - 1];
                Arrays.fill(padding, 'a');
                // Force the buffering path from index zero, with a surrogate
                // pair at the boundary, instead of skipping a safe prefix.
                add(suite, type, method, "boundary-" + size,
                    "\0" + new String(padding) + "\ud83d\ude00" + hostile);
            }
        }
        return suite;
    }

    private static void add(TestSuite suite, Class<? extends EncodingTag> type,
            Method facade, String name, String input) {
        suite.addTest(new TagEncodingTest(facade.getName() + ":" + name, type, facade, input));
    }

    @Override
    protected void runTest() throws Exception {
        String expected = input == null ? "null" : (String) facade.invoke(null, input);
        EncodingTag tag = tagClass.getConstructor().newInstance();
        tag.setJspContext(_pageContext);
        tag.setValue(input);
        tag.doTag();
        assertEquals(getName(), expected, _response.getContentAsString());
    }
}

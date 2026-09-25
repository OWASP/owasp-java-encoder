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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Locale;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Checks the HTML uses documented on {@link Encode#forJson(String)}: JSON
 * inside a {@code <script>} element, and JSON in an HTML attribute built
 * with {@code forJson} followed by {@code forHtmlAttribute}.
 */
public class JSONHtmlEmbeddingTest extends TestCase {

    /**
     * Values that try to leave a script element or an attribute.
     */
    private static final String[] HOSTILE = {
        "</script><script>alert(1)</script>",
        "</SCRIPT >",
        "<!--<script>",
        "-->]]>",
        "\"><img src=x onerror=alert(1)>",
        "' onmouseover='alert(1)",
        "&quot;&#34;&amp;",
        "\\\"\u2028\u2029\ud800",
    };

    private final ObjectMapper _mapper = new ObjectMapper();

    public static Test suite() {
        return new TestSuite(JSONHtmlEmbeddingTest.class);
    }

    private static String json(String value) {
        return "{\"name\":\"" + Encode.forJson(value) + "\"}";
    }

    private String parseName(String json) throws IOException {
        return _mapper.readTree(json).get("name").asText();
    }

    public void testScriptElement() throws IOException {
        for (String value : HOSTILE) {
            String open = "<script type=\"application/json\" id=\"data\">";
            String page = open + json(value) + "</script>";

            // The first end tag the HTML parser can see must be ours, and no
            // comment may start inside the element.
            String lower = page.toLowerCase(Locale.ROOT);
            assertEquals(value, page.length() - "</script>".length(), lower.indexOf("</script"));
            assertEquals(value, -1, page.indexOf("<!--"));

            String content = page.substring(open.length(), page.length() - "</script>".length());
            assertEquals(value, parseName(content));
        }
    }

    public void testHtmlAttribute() throws IOException {
        for (String value : HOSTILE) {
            String json = json(value);
            String attribute = Encode.forHtmlAttribute(json);

            // Nothing in the value can end a quoted attribute or start a tag.
            assertEquals(value, -1, attribute.indexOf('"'));
            assertEquals(value, -1, attribute.indexOf('\''));
            assertEquals(value, -1, attribute.indexOf('<'));

            // The browser decodes the attribute back to the JSON text.
            String decoded = decodeHtmlAttribute(attribute);
            assertEquals(value, json, decoded);
            assertEquals(value, parseName(decoded));
        }
    }

    /**
     * Decodes the character references {@link Encode#forHtmlAttribute}
     * produces.
     */
    private static String decodeHtmlAttribute(String attribute) {
        return attribute
            .replace("&#34;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&");
    }
}

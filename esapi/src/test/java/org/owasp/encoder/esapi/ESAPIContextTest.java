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

package org.owasp.encoder.esapi;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import junit.framework.TestCase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;
import org.owasp.esapi.errors.EncodingException;
import org.owasp.esapi.reference.DefaultEncoder;

/** Context and value contracts, also exercised by the stable ESAPI matrix. */
public class ESAPIContextTest extends TestCase {
    private final Encoder encoder = ESAPIEncoder.getInstance();

    public void testUrlComponentEncodingAndReferenceFormDifferences() throws Exception {
        assertEquals("UTF-8", ESAPI.securityConfiguration().getCharacterEncoding());
        Encoder reference = DefaultEncoder.getInstance();
        // Input, adapter component encoding, ESAPI reference form encoding.
        String[][] cases = {
            {"", "", ""},
            {"abcABC123-._", "abcABC123-._", "abcABC123-._"},
            {"a b+c", "a%20b%2Bc", "a+b%2Bc"},
            {"~*", "~%2A", "%7E*"},
            {"&=/?#", "%26%3D%2F%3F%23", "%26%3D%2F%3F%23"},
            {":[]@!$'(),;", "%3A%5B%5D%40%21%24%27%28%29%2C%3B",
                "%3A%5B%5D%40%21%24%27%28%29%2C%3B"},
            {"%20%zz%", "%2520%25zz%25", "%2520%25zz%25"},
            {"\"<>\\`{}", "%22%3C%3E%5C%60%7B%7D", "%22%3C%3E%5C%60%7B%7D"},
            {"\u00e9\u03a9\ud83d\ude00", "%C3%A9%CE%A9%F0%9F%98%80",
                "%C3%A9%CE%A9%F0%9F%98%80"},
            {"\u0000\r\n\t\u007f", "%00%0D%0A%09%7F", "%00%0D%0A%09%7F"}
        };
        for (String[] example : cases) {
            assertEquals(example[0], example[1], encoder.encodeForURL(example[0]));
            assertEquals(example[0], example[2], reference.encodeForURL(example[0]));
            assertEquals(example[0], URLDecoder.decode(example[1], "UTF-8"));
            assertEquals(example[0], URLDecoder.decode(example[2], "UTF-8"));
        }
    }

    public void testUrlRetainsAdapterNullAndMalformedUtf16Policy() throws Exception {
        Encoder reference = DefaultEncoder.getInstance();
        assertEquals("null", encoder.encodeForURL(null));
        assertNull(reference.encodeForURL(null));
        String[][] cases = {
            {"\ud800", "-", "%3F"},
            {"\udfff", "-", "%3F"},
            {"a\ud800z", "a-z", "a%3Fz"},
            {"\udc00\ud800", "--", "%3F%3F"},
            {"\ud800\ud800\udc00", "-%F0%90%80%80", "%3F%F0%90%80%80"}
        };
        for (String[] example : cases) {
            assertEquals(example[1], encoder.encodeForURL(example[0]));
            assertEquals(example[2], reference.encodeForURL(example[0]));
        }
    }

    public void testUrlEncodingCannotAddQueryParametersOrFragments() throws Exception {
        String input = "a b+c&admin=true/../?other=value#fragment\u03a9\ud83d\ude00";
        String encoded = encoder.encodeForURL(input);
        URI url = new URI("https://example.test/search?q=" + encoded + "&page=1");
        assertEquals("https", url.getScheme());
        assertEquals("example.test", url.getHost());
        assertEquals("/search", url.getRawPath());
        assertNull(url.getRawFragment());
        String[] query = url.getRawQuery().split("&", -1);
        assertEquals(2, query.length);
        assertEquals("page=1", query[1]);
        String[] parameter = query[0].split("=", -1);
        assertEquals(2, parameter.length);
        assertEquals("q", parameter[0]);
        assertEquals(input, URLDecoder.decode(parameter[1], "UTF-8"));

        URI path = new URI("https://example.test/items/" + encoded + "/detail");
        assertNull(path.getRawQuery());
        assertNull(path.getRawFragment());
        assertEquals(4, path.getRawPath().split("/", -1).length);
        assertEquals(input, URLDecoder.decode(path.getRawPath().split("/", -1)[2], "UTF-8"));
    }

    public void testUrlKeepsCheckedExceptionInterface() throws Exception {
        assertEquals(Arrays.asList(EncodingException.class), Arrays.asList(
            Encoder.class.getMethod("encodeForURL", String.class).getExceptionTypes()));
        assertEquals(Arrays.asList(EncodingException.class), Arrays.asList(
            encoder.getClass().getMethod("encodeForURL", String.class).getExceptionTypes()));
    }

    public void testQuotedHtmlAttributesPreserveDataAndCannotAddMarkup() {
        String[] inputs = {"", "'\" autofocus onfocus=alert(1) x='\"", "<svg onload=alert(1)>",
            "&quot;&#39;&amp;", "a=b / ` \t\n\u03a9\ud83d\ude00"};
        for (String input : inputs) {
            for (String quote : new String[] {"'", "\""}) {
                Document document = Jsoup.parse("<input value=" + quote
                    + encoder.encodeForHTMLAttribute(input) + quote + ">");
                assertEquals(1, document.body().childrenSize());
                Element element = document.body().child(0);
                assertEquals("input", element.tagName());
                assertEquals(1, element.attributes().size());
                assertEquals(input, element.attr("value"));
            }
        }
    }

    public void testHtmlAttributeEncodingRetainsQuotedContextOnly() {
        // Whitespace/equals remain data only because the caller supplies quotes.
        assertEquals("a b=c", encoder.encodeForHTMLAttribute("a b=c"));
        // HTML encoding does not validate schemes or encode JavaScript programs.
        assertEquals("javascript:alert(1)", encoder.encodeForHTMLAttribute("javascript:alert(1)"));
    }

    public void testUrlInsideQuotedHtmlAttributeSurvivesBothParsers() throws Exception {
        String input = "\"&admin=true#fragment+ space\u03a9";
        String url = "https://example.test/search?q=" + encoder.encodeForURL(input) + "&page=1";
        Element link = Jsoup.parse("<a href=\"" + encoder.encodeForHTMLAttribute(url)
            + "\">link</a>").selectFirst("a");
        assertNotNull(link);
        assertEquals(1, link.attributes().size());
        assertEquals(url, link.attr("href"));
        URI parsed = new URI(link.attr("href"));
        assertNull(parsed.getRawFragment());
        String[] parameters = parsed.getRawQuery().split("&");
        assertEquals(2, parameters.length);
        assertEquals(input, URLDecoder.decode(parameters[0].substring(2), "UTF-8"));
    }

    public void testCssLongSeparatorRunsRetainSecurityFix() {
        StringBuilder input = new StringBuilder();
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < 4096; i++) {
            input.append('\u2028').append('\u2029');
            expected.append("\\2028\\2029");
        }
        assertEquals(expected.toString(), encoder.encodeForCSS(input.toString()));
        assertEquals("\\27  a", encoder.encodeForCSS("' a"));
    }

    public void testJavaScriptTemplateBoundaryAndUtf8ValuePreservation() {
        String input = "${value}`\u007f\u0085\ud800|\udfff|\ud83d\ude00";
        String encoded = encoder.encodeForJavaScript(input);
        assertEquals("\\x24\\x7bvalue}\\x60\\x7f\\x85\\ud800|\\udfff|\ud83d\ude00", encoded);
        assertEquals(encoded, new String(encoded.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        assertEquals("\\x7bexecuted=true}", encoder.encodeForJavaScript("{executed=true}"));
    }
}

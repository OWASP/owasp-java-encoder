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

import java.util.Arrays;
import java.util.List;

/** Independent context wiring expectations shared by facade and registry tests. */
final class EncoderContexts {
    final String name;
    final String method;
    final Encoder encoder;

    private EncoderContexts(String name, String method, Encoder encoder) {
        this.name = name;
        this.method = method;
        this.encoder = encoder;
    }

    static final List<EncoderContexts> ALL = Arrays.asList(
        new EncoderContexts("html", "forHtml", new XMLEncoder(XMLEncoder.Mode.ALL)),
        new EncoderContexts("html-content", "forHtmlContent", new XMLEncoder(XMLEncoder.Mode.CONTENT)),
        new EncoderContexts("html-attribute", "forHtmlAttribute", new XMLEncoder(XMLEncoder.Mode.ATTRIBUTE)),
        new EncoderContexts("html-attribute-unquoted", "forHtmlUnquotedAttribute", new HTMLEncoder()),
        new EncoderContexts("xml", "forXml", new XMLEncoder(XMLEncoder.Mode.ALL)),
        new EncoderContexts("xml-content", "forXmlContent", new XMLEncoder(XMLEncoder.Mode.CONTENT)),
        new EncoderContexts("xml-attribute", "forXmlAttribute", new XMLEncoder(XMLEncoder.Mode.ATTRIBUTE)),
        new EncoderContexts("xml-comment", "forXmlComment", new XMLCommentEncoder()),
        new EncoderContexts("xml-1.1", "forXml11", new XMLEncoder(XMLEncoder.Mode.ALL, XMLEncoder.Version.XML_1_1)),
        new EncoderContexts("xml-1.1-content", "forXml11Content", new XMLEncoder(XMLEncoder.Mode.CONTENT, XMLEncoder.Version.XML_1_1)),
        new EncoderContexts("xml-1.1-attribute", "forXml11Attribute", new XMLEncoder(XMLEncoder.Mode.ATTRIBUTE, XMLEncoder.Version.XML_1_1)),
        new EncoderContexts("cdata", "forCDATA", new CDATAEncoder()),
        new EncoderContexts("css-string", "forCssString", new CSSEncoder(CSSEncoder.Mode.STRING)),
        new EncoderContexts("css-url", "forCssUrl", new CSSEncoder(CSSEncoder.Mode.URL)),
        new EncoderContexts("java", "forJava", new JavaEncoder()),
        new EncoderContexts("javascript", "forJavaScript", new JavaScriptEncoder(JavaScriptEncoder.Mode.HTML, false)),
        new EncoderContexts("javascript-attribute", "forJavaScriptAttribute", new JavaScriptEncoder(JavaScriptEncoder.Mode.ATTRIBUTE, false)),
        new EncoderContexts("javascript-block", "forJavaScriptBlock", new JavaScriptEncoder(JavaScriptEncoder.Mode.BLOCK, false)),
        new EncoderContexts("javascript-source", "forJavaScriptSource", new JavaScriptEncoder(JavaScriptEncoder.Mode.SOURCE, false)),
        new EncoderContexts("json", "forJson", new JSONEncoder()),
        // Deprecated but retained as a supported compatibility entry point.
        new EncoderContexts("uri", "forUri", new URIEncoder(URIEncoder.Mode.FULL_URI)),
        new EncoderContexts("uri-component", "forUriComponent", new URIEncoder(URIEncoder.Mode.COMPONENT))
    );

    static String probe() {
        StringBuilder value = new StringBuilder();
        // Every ASCII metacharacter plus DEL and C1 controls distinguishes modes.
        for (char ch = 0; ch <= 0x9f; ch++) {
            value.append(ch);
        }
        return value.append("--]]> ${x} \u00e9\u2028\u2029\ud83d\ude00\ud800X\udc00\ufdd0\ufffe\uffff\ud800").toString();
    }
}

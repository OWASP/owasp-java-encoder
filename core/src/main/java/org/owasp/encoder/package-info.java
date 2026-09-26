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

/**
 * Contextual output encoding for untrusted text. Choose the method for the
 * parser context receiving the output; callers supply the enclosing syntax.
 *
 * <table>
 * <caption>Common output contexts</caption>
 * <tr><th scope="col">Context</th><th scope="col">Method and constraints</th></tr>
 * <tr><td>HTML text</td><td>{@link org.owasp.encoder.Encode#forHtmlContent(String)}</td></tr>
 * <tr><td>Quoted HTML attribute</td><td>{@link org.owasp.encoder.Encode#forHtmlAttribute(String)};
 * validate URLs and their schemes before encoding URL-valued attributes</td></tr>
 * <tr><td>Inserted URI component</td><td>{@link org.owasp.encoder.Encode#forUriComponent(String)}</td></tr>
 * <tr><td>JavaScript string</td><td>{@link org.owasp.encoder.Encode#forJavaScript(String)};
 * single or double quotes, not template literals, JSON, or script URLs</td></tr>
 * <tr><td>CSS string / unquoted url(...)</td><td>{@link org.owasp.encoder.Encode#forCssString(String)} /
 * {@link org.owasp.encoder.Encode#forCssUrl(String)}; validate URLs separately</td></tr>
 * <tr><td>XML 1.0 text / quoted attribute</td><td>{@link org.owasp.encoder.Encode#forXmlContent(String)} /
 * {@link org.owasp.encoder.Encode#forXmlAttribute(String)}</td></tr>
 * <tr><td>XML 1.1 text / quoted attribute</td><td>{@link org.owasp.encoder.Encode#forXml11Content(String)} /
 * {@link org.owasp.encoder.Encode#forXml11Attribute(String)}</td></tr>
 * <tr><td>XML CDATA / XML comment</td><td>{@link org.owasp.encoder.Encode#forCDATA(String)} /
 * {@link org.owasp.encoder.Encode#forXmlComment(String)}; XML comments only, not HTML comments</td></tr>
 * <tr><td>Java string literal</td><td>{@link org.owasp.encoder.Encode#forJava(String)};
 * output with unpaired surrogates is not guaranteed to compile</td></tr>
 * </table>
 *
 * <p>Use a JSON serializer for JSON. Encoding does not validate a URL, make
 * arbitrary executable code safe, or sanitize HTML markup. See each method's
 * documentation for character handling and context restrictions.</p>
 *
 * <p>{@link org.owasp.encoder.Encode} provides String and Writer overloads;
 * prefer Writer overloads or {@link org.owasp.encoder.EncodedWriter} for large
 * inputs. {@link org.owasp.encoder.Encoders} exposes shared stateless encoder
 * singletons for the low-level buffer API.</p>
 */
package org.owasp.encoder;

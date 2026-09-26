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

package org.owasp.encoder.esapi;

import java.io.IOException;
import java.net.URI;
import org.owasp.encoder.Encode;
import org.owasp.esapi.Encoder;
import org.owasp.esapi.codecs.Codec;
import org.owasp.esapi.errors.EncodingException;
import org.owasp.esapi.reference.DefaultEncoder;

/**
 * ESAPIEncoder is a singleton implementation of the ESAPI Encoder API.  It
 * is meant to allow quick and easy drop-in replacement of the default
 * encoder included with the ESAPI library, as the Encoder libraries are
 * faster and use less memory thus cause fewer garbage collections.
 *
 * <p>Please note that the OWASP Java Encoders does not implement all
 * the encodings of the ESAPI Encoder API.  In such situations this
 * implementation will fallback onto the default reference implementation
 * included with ESAPI.  Thus you should see the performance benefit from
 * the methods included in the Encoder, but still maintain compatibility
 * with the delegated methods from ESAPI Encoder. The methods implemented
 * here have the contextual contracts described below.</p>
 *
 * <p>The adapter's {@code encodeForCSS} encodes only quoted CSS strings, and
 * {@code encodeForJavaScript} encodes only single- or double-quoted JavaScript
 * strings, not JSON, template literals, or script URLs. Its
 * {@code encodeForURL} delegates to deprecated {@link Encode#forUri(String)}:
 * it preserves URI delimiters such as {@code &amp; = / ? #} and therefore is
 * not a URL-component or form encoder. For an inserted component, use
 * {@link Encode#forUriComponent(String)}. Validate complete URLs and their
 * schemes, then encode for the enclosing output context.</p>
 *
 * <p>The following methods delegate to ESAPI. Most are outside the scope of
 * contextual output encoding; JSON encoding retains the reference behavior
 * for compatibility, including its {@code null} result for {@code null} input:</p>
 *
 * <ul>
 *     <li>Input validation/normalization methods:
 *     {@link org.owasp.esapi.Encoder#canonicalize(String)},
 *     {@link org.owasp.esapi.Encoder#canonicalize(String, boolean)},
 *     {@link org.owasp.esapi.Encoder#canonicalize(String, boolean, boolean)}
 *     {@link org.owasp.esapi.Encoder#getCanonicalizedURI(URI)}</li>
 *
 *     <li>Decoding methods:
 *     {@link org.owasp.esapi.Encoder#decodeForHTML(String)},
 *     {@link org.owasp.esapi.Encoder#decodeFromURL(String)}</li>
 *
 *     <li>JSON encoding and decoding:
 *     {@link org.owasp.esapi.Encoder#encodeForJSON(String)},
 *     {@link org.owasp.esapi.Encoder#decodeFromJSON(String)}.
 *     For the core library's JSON string context, use {@link Encode#forJson(String)}
 *     directly; its escaping and null contract differ from ESAPI's.</li>
 *
 *     <li>Binary-to-text/text-to-binary:
 *     {@link org.owasp.esapi.Encoder#encodeForBase64(byte[], boolean)},
 *     {@link org.owasp.esapi.Encoder#decodeFromBase64(String)}.</li>
 *
 *     <li>Bind-able APIs (such as {@link java.sql.PreparedStatement}:
 *     {@link org.owasp.esapi.Encoder#encodeForSQL(org.owasp.esapi.codecs.Codec, String)},
 *     {@link org.owasp.esapi.Encoder#encodeForXPath(String)},
 *     {@link org.owasp.esapi.Encoder#encodeForOS(org.owasp.esapi.codecs.Codec, String)}</li>
 *
 *     <li>Rarely-used or alternate compatible encoding:
 *     {@link org.owasp.esapi.Encoder#encodeForVBScript(String)},
 *     {@link org.owasp.esapi.Encoder#encodeForLDAP(String)},
 *     {@link org.owasp.esapi.Encoder#encodeForLDAP(String, boolean)},
 *     {@link org.owasp.esapi.Encoder#encodeForDN(String)}</li>
 * </ul>
 *
 * <p>(Please note that with sufficient feedback from the user base, the above
 * mentioned methods may be implemented in future releases of the OWASP
 * Java Encoders, if/when that happens, this shim class will be updated to
 * call out to the new methods.)</p>
 *
 * <p>You may notice that this class does not actually implement Encoder
 * itself.  Instead it simply provides a {@link #getInstance()} method that
 * does.  This allows the implementation details maximum flexibility by not
 * creating a any public API that would restrict changes later</p>
 *
 * @author jeffi
 */
public final class ESAPIEncoder {

    /** No instances. */
    private ESAPIEncoder() {}

    /**
     * Returns an instance of the Encoder.  This method is the only supported
     * mechanism by which an ESAPIEncoder instance should be obtained.  The
     * returned implementation is guaranteed to be thread-safe for the methods
     * that the OWASP Java Encoders implement (see class documentation).
     * Though not a requirement of the ESAPI Encoder API, the returned value
     * is also serializable.
     * Obtaining the instance and using OWASP Java Encoder-backed methods does
     * not load ESAPI configuration. Delegated methods resolve ESAPI's reference
     * encoder when called and require its configuration to be available.
     *
     * @return An encoder implementation that uses the OWASP Java Encoders
     * for most of the common encoding methods.
     */
    public static Encoder getInstance() {
        return Impl.INSTANCE;
    }

    /**
     * This is the private singleton that implements the ESAPI Encoder shim.
     * It is implemented as a single-value enum to get all the "free" singleton
     * properties associated with enums, including serialization and thread-safe
     * initialization. Initializing this enum does not initialize ESAPI's
     * reference encoder.
     *
     * <p>The implementation is intentionally private to avoid any API baggage.
     * The instance should be obtained using
     * {@link org.owasp.encoder.esapi.ESAPIEncoder#getInstance()}.</p>
     */
    private enum Impl implements Encoder {
        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * Resolves ESAPI's reference encoder for each delegated call. ESAPI
         * caches the successful singleton itself. Keeping this call out of
         * class initialization allows a failed configuration load to be retried.
         */
        private static Encoder reference() {
            return DefaultEncoder.getInstance();
        }

        /** {@inheritDoc} */
        @Override
        public String canonicalize(String s) {
            return reference().canonicalize(s);
        }

        /** {@inheritDoc} */
        @Override
        public String canonicalize(String s, boolean strict) {
            return reference().canonicalize(s, strict);
        }

        /** {@inheritDoc} */
        @Override
        public String canonicalize(String s, boolean restrictMultiple, boolean restrictMixed) {
            return reference().canonicalize(s, restrictMultiple, restrictMixed);
        }

        /** {@inheritDoc} */
        @Override
        public String getCanonicalizedURI(URI dirtyUri) {
            return reference().getCanonicalizedURI(dirtyUri);
        }

        /**
         * Encodes a quoted CSS string using {@link Encode#forCssString(String)}.
         * This is not an encoder for arbitrary CSS expressions or property values.
         */
        @Override
        public String encodeForCSS(String s) {
            return Encode.forCssString(s);
        }

        /** {@inheritDoc} */
        @Override
        public String encodeForHTML(String s) {
            return Encode.forHtml(s);
        }

        /** {@inheritDoc} */
        @Override
        public String decodeForHTML(String s) {
            return reference().decodeForHTML(s);
        }

        /** {@inheritDoc} */
        @Override
        public String encodeForHTMLAttribute(String s) {
            return Encode.forHtmlAttribute(s);
        }

        /**
         * Encodes a single- or double-quoted JavaScript string using
         * {@link Encode#forJavaScript(String)}. Not for JSON, template literals, or script URLs.
         */
        @Override
        public String encodeForJavaScript(String s) {
            return Encode.forJavaScript(s);
        }

        /** {@inheritDoc} */
        @Override
        public String encodeForVBScript(String s) {
            return reference().encodeForVBScript(s);
        }

        /** {@inheritDoc} */
        @Override
        public String encodeForSQL(Codec codec, String s) {
            return reference().encodeForSQL(codec, s);
        }

        /** {@inheritDoc} */
        @Override
        public String encodeForOS(Codec codec, String s) {
            return reference().encodeForOS(codec, s);
        }

        /** {@inheritDoc} */
        @Override
        public String encodeForLDAP(String s) {
            return reference().encodeForLDAP(s);
        }

        /** {@inheritDoc} */
        @Override
        public String encodeForLDAP(String s, boolean b) {
            return reference().encodeForLDAP(s, b);
        }

        /** {@inheritDoc} */
        @Override
        public String encodeForDN(String s) {
            return reference().encodeForDN(s);
        }

        /** {@inheritDoc} */
        @Override
        public String encodeForXPath(String s) {
            return reference().encodeForXPath(s);
        }

        /** {@inheritDoc} */
        @Override
        public String encodeForXML(String s) {
            return Encode.forXml(s);
        }

        /** {@inheritDoc} */
        @Override
        public String encodeForXMLAttribute(String s) {
            return Encode.forXmlAttribute(s);
        }

        /**
         * Encodes a complete URI using deprecated {@link Encode#forUri(String)}.
         * Preserves delimiters such as {@code &amp; = / ? #}; not a component
         * or form encoder. The caller must validate the URI and its scheme.
         */
        @Override
        public String encodeForURL(String s) throws EncodingException {
            return Encode.forUri(s);
        }

        /** {@inheritDoc} */
        @Override
        public String decodeFromURL(String s) throws EncodingException {
            return reference().decodeFromURL(s);
        }

        /** {@inheritDoc} */
        @Override
        public String encodeForBase64(byte[] bytes, boolean wrap) {
            return reference().encodeForBase64(bytes, wrap);
        }

        /** {@inheritDoc} */
        @Override
        public byte[] decodeFromBase64(String s) throws IOException {
            return reference().decodeFromBase64(s);
        }

        /**
         * Delegates JSON string encoding to the ESAPI reference encoder.
         */
        @Override
        public String encodeForJSON(String s) {
            return reference().encodeForJSON(s);
        }

        /**
         * Delegates JSON string decoding to the ESAPI reference encoder.
         */
        @Override
        public String decodeFromJSON(String s) {
            return reference().decodeFromJSON(s);
        }

    }
}

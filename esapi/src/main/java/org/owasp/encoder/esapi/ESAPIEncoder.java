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
 * with all methods from ESAPI Encoder.</p>
 *
 * <p>For clarity, the reason the OWASP Java Encoders do not include some
 * of the ESAPI library is that the Encoders library is specifically focused
 * on <i>encoding</i>, and thus does not include:</p>
 *
 * <ul>
 *     <li>Input validation/normalization methods:
 *     {@link org.owasp.esapi.Encoder#canonicalize(String)},
 *     {@link org.owasp.esapi.Encoder#canonicalize(String, boolean)},
 *     {@link org.owasp.esapi.Encoder#canonicalize(String, boolean, boolean)}</li>
 *
 *     <li>Decoding methods:
 *     {@link org.owasp.esapi.Encoder#decodeForHTML(String)},
 *     {@link org.owasp.esapi.Encoder#decodeFromURL(String)}</li>
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
     * properties associated with enums--such as serialization, and on-demand
     * initialization.
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
         * The reference encoder from ESAPI.  Any ESAPI method without an
         * OWASP Java Encoder equivalent is delegated to this instance.
         */
        private final Encoder _referenceEncoder = DefaultEncoder.getInstance();

        /** {@inheritDoc} */
        public String canonicalize(String s) {
            return _referenceEncoder.canonicalize(s);
        }

        /** {@inheritDoc} */
        public String canonicalize(String s, boolean strict) {
            return _referenceEncoder.canonicalize(s, strict);
        }

        /** {@inheritDoc} */
        public String canonicalize(String s, boolean restrictMultiple, boolean restrictMixed) {
            return _referenceEncoder.canonicalize(s, restrictMultiple, restrictMixed);
        }

        /** {@inheritDoc} */
        public String encodeForCSS(String s) {
            return Encode.forCssString(s);
        }

        /** {@inheritDoc} */
        public String encodeForHTML(String s) {
            return Encode.forHtml(s);
        }

        /** {@inheritDoc} */
        public String decodeForHTML(String s) {
            return _referenceEncoder.decodeForHTML(s);
        }

        /** {@inheritDoc} */
        public String encodeForHTMLAttribute(String s) {
            return Encode.forHtmlAttribute(s);
        }

        /** {@inheritDoc} */
        public String encodeForJavaScript(String s) {
            return Encode.forJavaScript(s);
        }

        /** {@inheritDoc} */
        public String encodeForVBScript(String s) {
            return _referenceEncoder.encodeForVBScript(s);
        }

        /** {@inheritDoc} */
        public String encodeForSQL(Codec codec, String s) {
            return _referenceEncoder.encodeForSQL(codec, s);
        }

        /** {@inheritDoc} */
        public String encodeForOS(Codec codec, String s) {
            return _referenceEncoder.encodeForOS(codec, s);
        }

        /** {@inheritDoc} */
        public String encodeForLDAP(String s) {
            return _referenceEncoder.encodeForLDAP(s);
        }

        /** {@inheritDoc} */
        public String encodeForDN(String s) {
            return _referenceEncoder.encodeForDN(s);
        }

        /** {@inheritDoc} */
        public String encodeForXPath(String s) {
            return _referenceEncoder.encodeForXPath(s);
        }

        /** {@inheritDoc} */
        public String encodeForXML(String s) {
            return Encode.forXml(s);
        }

        /** {@inheritDoc} */
        public String encodeForXMLAttribute(String s) {
            return Encode.forXmlAttribute(s);
        }

        /** {@inheritDoc} */
        public String encodeForURL(String s) throws EncodingException {
            return Encode.forUri(s);
        }

        /** {@inheritDoc} */
        public String decodeFromURL(String s) throws EncodingException {
            return _referenceEncoder.decodeFromURL(s);
        }

        /** {@inheritDoc} */
        public String encodeForBase64(byte[] bytes, boolean wrap) {
            return _referenceEncoder.encodeForBase64(bytes, wrap);
        }

        /** {@inheritDoc} */
        public byte[] decodeFromBase64(String s) throws IOException {
            return _referenceEncoder.decodeFromBase64(s);
        }
    }
}

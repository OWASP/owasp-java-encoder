package org.owasp.encoder.esapi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.owasp.encoder.Encode;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;
import org.owasp.esapi.codecs.MySQLCodec;
import org.owasp.esapi.codecs.UnixCodec;
import org.owasp.esapi.reference.DefaultEncoder;

/**
 * ESAPIEncoderTest
 *
 * @author jeffi
 */
public class ESAPIEncoderTest extends TestCase {
    public static Test suite() {
        return new TestSuite(ESAPIEncoderTest.class);
    }

    public void testConfiguredAsEsapiEncoder() {
        assertSame(ESAPIEncoder.getInstance(), ESAPI.encoder());
    }

    public void testJavaEncoderBackedMethods() throws Exception {
        Encoder encoder = ESAPIEncoder.getInstance();
        String input = "<>&\"' /\u03a9";

        assertEquals(Encode.forCssString(input), encoder.encodeForCSS(input));
        assertEquals(Encode.forHtml(input), encoder.encodeForHTML(input));
        assertEquals(Encode.forHtmlAttribute(input), encoder.encodeForHTMLAttribute(input));
        assertEquals(Encode.forJavaScript(input), encoder.encodeForJavaScript(input));
        assertEquals(Encode.forXml(input), encoder.encodeForXML(input));
        assertEquals(Encode.forXmlAttribute(input), encoder.encodeForXMLAttribute(input));
        assertEquals(Encode.forUri(input), encoder.encodeForURL(input));
    }

    public void testDelegatedTextMethods() throws Exception {
        Encoder encoder = ESAPIEncoder.getInstance();
        Encoder reference = DefaultEncoder.getInstance();
        URI uri = new URI("https://example.test/a%20b");

        assertEquals(reference.canonicalize("&lt;"), encoder.canonicalize("&lt;"));
        assertEquals(reference.canonicalize("&lt;", true), encoder.canonicalize("&lt;", true));
        assertEquals(reference.canonicalize("&lt;", true, true),
            encoder.canonicalize("&lt;", true, true));
        assertEquals(reference.getCanonicalizedURI(uri), encoder.getCanonicalizedURI(uri));
        assertEquals(reference.decodeForHTML("&lt;"), encoder.decodeForHTML("&lt;"));
        assertEquals(reference.encodeForVBScript("A"), encoder.encodeForVBScript("A"));
        assertEquals(reference.encodeForOS(new UnixCodec(), "a b"),
            encoder.encodeForOS(new UnixCodec(), "a b"));
        assertEquals(reference.encodeForLDAP("a*b"), encoder.encodeForLDAP("a*b"));
        assertEquals(reference.encodeForLDAP("a*b", true), encoder.encodeForLDAP("a*b", true));
        assertEquals(reference.encodeForDN("CN=Test, O=Example"),
            encoder.encodeForDN("CN=Test, O=Example"));
        assertEquals(reference.encodeForXPath("a'b"), encoder.encodeForXPath("a'b"));
        assertEquals(reference.decodeFromURL("a%20b"), encoder.decodeFromURL("a%20b"));
        assertEquals(reference.encodeForJSON("a\"b"), encoder.encodeForJSON("a\"b"));
        assertEquals(reference.decodeFromJSON("a\\\"b"), encoder.decodeFromJSON("a\\\"b"));
    }

    public void testDelegatedBinaryMethods() throws Exception {
        Encoder encoder = ESAPIEncoder.getInstance();
        Encoder reference = DefaultEncoder.getInstance();
        byte[] input = "ESAPI compatibility".getBytes(StandardCharsets.UTF_8);
        String encoded = encoder.encodeForBase64(input, false);

        assertEquals(reference.encodeForBase64(input, false), encoded);
        assertTrue(Arrays.equals(input, encoder.decodeFromBase64(encoded)));
    }

    public void testDelegatedSqlEncodingHonorsEsapiPolicy() {
        Encoder encoder = ESAPIEncoder.getInstance();
        MySQLCodec codec = new MySQLCodec(MySQLCodec.Mode.ANSI);
        String esapiVersion = Encoder.class.getPackage().getImplementationVersion();

        assertNotNull(esapiVersion);
        if (esapiVersion.startsWith("2.7.")) {
            try {
                encoder.encodeForSQL(codec, "value'");
                fail("ESAPI 2.7 must disable encodeForSQL by default");
            } catch (RuntimeException expected) {
                assertEquals("org.owasp.esapi.errors.NotConfiguredByDefaultException",
                    expected.getClass().getName());
            }
        } else {
            assertEquals(DefaultEncoder.getInstance().encodeForSQL(codec, "value'"),
                encoder.encodeForSQL(codec, "value'"));
        }
    }

    public void testSerialization() throws Exception {
        // Note: ESAPI reference implementation is NOT serializable.  Maybe
        // it will be in the future.  Our implementation is however
        // guaranteed serializable.

        Encoder encoder = ESAPI.encoder();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(encoder);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(
            new ByteArrayInputStream(baos.toByteArray()));

        Encoder deserializedEncoder = (Encoder)ois.readObject();

        assertSame(encoder, deserializedEncoder);
    }
}

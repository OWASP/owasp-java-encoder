package org.owasp.encoder.esapi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;

/**
 * ESAPIEncoderTest
 *
 * @author jeffi
 */
public class ESAPIEncoderTest extends TestCase {
    public static Test suite() {
        return new TestSuite(ESAPIEncoderTest.class);
    }

    public void testEncode() throws Exception {
        // Note: ESAPI reference encodes as: "&#x3c;&#x3e;&#x26;&#x3a9;"
        // That's 25 characters to OWASP Java Encoder's 14.
        assertEquals("&lt;&gt;&amp;\u03a9", ESAPI.encoder().encodeForXML("<>&\u03a9"));
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

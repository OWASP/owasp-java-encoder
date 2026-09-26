package org.owasp.encoder.esapi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.owasp.encoder.Encode;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;
import org.owasp.esapi.errors.ConfigurationException;
import org.owasp.esapi.reference.DefaultEncoder;
import static org.junit.Assert.*;

/** Runs only in a fresh JVM with no test configuration on its classpath. */
public final class ESAPIInitializationProbe {
    private ESAPIInitializationProbe() {}

    public static void main(String[] args) throws Exception {
        ClassLoader loader = ESAPIInitializationProbe.class.getClassLoader();
        for (String resource : new String[] {"ESAPI.properties", ".esapi/ESAPI.properties",
                "esapi/ESAPI.properties"}) {
            assertNull("Configuration leaked onto the probe classpath: " + resource,
                    loader.getResource(resource));
        }
        Path configuration = Paths.get(System.getProperty("org.owasp.esapi.resources"));
        assertFalse(Files.exists(configuration.resolve("ESAPI.properties")));

        Encoder encoder = ESAPIEncoder.getInstance();
        assertBackedMethods(encoder);
        assertSingletonSerialization(encoder);
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                encoder.decodeForHTML("&lt;");
                fail("Delegation must require ESAPI configuration");
            } catch (ConfigurationException expected) {
                // Errors such as ExceptionInInitializerError and
                // NoClassDefFoundError are intentionally not caught.
            }
            try {
                encoder.canonicalize("&lt;");
                fail("Canonicalization must require ESAPI configuration");
            } catch (ConfigurationException expected) {
                // The reference singleton must remain retryable after failure.
            }
            try {
                encoder.encodeForJSON(null);
                fail("JSON must still delegate to ESAPI, including null input");
            } catch (ConfigurationException expected) {
                // Preserve delegation instead of adding a separate JSON policy.
            }
            assertSame(encoder, ESAPIEncoder.getInstance());
            assertBackedMethods(encoder);
            assertSingletonSerialization(encoder);
        }

        // Make configuration available in the SAME JVM. Do not reset any ESAPI
        // singleton or use a new class loader: the adapter itself must recover.
        Files.copy(Paths.get(args[0]), configuration.resolve("ESAPI.properties"));
        assertEquals("<", encoder.decodeForHTML("&lt;"));
        assertEquals("<", encoder.canonicalize("&lt;"));
        assertEquals("<", encoder.canonicalize("&lt;", true));
        assertEquals("<", encoder.canonicalize("&lt;", true, true));
        assertNull(encoder.encodeForJSON(null));
        assertNull(encoder.decodeFromJSON(null));
        assertEquals(DefaultEncoder.getInstance().encodeForJSON("a\"b"),
                encoder.encodeForJSON("a\"b"));
        assertEquals("a\"b", encoder.decodeFromJSON("a\\\"b"));
        assertSame(DefaultEncoder.getInstance(), DefaultEncoder.getInstance());
        assertSame(encoder, ESAPI.encoder());
        assertBackedMethods(encoder);
        assertSingletonSerialization(encoder);
        System.out.println("ESAPI initialization recovery passed");
    }

    private static void assertBackedMethods(Encoder encoder) throws Exception {
        String input = "<>&\"' /\u03a9";
        assertEquals("&lt;", encoder.encodeForHTML("<"));
        assertEquals(Encode.forCssString(input), encoder.encodeForCSS(input));
        assertEquals(Encode.forHtml(input), encoder.encodeForHTML(input));
        assertEquals(Encode.forHtmlAttribute(input), encoder.encodeForHTMLAttribute(input));
        assertEquals(Encode.forJavaScript(input), encoder.encodeForJavaScript(input));
        assertEquals(Encode.forXml(input), encoder.encodeForXML(input));
        assertEquals(Encode.forXmlAttribute(input), encoder.encodeForXMLAttribute(input));
        assertEquals(Encode.forUri(input), encoder.encodeForURL(input));
    }

    private static void assertSingletonSerialization(Encoder encoder) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(encoder);
        }
        try (ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            assertSame(encoder, input.readObject());
        }
        assertSame(encoder, ESAPIEncoder.getInstance());
    }
}

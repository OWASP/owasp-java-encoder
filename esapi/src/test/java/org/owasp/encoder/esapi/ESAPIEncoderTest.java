package org.owasp.encoder.esapi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.rules.TemporaryFolder;
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

    public void testInitializationRecoversWhenConfigurationArrives() throws Exception {
        TemporaryFolder temporary = new TemporaryFolder();
        temporary.create();
        try {
            File work = temporary.newFolder("work");
            File home = temporary.newFolder("home");
            File configuration = temporary.newFolder("configuration");
            File classes = temporary.newFolder("classes");
            String probe = ESAPIInitializationProbe.class.getName();
            String resource = probe.replace('.', '/') + ".class";
            Path target = classes.toPath().resolve(resource);
            Files.createDirectories(target.getParent());
            try (InputStream input = getClass().getResourceAsStream("/" + resource)) {
                assertNotNull(input);
                Files.copy(input, target);
            }
            File fixture = new File(temporary.getRoot(), "fixture.properties");
            try (InputStream input = getClass().getResourceAsStream("/.esapi/ESAPI.properties")) {
                assertNotNull(input);
                Files.copy(input, fixture.toPath());
            }

            // Include production classes and dependency JARs, but never the test
            // output directory: it contains .esapi/ESAPI.properties.
            StringBuilder classpath = new StringBuilder(classes.getAbsolutePath());
            for (Class<?> type : new Class<?>[] {ESAPIEncoder.class, Encode.class}) {
                classpath.append(File.pathSeparator).append(new File(type.getProtectionDomain()
                        .getCodeSource().getLocation().toURI()).getAbsolutePath());
            }
            String testClasspath = System.getProperty("surefire.test.class.path",
                    System.getProperty("java.class.path"));
            for (String entry : testClasspath.split(File.pathSeparator)) {
                File file = new File(entry);
                if (file.isFile() && file.getName().endsWith(".jar")) {
                    classpath.append(File.pathSeparator).append(file.getAbsolutePath());
                }
            }
            File java = new File(System.getProperty("java.home"),
                    File.separatorChar == '\\' ? "bin/java.exe" : "bin/java");
            File output = temporary.newFile("probe.log");
            ProcessBuilder builder = new ProcessBuilder(java.getAbsolutePath(),
                    "-Duser.home=" + home.getAbsolutePath(),
                    "-Dorg.owasp.esapi.resources=" + configuration.getAbsolutePath(),
                    "-cp", classpath.toString(), probe, fixture.getAbsolutePath());
            // Do not inherit JVM options that could supply configuration or a
            // previously customized ESAPI implementation to the isolated process.
            builder.environment().remove("JAVA_TOOL_OPTIONS");
            builder.environment().remove("JDK_JAVA_OPTIONS");
            builder.environment().remove("_JAVA_OPTIONS");
            Process process = builder.directory(work).redirectErrorStream(true)
                    .redirectOutput(output).start();
            try {
                assertTrue("Initialization probe timed out", process.waitFor(30, TimeUnit.SECONDS));
                String text = new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);
                assertEquals(text, 0, process.exitValue());
                assertTrue(text, text.contains("ESAPI initialization recovery passed"));
            } finally {
                process.destroyForcibly();
            }
        } finally {
            temporary.delete();
        }
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

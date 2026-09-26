/* Copyright (c) 2026 OWASP. All rights reserved. BSD-3-Clause. */
package fixture;

import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.w3c.dom.Element;

/** Compiles and serves the actual packaged TLD surface in an isolated engine JVM. */
public final class JspEngineTest {
    private record Binding(String kind, String name) { }
    private record Descriptor(String uri, List<Binding> bindings) { }
    private record Scenario(String name, String expression, String input, String coerced) { }
    private record Page(String name, byte[] expected, String rejection) { }
    private static final String DIRECTIVE = "<%@page contentType=\"text/plain; charset=UTF-8\" "
        + "pageEncoding=\"UTF-8\" trimDirectiveWhitespaces=\"true\"%>";
    private static final List<Scenario> CASES = List.of(
        new Scenario("hostile", "${fixtureValue}", "</script><img src=x onerror=alert(1)>&\"'`$\\\r\n\t\u0001\u0085\u2028é😀", null),
        new Scenario("boundary", "${fixtureValue}", "x".repeat(2047) + "<&\"'😀" + "y".repeat(2049), null),
        new Scenario("empty", "${fixtureValue}", "", ""),
        new Scenario("missing", "${missingValue}", null, ""),
        new Scenario("null-el", "${null}", null, ""),
        new Scenario("number", "${42}", null, "42"),
        new Scenario("boolean", "${true}", null, "true"),
        new Scenario("null-scriptlet", "<%= (String)null %>", null, null));

    public static void main(String[] args) throws Exception {
        Path core = Path.of(args[0]).toAbsolutePath();
        Path adapter = Path.of(args[1]).toAbsolutePath();
        Path base = Files.createTempDirectory(Path.of(args[2]), "run-");
        Path web = Files.createDirectories(base.resolve("webapp"));
        Path libs = Files.createDirectories(web.resolve("WEB-INF/lib"));
        Files.copy(core, libs.resolve(core.getFileName()));
        Files.copy(adapter, libs.resolve(adapter.getFileName()));
        // Do not add reactor classes, adapters or alternate APIs to the engine/host classpath.
        try (URLClassLoader oracle = new URLClassLoader(new URL[]{core.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Class<?> encode = Class.forName("org.owasp.encoder.Encode", true, oracle);
            List<Page> pages = new ArrayList<>();
            Set<String> discovered = new LinkedHashSet<>();
            Set<String> exercised = new LinkedHashSet<>();
            int renderAssertions = 0;
            try (JarFile jar = new JarFile(adapter.toFile())) {
                for (String file : List.of("java-encoder.tld", "java-encoder-advanced.tld")) {
                    Descriptor descriptor = descriptor(jar, "META-INF/" + file);
                    String prefix = DIRECTIVE + "<%@taglib prefix=\"e\" uri=\"" + descriptor.uri() + "\"%>";
                    for (Binding binding : descriptor.bindings()) {
                        if (!discovered.add(file + ":" + binding)) throw new AssertionError("Duplicate binding " + binding);
                    }
                    for (Scenario scenario : CASES) {
                        StringBuilder jsp = new StringBuilder(prefix);
                        if (scenario.input() != null) {
                            String b64 = Base64.getEncoder().encodeToString(scenario.input().getBytes(StandardCharsets.UTF_8));
                            jsp.append("<%request.setAttribute(\"fixtureValue\", new String(java.util.Base64.getDecoder().decode(\"")
                                .append(b64).append("\"), java.nio.charset.StandardCharsets.UTF_8));%>");
                        }
                        StringBuilder expected = new StringBuilder();
                        for (Binding binding : descriptor.bindings()) {
                            boolean tag = binding.kind().equals("tag");
                            String marker = "[" + binding.kind() + ":" + binding.name() + "]";
                            jsp.append(marker);
                            expected.append(marker);
                            if (tag) {
                                jsp.append("<e:").append(binding.name()).append(" value=\"")
                                    .append(scenario.expression()).append("\"/>");
                            } else {
                                String expression = scenario.name().equals("null-scriptlet") ? "null"
                                    : scenario.expression().substring(2, scenario.expression().length() - 1);
                                jsp.append("${e:").append(binding.name()).append('(').append(expression).append(")}");
                            }
                            String input = scenario.coerced() != null ? scenario.coerced() : scenario.input();
                            // JSP String attributes preserve scriptlet null; EL coerces null/missing to "".
                            if (!tag && scenario.name().equals("null-scriptlet")) input = "";
                            if (tag) {
                                StringWriter writer = new StringWriter();
                                encode.getMethod(binding.name(), Writer.class, String.class).invoke(null, writer, input);
                                expected.append(writer);
                            } else {
                                expected.append(encode.getMethod(binding.name(), String.class).invoke(null, input));
                            }
                            exercised.add(file + ":" + binding);
                            renderAssertions++;
                        }
                        String name = file + "-" + scenario.name() + ".jsp";
                        Files.writeString(web.resolve(name), jsp, StandardCharsets.UTF_8);
                        pages.add(new Page(name, expected.toString().getBytes(StandardCharsets.UTF_8), null));
                    }
                    for (Binding binding : descriptor.bindings()) {
                        if (!binding.kind().equals("tag")) continue;
                        String name = file + "-" + binding.name();
                        Files.writeString(web.resolve(name + "-body.jsp"), prefix + "<e:" + binding.name()
                            + " value=\"x\">forbidden body</e:" + binding.name() + ">");
                        pages.add(new Page(name + "-body.jsp", null, "must be empty"));
                        Files.writeString(web.resolve(name + "-missing.jsp"), prefix + "<e:" + binding.name() + "/>");
                        pages.add(new Page(name + "-missing.jsp", null, "attribute [value] is mandatory"));
                    }
                }
            }
            if (discovered.isEmpty() || !discovered.equals(exercised)) throw new AssertionError("Coverage gap " + discovered);
            Tomcat tomcat = new Tomcat();
            tomcat.setBaseDir(base.resolve("tomcat").toString());
            tomcat.setPort(0);
            tomcat.getConnector().setProperty("address", "127.0.0.1");
            Context context = tomcat.addWebapp("", web.toString());
            ((StandardJarScanner) context.getJarScanner()).setScanClassPath(false);
            try {
                tomcat.start();
                if (!context.getState().isAvailable()) throw new AssertionError("Webapp failed to start");
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                for (Page page : pages) {
                    URI uri = URI.create("http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/" + page.name());
                    HttpResponse<byte[]> response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30))
                        .build(), HttpResponse.BodyHandlers.ofByteArray());
                    if (page.rejection() == null) {
                        if (response.statusCode() != 200 || !Arrays.equals(page.expected(), response.body())) {
                            throw new AssertionError(page.name() + " HTTP " + response.statusCode() + " expected bytes="
                                + page.expected().length + " actual bytes=" + response.body().length + "\n"
                                + new String(response.body(), StandardCharsets.UTF_8));
                        }
                    } else {
                        String message = new String(response.body(), StandardCharsets.UTF_8);
                        if (response.statusCode() != 500 || !message.contains("JasperException")
                                || !message.toLowerCase(java.util.Locale.ROOT).contains(page.rejection())) {
                            throw new AssertionError("Expected JSP translation rejection: " + page.name() + "\n" + message);
                        }
                    }
                }
                System.out.println("PASS " + adapter.getFileName() + ": " + discovered.size() + " TLD bindings; "
                    + renderAssertions + " exact-byte assertions; " + (pages.size() - CASES.size() * 2)
                    + " translation rejections; " + org.apache.catalina.util.ServerInfo.getServerInfo());
            } finally {
                try { tomcat.stop(); } finally { tomcat.destroy(); }
            }
        }
        // Retain generated JSPs/engine diagnostics under target for failed-run inspection.
    }

    private static Descriptor descriptor(JarFile jar, String path) throws Exception {
        var entry = jar.getJarEntry(path);
        if (entry == null) throw new AssertionError("Missing packaged descriptor " + path);
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Element root;
        try (var stream = jar.getInputStream(entry)) {
            root = factory.newDocumentBuilder().parse(stream).getDocumentElement();
        }
        List<Binding> bindings = new ArrayList<>();
        for (String kind : List.of("tag", "function")) {
            var elements = root.getElementsByTagNameNS("*", kind);
            if (elements.getLength() == 0) throw new AssertionError("Empty " + kind + " list in " + path);
            for (int i = 0; i < elements.getLength(); i++) {
                String name = text((Element) elements.item(i), "name");
                if (!name.matches("for[A-Za-z0-9]+")) throw new AssertionError(name);
                bindings.add(new Binding(kind, name));
            }
        }
        String uri = text(root, "uri");
        if (!uri.matches("[A-Za-z0-9:/_.#-]+")) throw new AssertionError(uri);
        return new Descriptor(uri, bindings);
    }

    private static String text(Element element, String name) {
        var nodes = element.getElementsByTagNameNS("*", name);
        if (nodes.getLength() == 0) throw new AssertionError("Missing " + name);
        return nodes.item(0).getTextContent().trim();
    }
}

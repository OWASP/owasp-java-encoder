package org.owasp.encoder.testing.jakarta_test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.owasp.encoder.Encode;
import static org.junit.jupiter.api.Assertions.*;

/** Starts the executable WAR itself, including its packaged JSP/JSTL dependencies. */
class PackagedWarIT {
    @Test
    @Timeout(90)
    void executableWarStartsAndRendersPackagedViews() throws Exception {
        Path war = Path.of(System.getProperty("fixture.war")).toAbsolutePath();
        try (ZipFile zip = new ZipFile(war.toFile())) {
            for (String prefix : new String[]{"WEB-INF/lib/jakarta.servlet.jsp.jstl-api-",
                    "WEB-INF/lib/jakarta.servlet.jsp.jstl-", "WEB-INF/lib/encoder-jakarta-jsp-"}) {
                assertTrue(zip.stream().anyMatch(entry -> entry.getName().startsWith(prefix)), prefix);
            }
            assertNotNull(zip.getEntry("WEB-INF/jsp/view-items.jsp"));
            for (String prefix : new String[]{"WEB-INF/lib/jakarta.el-api-", "WEB-INF/lib/jakarta.servlet-api-",
                    "WEB-INF/lib/jakarta.servlet.jsp-api-"}) {
                assertTrue(zip.stream().noneMatch(entry -> entry.getName().startsWith(prefix)), prefix);
            }
        }
        Path log = war.getParent().resolve("packaged-war.log");
        Process server = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-jar", war.toString(), "--server.address=127.0.0.1", "--server.port=0")
            .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            var portPattern = Pattern.compile("Tomcat started on port (\\d+)");
            int port = 0;
            long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
            while (System.nanoTime() < deadline && server.isAlive()) {
                var matcher = portPattern.matcher(Files.readString(log));
                if (matcher.find()) { port = Integer.parseInt(matcher.group(1)); break; }
                Thread.sleep(100);
            }
            assertTrue(port > 0, () -> "WAR failed to start; inspect " + log);
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            String root = "http://127.0.0.1:" + port + "/jakarta-test";
            HttpResponse<String> index = get(client, root + "/");
            assertEquals(200, index.statusCode());
            assertTrue(index.body().contains("href=\"/jakarta-test/item/viewItems\""), index.body());
            HttpResponse<String> page = get(client, root + "/item/viewItems");
            assertEquals(200, page.statusCode(), page.body());
            assertEquals(Encode.forHtml("top<script>alert(1)</script>"), cell(page.body(), "b2"));
            assertEquals(Encode.forHtml("fancy <script>alert(1)</script>"), cell(page.body(), "c2"));
            assertFalse(page.body().contains("<script>"));
            assertTrue(page.body().contains("<!DOCTYPE html>"));
        } finally {
            server.destroy();
            if (!server.waitFor(5, TimeUnit.SECONDS)) {
                server.destroyForcibly();
                assertTrue(server.waitFor(5, TimeUnit.SECONDS), "WAR process did not stop");
            }
        }
    }

    private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String cell(String html, String id) {
        var matcher = Pattern.compile("<td id=\"" + id + "\">(.*?)</td>", Pattern.DOTALL).matcher(html);
        assertTrue(matcher.find(), "Missing cell " + id);
        return matcher.group(1);
    }
}

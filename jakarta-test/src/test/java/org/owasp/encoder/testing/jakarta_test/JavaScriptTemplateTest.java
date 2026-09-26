package org.owasp.encoder.testing.jakarta_test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.owasp.encoder.Encode;
import org.testcontainers.containers.BrowserWebDriverContainer;

/** JavaScript grammar and HTML parser checks in the browser used by CI. */
class JavaScriptTemplateTest {
    private static BrowserWebDriverContainer<?> container;
    private static RemoteWebDriver browser;
    private static final List<UnaryOperator<String>> ENCODERS = Arrays.asList(
        Encode::forJavaScript, Encode::forJavaScriptAttribute,
        Encode::forJavaScriptBlock, Encode::forJavaScriptSource);
    private static final String[] INPUTS = {
        "", "plain", "$", "`", "{executed=true}", "${", "${executed=true}",
        "hell`;executed=true;value=`o", "\\${executed=true}", "\\`", "end$",
        "'\"\\\r\n\t\b\f\u0000\u000b\u2028\u2029",
        "\u007f\u0080\u0085\u009f\u00a0\u00e9\u1234\ud83d\ude00",
        "</script><script>executed=true</script>", "<!--<script>-->",
        "&quot;&#96;&#36;{executed=true}"
    };

    @BeforeAll
    static void startBrowser() {
        ChromeOptions options = new ChromeOptions().addArguments("--headless=new");
        // Local verification: -Dencoder.browser.local=true -Dtest=JavaScriptTemplateTest
        if (Boolean.getBoolean("encoder.browser.local")) {
            browser = new ChromeDriver(options);
        } else {
            container = BrowserFixture.container(options);
            container.start();
            browser = new RemoteWebDriver(container.getSeleniumAddress(), options);
        }
    }

    @Test
    void inputCannotCompleteInterpolationAfterTrustedDollar() {
        String input = "{executed=true}";
        for (int mode = 0; mode < ENCODERS.size(); mode++) {
            String encoded = ENCODERS.get(mode).apply(input);
            assertEquals(Arrays.asList("$" + input, false), evaluate(
                "var executed=false;var value=`$" + encoded
                + "`;return [value,executed];"), "mode=" + mode);
        }
    }

    @AfterAll
    static void stopBrowser() {
        try {
            if (browser != null) browser.quit();
        } finally {
            if (container != null) container.stop();
        }
    }

    @Test
    void preservesQuotedStringsAndOrdinaryTemplateValuesWithoutExecutingInput() {
        for (int mode = 0; mode < ENCODERS.size(); mode++) {
            for (String input : INPUTS) {
                String encoded = ENCODERS.get(mode).apply(input);
                for (String quote : new String[]{"'", "\"", "`"}) {
                    String source = "var executed=false;var value=" + quote + encoded
                        + quote + ";return [value,executed];";
                    assertEquals(Arrays.asList(input, false), evaluate(source),
                        "mode=" + mode + ", quote=" + quote + ", input=" + input);
                }
                // Encoder output is literal text next to trusted substitutions too.
                // An array preserves CR/LF; WebDriver normalizes a bare string result.
                assertEquals(Arrays.asList("start" + input + "end"),
                    evaluate("return [`${'start'}" + encoded + "${'end'}`];"));
            }
        }
    }

    @Test
    void survivesHtmlParsingInEachSupportedEmbeddingContext() {
        for (String input : INPUTS) {
            for (UnaryOperator<String> encoder : Arrays.<UnaryOperator<String>>asList(
                    Encode::forJavaScript, Encode::forJavaScriptBlock)) {
                String source = "var executed=false;var value=`" + encoder.apply(input)
                    + "`;return [value,executed];";
                String html = "<script>" + source + "</script><p id=after>after</p>";
                assertEquals(Arrays.asList(1L, true, input, false), browser.executeScript(
                    "const doc=new DOMParser().parseFromString(arguments[0],'text/html');"
                    + "const scripts=doc.querySelectorAll('script');"
                    + "return [scripts.length,!!doc.querySelector('#after'),"
                    + "...new Function(scripts[0].textContent)()];", html));
            }
            for (UnaryOperator<String> encoder : Arrays.<UnaryOperator<String>>asList(
                    Encode::forJavaScript, Encode::forJavaScriptAttribute)) {
                for (String quote : new String[]{"'", "\""}) {
                    String source = "var executed=false;var value=`" + encoder.apply(input)
                        + "`;return [value,executed];";
                    String html = "<button onclick=" + quote + source + quote + ">ok</button>";
                    assertEquals(Arrays.asList(input, false), browser.executeScript(
                        "const doc=new DOMParser().parseFromString(arguments[0],'text/html');"
                        + "return new Function(doc.querySelector('button').getAttribute('onclick'))();",
                        html));
                }
            }
        }
    }

    @Test
    void rawTemplateValuesAreOutsideTheRoundTripContract() {
        String input = "$`\\\n";
        for (UnaryOperator<String> encoder : ENCODERS) {
            String encoded = encoder.apply(input);
            Object raw = evaluate("return String.raw`" + encoded + "`;");
            assertEquals(encoded, raw);
            assertNotEquals(input, raw);
        }
    }

    @Test
    void preservesEveryLoneSurrogateAndControlThroughUtf8AndJavaScriptParsing() throws Exception {
        StringBuilder input = new StringBuilder("\u00a0\u00ff\ud83d\ude00\udc00\ud800|");
        for (int ch = 0x7f; ch <= 0x9f; ch++) {
            input.append((char) ch);
        }
        for (int ch = Character.MIN_SURROGATE; ch <= Character.MAX_SURROGATE; ch++) {
            input.append((char) ch).append('|');
        }
        String value = input.toString();
        // Numeric code units avoid asking WebDriver to serialize lone surrogates.
        List<Long> expected = value.chars().mapToObj(ch -> (long) ch).toList();
        String[] methods = {"forJavaScript", "forJavaScriptAttribute",
            "forJavaScriptBlock", "forJavaScriptSource"};
        for (int mode = 0; mode < methods.length; mode++) {
            String encoded = ENCODERS.get(mode).apply(value);
            for (boolean writerApi : new boolean[]{false, true}) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (Writer out = new OutputStreamWriter(bytes, StandardCharsets.UTF_8.newEncoder())) {
                    if (writerApi) {
                        Encode.class.getMethod(methods[mode], Writer.class, String.class)
                            .invoke(null, out, value);
                    } else {
                        out.write(encoded);
                    }
                }
                String transported = bytes.toString(StandardCharsets.UTF_8);
                assertEquals(encoded, transported);
                for (String quote : new String[]{"'", "\"", "`"}) {
                    String source = "var value=" + quote + transported + quote
                        + ";return Array.from({length:value.length},(_,i)=>value.charCodeAt(i));";
                    assertEquals(expected, evaluate(source), methods[mode] + ", writer=" + writerApi);
                }
                String source = "var value=`" + transported
                    + "`;return Array.from({length:value.length},(_,i)=>value.charCodeAt(i));";
                if (mode == 0 || mode == 2) {
                    assertEquals(expected, browser.executeScript(
                        "const doc=new DOMParser().parseFromString(arguments[0],'text/html');"
                        + "return new Function(doc.querySelector('script').textContent)();",
                        "<script>" + source + "</script>"));
                }
                if (mode == 0 || mode == 1) {
                    assertEquals(expected, browser.executeScript(
                        "const doc=new DOMParser().parseFromString(arguments[0],'text/html');"
                        + "return new Function(doc.querySelector('button').getAttribute('onclick'))();",
                        "<button onclick=\"" + source + "\">ok</button>"));
                }
            }
        }
    }

    private static Object evaluate(String source) {
        return browser.executeScript("return new Function(arguments[0])();", source);
    }
}

package org.owasp.encoder.testing.jakarta_test;

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
        "\u00e9\u0085\u1234\ud83d\ude00",
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
            container = new BrowserWebDriverContainer<>().withCapabilities(options);
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
        if (browser != null) {
            browser.quit();
        }
        if (container != null) {
            container.stop();
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

    private static Object evaluate(String source) {
        return browser.executeScript("return new Function(arguments[0])();", source);
    }
}

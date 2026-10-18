package org.owasp.encoder.testing.jakarta_test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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
import org.owasp.encoder.EncodedWriter;
import org.owasp.encoder.Encoders;
import org.testcontainers.containers.BrowserWebDriverContainer;

/** JavaScript grammar and HTML parser checks in the browser used by CI. */
class JavaScriptTemplateTest {
    private static BrowserWebDriverContainer<?> container;
    private static RemoteWebDriver browser;
    private static final List<UnaryOperator<String>> ENCODERS = Arrays.asList(
        Encode::forJavaScript, Encode::forJavaScriptAttribute,
        Encode::forJavaScriptBlock, Encode::forJavaScriptSource);
    private static final List<UnaryOperator<String>> SCRIPT_ENCODERS = Arrays.asList(
        Encode::forJavaScript, Encode::forJavaScriptBlock);
    private static final String[] SCRIPT_DELIMITERS = {">", " ", "\t", "\n", "\r", "\f", "/"};
    private static final String SCRIPT_LETTERS = "CIPRSTciprst";
    private static final String[] HARMLESS_NAME_SUFFIXES = {
        "ure", "ed", "s", "X", "0", "_", "Z", "ural", "ing"
    };
    private static final String VERIFY_HTML_BATCH = """
        const failures=[];
        for (const [label,source,expected] of arguments[0]) {
            const doc=new DOMParser().parseFromString(
                '<!doctype html><html><body><script id=subject>'+source+
                '</script><p id=after>after</p><script id=tail>/* tail */</script>'+
                '</body></html>','text/html');
            const subject=doc.getElementById('subject');
            const after=doc.getElementById('after');
            const tail=doc.getElementById('tail');
            // HTML normalizes raw CR/CRLF before tokenization. Encoded CR stays escaped.
            const exact=subject && subject.textContent===source.replace(/\\r\\n?/g,'\\n');
            const contained=!!(exact && doc.scripts.length===2 &&
                subject.parentElement===doc.body && doc.body.childElementCount===3 &&
                after && after.textContent==='after' && after.previousElementSibling===subject &&
                tail && after.nextElementSibling===tail && tail.textContent==='/* tail */');
            if (contained!==expected && failures.length<8) {
                failures.push([label,expected,contained,doc.scripts.length,
                    subject ? subject.textContent.slice(0,250) : null]);
            }
        }
        return failures;
        """;
    private static final String VERIFY_VALUE_BATCH = """
        const failures=[];
        for (const [label,source,metadata] of arguments[0]) {
            const [input,mode,quote]=metadata;
            try {
                const sources=[source];
                if (mode===0 || mode===2) {
                    const doc=new DOMParser().parseFromString(
                        '<script>'+source+'</script><p id=after>after</p>','text/html');
                    if (doc.scripts.length!==1 || !doc.getElementById('after') ||
                        doc.scripts[0].textContent!==source) throw Error('script containment');
                    sources.push(doc.scripts[0].textContent);
                }
                if ((mode===0 || mode===1) && quote==='`') {
                    for (const outer of ["'",'"']) {
                        const doc=new DOMParser().parseFromString(
                            '<button onclick='+outer+source+outer+'>ok</button>','text/html');
                        sources.push(doc.querySelector('button').getAttribute('onclick'));
                    }
                }
                for (const candidate of sources) {
                    const result=new Function(candidate)();
                    if (result[0]!==input || result[1]!==false) throw Error('value/execution changed');
                }
            } catch (error) {
                if (failures.length<8) failures.push([label,String(error)]);
            }
        }
        return failures;
        """;
    private static final String VERIFY_LIVE_FRAMES = """
        const fixtures=arguments[0],done=arguments[arguments.length-1],failures=[];
        let next=0;
        async function worker() {
            while (next<fixtures.length) {
                const [label,source,value,expectedSafe,expectedInjection]=fixtures[next++];
                const expectedValue=Array.isArray(value) ? String.fromCharCode(...value) : value;
                await new Promise(resolve=>{
                    const frame=document.createElement('iframe');
                    frame.hidden=true;
                    let finished=false,timer;
                    const finish=failure=>{
                        if (finished) return;
                        finished=true;
                        clearTimeout(timer);
                        if (failure && failures.length<8) failures.push([label,failure]);
                        frame.remove();
                        resolve();
                    };
                    frame.onload=()=>{
                        try {
                            const win=frame.contentWindow,doc=frame.contentDocument;
                            const subject=doc.getElementById('subject'),after=doc.getElementById('after');
                            const tail=doc.getElementById('tail'),probe=win.__encoderProbe;
                            const injected=win.__encoderInjected===true;
                            const safe=!!(probe && probe.value===expectedValue && probe.executed===false &&
                                !injected && win.__encoderTail===true && doc.scripts.length===2 &&
                                subject && subject.textContent===source && subject.parentElement===doc.body &&
                                after && after.textContent==='after' && after.previousElementSibling===subject &&
                                tail && after.nextElementSibling===tail && doc.body.childElementCount===3 &&
                                tail.textContent==='window.__encoderTail=true;' && !doc.getElementById('pwn'));
                            finish(safe===expectedSafe && injected===expectedInjection &&
                                (!expectedInjection || doc.getElementById('pwn')) ? null :
                                'safe='+safe+', injected='+injected+', scripts='+doc.scripts.length);
                        } catch (error) { finish(String(error)); }
                    };
                    timer=setTimeout(()=>finish('iframe load timeout'),5000);
                    frame.srcdoc='<!doctype html><html><body><script id=subject>'+source+
                        '</script><p id=after>after</p><script id=tail>window.__encoderTail=true;</script>'+
                        '</body></html>';
                    document.body.append(frame);
                });
            }
        }
        Promise.all(Array.from({length:8},worker)).then(()=>done([fixtures.length,failures]))
            .catch(error=>done([-1,String(error)]));
        """;
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

    @Test
    void containsEveryNonemptyPartitionOfEveryCaseAndDelimiterOfScriptNames() {
        BrowserBatch batch = new BrowserBatch(VERIFY_HTML_BATCH);
        for (int mode = 0; mode < SCRIPT_ENCODERS.size(); mode++) {
            UnaryOperator<String> encoder = SCRIPT_ENCODERS.get(mode);
            for (int bits = 0; bits < 64; bits++) {
                String name = scriptCase(bits);
                for (int delimiter = 0; delimiter < SCRIPT_DELIMITERS.length; delimiter++) {
                    for (boolean closing : new boolean[]{false, true}) {
                        String leading = closing ? "</" : "<";
                        String nameToken = leading + name;
                        String token = nameToken + SCRIPT_DELIMITERS[delimiter]
                            + (delimiter == 0 ? "" : ">");
                        // An insertion may include the final >, but cannot start
                        // after a trusted delimiter already committed the token.
                        for (int start = 0; start <= nameToken.length(); start++) {
                            for (int end = start + 1; end <= token.length(); end++) {
                                String input = token.substring(start, end);
                                String encoded = encoder.apply(input);
                                String left = token.substring(0, start);
                                String right = token.substring(end);
                                String label = "mode=" + mode + ",case=" + bits + ",delimiter="
                                    + delimiter + ",closing=" + closing + ",slice=" + start + ":" + end;
                                for (ScriptState state : ScriptState.values()) {
                                    batch.add(label + ",state=" + state,
                                        state.source(left + encoded + right), true);
                                    if (start >= leading.length() && end <= leading.length() + name.length()
                                            && (closing || state == ScriptState.ESCAPED)) {
                                        // Mutate actual output by removing only script-letter
                                        // escapes. Every such lost barrier must be detected.
                                        String unsafe = restoreScriptLetterEscapes(encoded);
                                        assertNotEquals(encoded, unsafe, label);
                                        batch.add("UNSAFE letter mutation: " + label + ",state=" + state,
                                            state.source(left + unsafe + right), false);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        batch.finish("Script-name partition matrix");
        assertEquals(332160, batch.cases);
        assertEquals(75264, batch.negativeControls);
    }

    @Test
    void containsEveryNonemptyHtmlCommentTokenPartitionWithStateSensitiveControls() {
        BrowserBatch batch = new BrowserBatch(VERIFY_HTML_BATCH);
        for (int mode = 0; mode < SCRIPT_ENCODERS.size(); mode++) {
            for (String token : new String[]{"<!--", "-->"}) {
                for (int start = 0; start < token.length(); start++) {
                    for (int end = start + 1; end <= token.length(); end++) {
                        String input = token.substring(start, end);
                        String encoded = SCRIPT_ENCODERS.get(mode).apply(input);
                        String joined = token.substring(0, start) + encoded + token.substring(end);
                        String label = "mode=" + mode + ",token=" + token + ",slice=" + start + ":" + end;
                        for (ScriptState state : ScriptState.values()) {
                            String extra = "";
                            boolean unsafeControl = false;
                            if (token.equals("<!--") && state == ScriptState.DATA) {
                                // A raw opener would let the following trusted name enter
                                // double-escaped state and consume the page tail.
                                extra = "\n//<script>\n";
                                unsafeControl = true;
                            } else if (token.equals("-->") && state == ScriptState.ESCAPED) {
                                // A raw closer would leave escaped state too early, so
                                // the trusted reset end tag below would close the script.
                                extra = "\n//<script>\n//</script>\n";
                                unsafeControl = true;
                            } else if (token.equals("-->") && state == ScriptState.DOUBLE_ESCAPED) {
                                unsafeControl = true;
                            }
                            batch.add(label + ",state=" + state, state.source(joined, extra), true);
                            if (unsafeControl) {
                                batch.add("UNSAFE raw comment: " + label + ",state=" + state,
                                    state.source(token, extra), false);
                            }
                        }
                    }
                }
            }
        }
        batch.finish("HTML comment partition matrix");
        assertEquals(140, batch.cases);
        assertEquals(44, batch.negativeControls);
    }

    @Test
    void harmlessLongerNamesAndNonHtmlDelimitersStayInsideTheScript() {
        BrowserBatch batch = new BrowserBatch(VERIFY_HTML_BATCH);
        for (int bits = 0; bits < 64; bits++) {
            String script = scriptCase(bits);
            for (String suffix : HARMLESS_NAME_SUFFIXES) {
                for (int mode = 0; mode < SCRIPT_ENCODERS.size(); mode++) {
                    String encoded = SCRIPT_ENCODERS.get(mode).apply(script + suffix);
                    for (boolean closing : new boolean[]{false, true}) {
                        String token = (closing ? "</" : "<") + encoded + ">";
                        for (ScriptState state : ScriptState.values()) {
                            batch.add("harmless mode=" + mode + ",name=" + script + suffix
                                + ",closing=" + closing + ",state=" + state, state.source(token), true);
                        }
                    }
                }
            }
            // These raw characters are deliberately NOT HTML name delimiters.
            // This also guards the browser oracle against overmatching a prefix.
            for (String nonDelimiter : new String[]{"=", "\u000b", "\u0085", "\u00a0", "\u2028", "\u2029"}) {
                for (boolean closing : new boolean[]{false, true}) {
                    String token = (closing ? "</" : "<") + script + nonDelimiter + ">";
                    for (ScriptState state : ScriptState.values()) {
                        batch.add("raw harmless case=" + bits + ",character=" + (int) nonDelimiter.charAt(0)
                            + ",closing=" + closing + ",state=" + state, state.source(token), true);
                    }
                }
            }
        }
        batch.finish("Harmless names and non-HTML delimiter controls");
        assertEquals(9216, batch.cases);
        assertEquals(0, batch.negativeControls);
    }

    @Test
    void preservesScriptNamesAndIsolatedLettersAcrossQuotesTemplatesAndHtmlEmbedding() {
        List<String> values = new ArrayList<>(Arrays.asList(INPUTS));
        for (char ch : SCRIPT_LETTERS.toCharArray()) values.add(Character.toString(ch));
        for (String suffix : HARMLESS_NAME_SUFFIXES) values.add("script" + suffix);
        for (int bits = 0; bits < 64; bits++) {
            String name = scriptCase(bits);
            values.add(name);
            for (String delimiter : SCRIPT_DELIMITERS) {
                String tail = delimiter.equals(">") ? "" : ">";
                values.add("<" + name + delimiter + tail);
                values.add("</" + name + delimiter + tail);
            }
        }
        String[] pieces = {"script", "</ScRiPt>", "<scripture>", "<!--", "-->", "'\"", "`",
            "${executed=true}", "\\", "\r\n\t\f", "&quot;", " !-/<> ", "CIPRSTciprst",
            "http://example.test/", "\u00a0\u00e9\u1234\ud83d\ude00", "\u2028\u2029"};
        Random random = new Random(243);
        for (int index = 0; index < 128; index++) {
            StringBuilder value = new StringBuilder();
            int count = 8 + random.nextInt(25);
            for (int part = 0; part < count; part++) value.append(pieces[random.nextInt(pieces.length)]);
            values.add(value.toString());
        }
        BrowserBatch batch = new BrowserBatch(VERIFY_VALUE_BATCH);
        for (int mode = 0; mode < ENCODERS.size(); mode++) {
            for (int index = 0; index < values.size(); index++) {
                String input = values.get(index);
                String encoded = ENCODERS.get(mode).apply(input);
                for (String quote : new String[]{"'", "\"", "`"}) {
                    String source = "var executed=false;var value=" + quote + encoded
                        + quote + ";return [value,executed];";
                    batch.add("mode=" + mode + ",value=" + index + ",quote=" + quote,
                        source, Arrays.asList(input, mode, quote));
                }
            }
        }
        batch.finish("Script alphabet and token JavaScript value matrix");
        assertEquals(values.size() * ENCODERS.size() * 3, batch.cases);
    }

    @Test
    void liveHtmlPagesExecuteOnlyTheIntendedScriptsAndPreserveSplitValues() {
        List<List<Object>> fixtures = new ArrayList<>();
        String marker = "<span id=pwn>unintended</span>";
        for (int mode = 0; mode < SCRIPT_ENCODERS.size(); mode++) {
            UnaryOperator<String> encoder = SCRIPT_ENCODERS.get(mode);
            for (int bits : new int[]{0, 63}) {
                String name = scriptCase(bits);
                for (int index = 0; index < name.length(); index++) {
                    String input = name.substring(index, index + 1);
                    for (ScriptState state : ScriptState.values()) {
                        String left = (state == ScriptState.DATA ? "</" :
                            state == ScriptState.ESCAPED ? "<!--<" : "<!--<script></")
                            + name.substring(0, index);
                        String right = name.substring(index + 1) + ">"
                            + (state == ScriptState.DOUBLE_ESCAPED ? "</script>" : "") + marker;
                        for (String quote : new String[]{"'", "\"", "`"}) {
                            fixtures.add(liveFrame("mode=" + mode + ",name=" + name + ",letter=" + index
                                + ",state=" + state + ",quote=" + quote,
                                left + encoder.apply(input) + right, left + input + right, quote, true, false));
                        }
                    }
                }
            }
            for (String[] parts : new String[][]{{"</scrip", "tone", ">"}, {"</", "scripture", ">"},
                    {"<", "scripture", ">"}, {"</scr", "iption", ">"}, {"", "javascript", ""},
                    {"", "http://google.com", ""}, {"", "CIPRSTciprst", ""}}) {
                for (String quote : new String[]{"'", "\"", "`"}) {
                    fixtures.add(liveFrame("longer mode=" + mode + ",input=" + parts[1] + ",quote=" + quote,
                        parts[0] + encoder.apply(parts[1]) + parts[2], String.join("", parts), quote, true, false));
                }
            }
            for (ScriptState state : ScriptState.values()) {
                String left = state == ScriptState.DATA ? "</scr" :
                    state == ScriptState.ESCAPED ? "<!--<scr" : "<!--<script></scr";
                String right = "pt>" + (state == ScriptState.DOUBLE_ESCAPED ? "</script>" : "") + marker;
                if (state == ScriptState.DATA) right += "<script>window.__encoderInjected=true;//";
                String encoded = encoder.apply("i");
                for (String quote : new String[]{"'", "\"", "`"}) {
                    String label = "mode=" + mode + ",state=" + state + ",quote=" + quote;
                    if (state == ScriptState.DATA) {
                        fixtures.add(liveFrame("injection barrier " + label, left + encoded + right,
                            left + "i" + right, quote, true, false));
                    }
                    fixtures.add(liveFrame("UNSAFE live letter mutation " + label,
                        left + restoreScriptLetterEscapes(encoded) + right, left + "i" + right,
                        quote, false, state == ScriptState.DATA));
                }
            }
        }
        assertEquals(282, fixtures.size());
        assertEquals(Arrays.asList((long) fixtures.size(), Collections.emptyList()),
            browser.executeAsyncScript(VERIFY_LIVE_FRAMES, fixtures));
        System.out.println("Live iframe execution matrix: 282 pages, 18 unsafe controls detected, "
            + "including 6 executed injection controls");
    }

    @Test
    void liveExplicitFlushCannotWithholdALetterAndJoinTrustedTextIntoAnEndTag() throws Exception {
        List<List<Object>> fixtures = new ArrayList<>();
        String suffix = "ipt><span id=pwn>unintended</span>"
            + "<script>window.__encoderInjected=true;//";
        for (String context : new String[]{Encoders.JAVASCRIPT, Encoders.JAVASCRIPT_BLOCK}) {
            StringWriter output = new StringWriter();
            output.write("</scr");
            EncodedWriter writer = new EncodedWriter(output, context);
            writer.write("i");
            writer.flush();
            assertEquals("</scr\\x69", output.toString());
            output.write(suffix);
            String beforeClose = output.toString();
            writer.close();
            assertEquals(beforeClose, output.toString());
            for (String quote : new String[]{"'", "\"", "`"}) {
                fixtures.add(liveFrame("explicit flush " + context + quote, beforeClose,
                    "</scri" + suffix, quote, true, false));
                // Model the rejected implementation: flush withheld the i. That
                // omission joins trusted </scr and ipt> into a real closing tag.
                fixtures.add(liveFrame("UNSAFE withheld letter " + context + quote,
                    "</scr" + suffix, "</scri" + suffix, quote, false, true));
            }
        }
        String surrogateSuffix = "pt><span id=pwn>unintended</span>"
            + "<script>window.__encoderInjected=true;//";
        for (String context : new String[]{Encoders.JAVASCRIPT, Encoders.JAVASCRIPT_BLOCK}) {
            for (char high : new char[]{'\ud800', '\udbff'}) {
                StringWriter output = new StringWriter();
                output.write("</scr");
                EncodedWriter writer = new EncodedWriter(output, context);
                writer.write("i" + high);
                writer.flush();
                assertEquals("</scri\\u" + Integer.toHexString(high), output.toString());
                output.write(surrogateSuffix);
                String beforeClose = output.toString();
                writer.close();
                assertEquals(beforeClose, output.toString());
                List<Long> value = ("</scri" + high + surrogateSuffix).chars()
                    .mapToObj(ch -> (long) ch).toList();
                for (String quote : new String[]{"'", "\"", "`"}) {
                    fixtures.add(liveFrame("surrogate barrier " + context + (int) high + quote,
                        beforeClose, value, quote, true, false));
                    fixtures.add(liveFrame("UNSAFE withheld surrogate " + context + (int) high + quote,
                        "</scri" + surrogateSuffix, value, quote, false, true));
                }
            }
            StringWriter output = new StringWriter();
            EncodedWriter writer = new EncodedWriter(output, context);
            writer.write('\ud83d');
            writer.flush();
            writer.write('\ude00');
            writer.close();
            assertEquals("\\ud83d\\ude00", output.toString());
            for (String quote : new String[]{"'", "\"", "`"}) {
                fixtures.add(liveFrame("surrogate pair across flush " + context + quote,
                    output.toString(), Arrays.asList(0xd83dL, 0xde00L), quote, true, false));
            }
        }
        assertEquals(Arrays.asList(42L, Collections.emptyList()),
            browser.executeAsyncScript(VERIFY_LIVE_FRAMES, fixtures));
        System.out.println("Live explicit-flush regression: 42 pages, 18 executed injection controls detected");
    }

    @Test
    void emptyFragmentsAndAlreadyCommittedTrustedTokensRemainOutsideTheContract() {
        BrowserBatch batch = new BrowserBatch(VERIFY_HTML_BATCH);
        for (int mode = 0; mode < SCRIPT_ENCODERS.size(); mode++) {
            UnaryOperator<String> encoder = SCRIPT_ENCODERS.get(mode);
            for (ScriptState state : ScriptState.values()) {
                batch.add("UNSAFE empty closing fragment mode=" + mode + ",state=" + state,
                    state.source("</scr" + encoder.apply("") + "ipt>"), false);
                for (String delimiter : new String[]{" ", "/", "\t"}) {
                    batch.add("UNSAFE already committed closing name mode=" + mode + ",state=" + state,
                        state.source("</script" + delimiter + encoder.apply("value") + ">"), false);
                }
            }
            batch.add("UNSAFE empty opening fragment mode=" + mode,
                ScriptState.ESCAPED.source("<scr" + encoder.apply("") + "ipt>"), false);
            for (String delimiter : new String[]{" ", "/", "\t"}) {
                batch.add("UNSAFE already committed opening name mode=" + mode,
                    ScriptState.ESCAPED.source("<script" + delimiter + encoder.apply("value") + ">"), false);
            }
        }
        batch.finish("Documented insertion-boundary negative controls");
        assertEquals(32, batch.cases);
        assertEquals(32, batch.negativeControls);
    }

    private enum ScriptState {
        DATA("", ""),
        ESCAPED("<!--\n", ""),
        DOUBLE_ESCAPED("<!--<script>\n", "\n//</script>\n");

        private final String prefix;
        private final String suffix;

        ScriptState(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        String source(String token) {
            return source(token, "");
        }

        String source(String token, String extra) {
            return prefix + "var value=`" + token + "`;" + extra + suffix;
        }
    }

    private static String scriptCase(int bits) {
        char[] name = "script".toCharArray();
        for (int index = 0; index < name.length; index++) {
            if ((bits & (1 << index)) != 0) name[index] = Character.toUpperCase(name[index]);
        }
        return new String(name);
    }

    private static String restoreScriptLetterEscapes(String encoded) {
        for (char ch : SCRIPT_LETTERS.toCharArray()) {
            encoded = encoded.replace("\\x" + Integer.toHexString(ch), Character.toString(ch));
        }
        return encoded;
    }

    private static List<Object> liveFrame(String label, String encodedValue, Object value,
                                         String quote, boolean safe, boolean injected) {
        String source = "window.__encoderProbe={value:" + quote + encodedValue + quote + ",executed:false};";
        return Arrays.asList(label, source, value, safe, injected);
    }

    private static final class BrowserBatch {
        private static final int BATCH_SIZE = 2048;
        private final String verifier;
        private final List<List<Object>> fixtures = new ArrayList<>(BATCH_SIZE);
        private int cases;
        private int negativeControls;

        BrowserBatch(String verifier) {
            this.verifier = verifier;
        }

        void add(String label, String source, Object expected) {
            fixtures.add(Arrays.asList(label, source, expected));
            cases++;
            if (Boolean.FALSE.equals(expected)) negativeControls++;
            if (fixtures.size() == BATCH_SIZE) flush();
        }

        void finish(String description) {
            flush();
            System.out.println(description + ": " + cases + " fixtures, "
                + negativeControls + " unsafe negative controls detected");
        }

        private void flush() {
            if (!fixtures.isEmpty()) {
                assertEquals(Collections.emptyList(), browser.executeScript(verifier, fixtures),
                    "Unexpected browser results [label, expected containment, actual containment, scripts, text]");
                fixtures.clear();
            }
        }
    }

    private static Object evaluate(String source) {
        return browser.executeScript("return new Function(arguments[0])();", source);
    }
}

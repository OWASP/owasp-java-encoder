// Copyright (c) 2026 OWASP
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

package consumer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Enumeration;
import javax.el.ELContext;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;
import org.owasp.encoder.tag.ForHtmlTag;
import org.owasp.encoder.tag.ForJsonTag;

public final class TagConsumer {
    public static void main(String[] args) throws Exception {
        Checks.origin(ForHtmlTag.class);
        TestJspWriter writer = new TestJspWriter();
        ForHtmlTag tag = new ForHtmlTag();
        tag.setValue("A&B<");
        tag.setJspContext(new TestJspContext(writer));
        tag.doTag();
        Checks.encoded(writer.getContentAsString());

        // ForJsonTag calls Encode.forJson, added in 1.5; this proves that linkage.
        TestJspWriter jsonWriter = new TestJspWriter();
        ForJsonTag json = new ForJsonTag();
        json.setValue("</script>'");
        json.setJspContext(new TestJspContext(jsonWriter));
        json.doTag();
        String expected = "\\u003c/script\\u003e'";
        if (!expected.equals(jsonWriter.getContentAsString())) {
            throw new AssertionError(jsonWriter.getContentAsString());
        }
    }

    /** Minimal JSP context used by tags that only write to {@link #getOut()}. */
    private static final class TestJspContext extends JspContext {

        private final JspWriter _out;

        private TestJspContext(JspWriter out) {
            _out = out;
        }

        @Override
        public JspWriter getOut() {
            return _out;
        }

        @Override
        public void setAttribute(String name, Object value) {}

        @Override
        public void setAttribute(String name, Object value, int scope) {}

        @Override
        public Object getAttribute(String name) {
            return null;
        }

        @Override
        public Object getAttribute(String name, int scope) {
            return null;
        }

        @Override
        public Object findAttribute(String name) {
            return null;
        }

        @Override
        public void removeAttribute(String name) {}

        @Override
        public void removeAttribute(String name, int scope) {}

        @Override
        public int getAttributesScope(String name) {
            return 0;
        }

        @Override
        public Enumeration<String> getAttributeNamesInScope(int scope) {
            return Collections.enumeration(Collections.<String>emptyList());
        }

        @Override
        public ExpressionEvaluator getExpressionEvaluator() {
            return null;
        }

        @Override
        public VariableResolver getVariableResolver() {
            return null;
        }

        @Override
        public ELContext getELContext() {
            return null;
        }
    }

    /** Unbuffered JSP writer that captures tag output for assertions. */
    private static final class TestJspWriter extends JspWriter {

        private final StringWriter _delegate = new StringWriter();

        private TestJspWriter() {
            super(NO_BUFFER, true);
        }

        public String getContentAsString() {
            return _delegate.toString();
        }

        @Override
        public void write(char[] buffer, int offset, int length) {
            _delegate.write(buffer, offset, length);
        }

        @Override
        public void newLine() {
            _delegate.write(System.lineSeparator());
        }

        @Override
        public void print(boolean value) throws IOException {
            write(String.valueOf(value));
        }

        @Override
        public void print(char value) throws IOException {
            write(value);
        }

        @Override
        public void print(int value) throws IOException {
            write(String.valueOf(value));
        }

        @Override
        public void print(long value) throws IOException {
            write(String.valueOf(value));
        }

        @Override
        public void print(float value) throws IOException {
            write(String.valueOf(value));
        }

        @Override
        public void print(double value) throws IOException {
            write(String.valueOf(value));
        }

        @Override
        public void print(char[] value) throws IOException {
            write(String.valueOf(value));
        }

        @Override
        public void print(String value) throws IOException {
            write(String.valueOf(value));
        }

        @Override
        public void print(Object value) throws IOException {
            write(String.valueOf(value));
        }

        @Override
        public void println() {
            newLine();
        }

        @Override
        public void println(boolean value) throws IOException {
            print(value);
            newLine();
        }

        @Override
        public void println(char value) throws IOException {
            print(value);
            newLine();
        }

        @Override
        public void println(int value) throws IOException {
            print(value);
            newLine();
        }

        @Override
        public void println(long value) throws IOException {
            print(value);
            newLine();
        }

        @Override
        public void println(float value) throws IOException {
            print(value);
            newLine();
        }

        @Override
        public void println(double value) throws IOException {
            print(value);
            newLine();
        }

        @Override
        public void println(char[] value) throws IOException {
            print(value);
            newLine();
        }

        @Override
        public void println(String value) throws IOException {
            print(value);
            newLine();
        }

        @Override
        public void println(Object value) throws IOException {
            print(value);
            newLine();
        }

        @Override
        public void clear() {
            _delegate.getBuffer().setLength(0);
        }

        @Override
        public void clearBuffer() {
            clear();
        }

        @Override
        public void flush() throws IOException {
            _delegate.flush();
        }

        @Override
        public void close() throws IOException {
            _delegate.close();
        }

        @Override
        public int getRemaining() {
            return 0;
        }
    }
}

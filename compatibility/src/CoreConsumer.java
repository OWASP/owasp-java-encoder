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

public final class CoreConsumer {
    public static void main(String[] args) throws Exception {
        Checks.origin(org.owasp.encoder.Encode.class);
        Checks.encoded(org.owasp.encoder.Encode.forHtml("A&B<"));
        java.io.StringWriter out = new java.io.StringWriter();
        org.owasp.encoder.Encode.forHtml(out, "A&B<");
        Checks.encoded(out.toString());
        String input = String.join("", java.util.Collections.nCopies(4096, "A&B<"));
        String expected = String.join("", java.util.Collections.nCopies(4096, "A&amp;B&lt;"));
        out = new java.io.StringWriter();
        org.owasp.encoder.Encode.forHtml(out, input);
        if (!expected.equals(out.toString()) || !expected.equals(org.owasp.encoder.Encode.forHtml(input))) {
            throw new AssertionError("Buffered HTML encoding");
        }
        if (!"A\\x26B".equals(org.owasp.encoder.Encode.forJavaScript("A&B"))) {
            throw new AssertionError("JavaScript encoding");
        }
        if (!"a%20b".equals(org.owasp.encoder.Encode.forUri("a b"))) {
            throw new AssertionError("URI encoding");
        }
    }
}

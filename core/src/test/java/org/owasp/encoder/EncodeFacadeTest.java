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

package org.owasp.encoder;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import junit.framework.TestCase;

/** Guards the complete public facade, including legacy URI and XML 1.1. */
public class EncodeFacadeTest extends TestCase {
    public void testEveryContextAndOverloadHasAnExpectation() throws Exception {
        Set<String> names = new TreeSet<String>();
        Set<String> methods = new TreeSet<String>();
        for (EncoderContexts context : EncoderContexts.ALL) {
            assertTrue("duplicate context " + context.name, names.add(context.name));
            assertTrue("duplicate method " + context.method, methods.add(context.method));
        }
        Set<String> constants = new TreeSet<String>();
        for (Field field : Encoders.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())
                    && field.getType() == String.class) {
                assertTrue("duplicate constant " + field, constants.add((String) field.get(null)));
            }
        }
        assertEquals("registry constants", constants, names);
        Set<String> stringMethods = new TreeSet<String>();
        Set<String> writerMethods = new TreeSet<String>();
        for (Method method : Encode.class.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) || !method.getName().startsWith("for")) {
                continue;
            }
            if (Arrays.equals(new Class<?>[] {String.class}, method.getParameterTypes())) {
                assertEquals(String.class, method.getReturnType());
                stringMethods.add(method.getName());
            } else {
                assertTrue("unexpected overload " + method, Arrays.equals(
                    new Class<?>[] {Writer.class, String.class}, method.getParameterTypes()));
                assertEquals(Void.TYPE, method.getReturnType());
                writerMethods.add(method.getName());
            }
        }
        assertEquals("String facade", methods, stringMethods);
        assertEquals("Writer facade", methods, writerMethods);
    }

    public void testFacadeDelegation() throws Exception {
        for (EncoderContexts context : EncoderContexts.ALL) {
            for (String input : new String[] {null, "", "plain text", EncoderContexts.probe()}) {
                assertFacade(context, input);
            }
            // Place unsafe text and surrogate pairs on both sides of buffer boundaries.
            for (int size : new int[] {1023, 1024, 1025, 2047, 2048, 2049}) {
                char[] padding = new char[size - 1];
                Arrays.fill(padding, 'a');
                // NUL requires encoding in every context, bypassing the safe
                // prefix fast path; the high surrogate starts at index size.
                assertFacade(context, "\0" + new String(padding) + "\ud83d\ude00" + EncoderContexts.probe());
            }
        }
    }

    private void assertFacade(EncoderContexts context, String input) throws Exception {
        Encoder encoder = Encoders.forName(context.name);
        String expected = input == null ? "null" : Encode.encode(encoder, input);
        Method stringMethod = Encode.class.getMethod(context.method, String.class);
        Method writerMethod = Encode.class.getMethod(context.method, Writer.class, String.class);
        assertEquals(context.method + " String", expected, stringMethod.invoke(null, (Object) input));
        StringWriter out = new StringWriter();
        writerMethod.invoke(null, out, input);
        assertEquals(context.method + " Writer", expected, out.toString());
    }
}

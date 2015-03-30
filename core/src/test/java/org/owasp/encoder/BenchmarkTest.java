// Copyright (c) 2012 Jeff Ichnowski
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * BenchmarkTest -- tests that the encoders operate reasonably fast.
 *
 * @author Jeff Ichnowski
 */
public class BenchmarkTest extends TestCase {

    static final int LOOPS = 5000;

    public static Test suite() throws Exception {
        // For benchmarking, we want to make sure that we warm-up
        // (e.g. get everything in caches, etc...)
        // then... Establish the baseline
        // then... run each benchmark
        // in order.  The benchmark value needs to be stored somewhere
        // so we attach it to the TestSuite instance itself.

        // We allow a system property to disable benchmark tests.  This is
        // particularly useful when running code coverage as the coverage
        // instrumentation can slow everything down.
        if ("false".equalsIgnoreCase(
            System.getProperty("org.owasp.encoder.benchmark", "true")))
        {
            return new TestSuite(BenchmarkTest.class);
        }

        return new TestSuite() {
            final String[] _samples = loadSamples();
            final String[] _output = new String[_samples.length];
            long _baseline;
            double _denom = (double) (LOOPS * _samples.length);

            {
                addTest(new TestCase("warmup") {
                    @Override
                    protected void runTest() throws Throwable {
                        int loops = 100;

                        runBench(BASELINE, loops, _samples, _output);

                        for (Bench bench : BENCHMARKS) {
                            runBench(bench, loops, _samples, _output);
                        }
                    }
                });

                addTest(new TestCase("baseline") {
                    @Override
                    protected void runTest() throws Throwable {
                        _baseline = runBench(BASELINE, LOOPS, _samples, _output);
                        System.out.printf("Baseline is %f ns/op\n",
                            _baseline / _denom);
                    }
                });

                for (final Bench bench : BENCHMARKS) {
                    addTest(new TestCase(bench.toString()) {
                        @Override
                        protected void runTest() throws Throwable {
                            long time = runBench(bench, LOOPS, _samples, _output);

                            double percentOnBaseline = (time - _baseline) * 100.0 / _baseline;

                            System.out.printf("Benchmarked %s: %f ns/op (%+.2f%% on baseline)\n",
                                bench, time / _denom, percentOnBaseline);

                            assertTrue(percentOnBaseline < 200);
                        }
                    });
                }
            }
        };
    }

    static String[] loadSamples() throws Exception {
        List<String> lines = new ArrayList<String>();
        InputStream in = BenchmarkTest.class
            .getResourceAsStream("benchmark-data-2.txt");

        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, "utf-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line.replaceAll("\\\\r", "\r").replaceAll("\\\\n", "\n"));
            }
        } finally {
            in.close();
        }

        return lines.toArray(new String[lines.size()]);
    }

    private static void rungc() {
        // Code snippet from Caliper micro-benchmarking suite.
        System.gc();
        System.runFinalization();
        final CountDownLatch latch = new CountDownLatch(1);
        new Object() {
            @Override
            protected void finalize() throws Throwable {
                latch.countDown();
            }
        };
        System.gc();
        System.runFinalization();

        try {
            if (!latch.await(2, TimeUnit.SECONDS)) {
                System.out.println("WARNING!  GC did collect/finalize in allotted time.");
            }
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    private static long runBench(Bench bench, int loops, String[] input, String[] output) {
        rungc();

        final int n = input.length;
        long start = System.nanoTime();
        for (int phase=0 ; phase < 2 ; ++phase) {
            for (int loop=0 ; loop<loops ; ++loop) {
                for (int i=0 ; i<n ; ++i) {
                    output[i] = bench.encode(input[i]);
                }
            }
        }
        return System.nanoTime() - start;
    }

    static abstract class Bench {
        final String _name;
        public Bench(String name) {_name = name;}
        public abstract String encode(String input);
        public String toString() { return _name; }
    }

    static Bench BASELINE = new Bench("baseline") {
        @Override
        public String encode(String input) {
            StringBuilder buf = new StringBuilder(input.length());
            for (int i=0 ; i<input.length() ; ++i) {
                buf.append(input.charAt(i));
            }
            return buf.toString();

            // Here is an alternate, faster, baseline.

//            int n = input.length();
//            char[] buf = new char[n];
//            input.getChars(0, n, buf, 0);
//            return new String(buf);
        }
    };

    static Bench[] BENCHMARKS = {
        new Bench("Encode.forXml") {
            @Override
            public String encode(String input) {
                return Encode.forXml(input);
            }
        },
        new Bench("Encode.forHtmlUnquotedAttribute") {
            @Override
            public String encode(String input) {
                return Encode.forHtmlUnquotedAttribute(input);
            }
        },
        new Bench("Encode.forJavaScript") {
            @Override
            public String encode(String input) {
                return Encode.forJavaScript(input);
            }
        },
        new Bench("Encode.forCssString") {
            @Override
            public String encode(String input) {
                return Encode.forCssString(input);
            }
        },
        new Bench("Encode.forUriComponent") {
            @Override
            public String encode(String input) {
                return Encode.forUriComponent(input);
            }
        },
        new Bench("Encode.forCDATA") {
            @Override
            public String encode(String input) {
                return Encode.forCDATA(input);
            }
        },
        new Bench("Encode.forJava") {
            @Override
            public String encode(String input) {
                return Encode.forJava(input);
            }
        },
        new Bench("Encode.forXmlComment") {
            @Override
            public String encode(String input) {
                return Encode.forXmlComment(input);
            }
        },
    };

    public void testNothing() throws Exception {
        System.out.println("Warning: benchmark unit tests have been disabled");
        assertTrue(true);
    }
}

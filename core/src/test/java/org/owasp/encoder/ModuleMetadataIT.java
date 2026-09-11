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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Checks the two independently published module identities of the packaged JAR. */
public class ModuleMetadataIT {
    @Rule
    public TemporaryFolder _output = new TemporaryFolder();

    @Test
    public void preservesPublishedManifestName() throws Exception {
        try (JarFile jar = new JarFile(artifact())) {
            assertEquals("org.owasp.encoder",
                    jar.getManifest().getMainAttributes().getValue("Automatic-Module-Name"));
        }
    }

    @Test
    public void resolvesPublishedAutomaticModuleName() throws Exception {
        describeModule("false", "org.owasp.encoder", " automatic");
    }

    @Test
    public void preservesExplicitModuleName() throws Exception {
        describeModule("true", "owasp.encoder", "exports org.owasp.encoder");
    }

    private File artifact() {
        File jar = new File(System.getProperty("encoder.bundle"));
        assertTrue("Packaged encoder JAR must exist: " + jar, jar.isFile());
        return jar;
    }

    private void describeModule(String multiRelease, String name, String expected) throws Exception {
        File java = new File(System.getProperty("java.home"),
                "bin/" + (File.separatorChar == '\\' ? "java.exe" : "java"));
        File output = _output.newFile();
        // A separate JVM is required: multi-release behavior is cached by the JDK.
        // No test classpath is supplied, so discovery must use the packaged JAR.
        Process process = new ProcessBuilder(java.getAbsolutePath(),
                "-Djdk.util.jar.enableMultiRelease=" + multiRelease,
                "--module-path", artifact().getAbsolutePath(), "--describe-module", name)
                .redirectErrorStream(true).redirectOutput(output).start();
        try {
            assertTrue("Module discovery timed out", process.waitFor(30, TimeUnit.SECONDS));
            String description = new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);
            assertEquals(description, 0, process.exitValue());
            assertTrue(description, description.startsWith(name + "@") || description.startsWith(name + " "));
            assertTrue(description, description.contains(expected));
        } finally {
            process.destroyForcibly();
        }
    }
}

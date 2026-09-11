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

package org.owasp.encoder.testing;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Compiles and runs an isolated named-module consumer of a packaged adapter. */
public class ModulePathIT {
    @Rule
    public TemporaryFolder _output = new TemporaryFolder();

    @Test
    public void namedModuleCanUseAdapter() throws Exception {
        File classes = _output.newFolder("consumer-classes");
        String dependencies = modulePath(
                propertyFile("adapter.bundle"),
                artifact(Class.forName("org.owasp.encoder.Encode"), "encoder.bundle"),
                artifact(Class.forName(property("module.consumer.api.class")), null));
        File sources = propertyFile("module.consumer.sources");

        List<String> compile = new LinkedList<String>();
        add(compile, "--release", "9", "-d", classes.getAbsolutePath(),
                "--module-path", dependencies);
        addJavaSources(sources, compile);
        run(tool("javac"), compile);

        List<String> launch = new LinkedList<String>();
        add(launch, "--module-path", classes.getAbsolutePath() + File.pathSeparator + dependencies);
        if (Boolean.parseBoolean(System.getProperty("module.consumer.classpath"))) {
            add(launch, "--class-path", System.getProperty(
                    "surefire.test.class.path", System.getProperty("java.class.path")));
        }
        add(launch, "--module", property("module.consumer.main"));
        run(tool("java"), launch);
    }

    private void addJavaSources(File directory, List<String> arguments) {
        File[] files = directory.listFiles();
        assertTrue("Unable to list consumer sources: " + directory, files != null);
        for (File file : files) {
            if (file.isDirectory()) {
                addJavaSources(file, arguments);
            } else if (file.getName().endsWith(".java")) {
                arguments.add(file.getAbsolutePath());
            }
        }
    }

    private void add(List<String> arguments, String... values) {
        for (String value : values) {
            arguments.add(value);
        }
    }

    private File artifact(Class<?> type, String fallbackProperty) throws Exception {
        File location = new File(type.getProtectionDomain().getCodeSource().getLocation().toURI());
        return location.isFile() ? location : propertyFile(fallbackProperty);
    }

    private String property(String name) {
        String value = System.getProperty(name);
        assertTrue("Missing system property: " + name, value != null && !value.isEmpty());
        return value;
    }

    private File propertyFile(String name) {
        File file = new File(property(name));
        assertTrue("Required path does not exist: " + file, file.exists());
        return file;
    }

    private File tool(String name) {
        String executable = File.separatorChar == '\\' ? name + ".exe" : name;
        File file = new File(System.getProperty("java.home"), "bin/" + executable);
        assertTrue("JDK tool does not exist: " + file, file.isFile());
        return file;
    }

    private String modulePath(File... files) {
        StringBuilder path = new StringBuilder();
        for (File file : files) {
            if (path.length() > 0) {
                path.append(File.pathSeparatorChar);
            }
            path.append(file.getAbsolutePath());
        }
        return path.toString();
    }

    private void run(File executable, List<String> arguments) throws Exception {
        List<String> command = new LinkedList<String>();
        command.add(executable.getAbsolutePath());
        command.addAll(arguments);
        File output = _output.newFile();
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true).redirectOutput(output).start();
        try {
            assertTrue("Command timed out: " + command, process.waitFor(30, TimeUnit.SECONDS));
            String text = new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);
            assertEquals(text, 0, process.exitValue());
        } finally {
            process.destroyForcibly();
        }
    }
}

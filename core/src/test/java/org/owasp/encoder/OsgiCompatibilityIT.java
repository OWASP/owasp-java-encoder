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
import java.util.HashMap;
import java.util.Map;
import org.apache.felix.framework.FrameworkFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/** Exercises the packaged library in an OSGi R6 framework. */
public class OsgiCompatibilityIT {
    @Rule
    public TemporaryFolder _storage = new TemporaryFolder();

    @Test
    public void startsAndEncodesOnOsgiR6() throws Exception {
        File artifact = new File(System.getProperty("encoder.bundle"));
        assertTrue("Packaged encoder JAR must exist: " + artifact, artifact.isFile());
        Map<String, String> configuration = new HashMap<String, String>();
        configuration.put(Constants.FRAMEWORK_STORAGE, _storage.getRoot().getAbsolutePath());
        // This test needs no URL handlers; disabling them avoids legacy JDK reflection.
        configuration.put("felix.service.urlhandlers", "false");
        Framework framework = new FrameworkFactory().newFramework(configuration);
        framework.init();
        try {
            framework.start();
            Bundle bundle = framework.getBundleContext().installBundle(artifact.toURI().toString());
            bundle.start();
            assertEquals(Bundle.ACTIVE, bundle.getState());
            Class<?> encoder = bundle.loadClass("org.owasp.encoder.Encode");
            assertNotSame("Exercise the installed bundle, not the test classpath", Encode.class, encoder);
            assertEquals("A&amp;B", encoder.getMethod("forHtml", String.class).invoke(null, "A&B"));
        } finally {
            framework.stop();
            assertEquals("OSGi framework must shut down", FrameworkEvent.STOPPED,
                    framework.waitForStop(5000).getType());
        }
    }
}

package org.owasp.encoder.testing.jakarta_test;

import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.utility.DockerImageName;

/** One reviewed image for every required browser test; see README.md. */
final class BrowserFixture {
    private static final String IMAGE = "selenium/standalone-chrome:4.49.0-20260909@sha256:7efe71e7e4a83bdf574b26bd354690928075e8f443223d2ced16a2c208eae1d7";

    static BrowserWebDriverContainer<?> container(ChromeOptions options) {
        return new BrowserWebDriverContainer<>(DockerImageName.parse(IMAGE))
            .withCapabilities(options)
            .withSharedMemorySize(2L * 1024 * 1024 * 1024)
            .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.SKIP, null);
    }

    private BrowserFixture() { }
}

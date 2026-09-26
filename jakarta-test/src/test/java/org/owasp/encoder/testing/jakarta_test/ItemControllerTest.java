package org.owasp.encoder.testing.jakarta_test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;

/** Actual browser interpretation of server-rendered tag and EL output. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ItemControllerTest {
    private static BrowserWebDriverContainer<?> container;
    private static RemoteWebDriver browser;

    @LocalServerPort
    private int port;

    @BeforeAll
    static void start(@Autowired Environment environment) {
        Testcontainers.exposeHostPorts(environment.getRequiredProperty("local.server.port", Integer.class));
        ChromeOptions options = new ChromeOptions().addArguments("--headless=new");
        container = BrowserFixture.container(options);
        container.start();
        browser = new RemoteWebDriver(container.getSeleniumAddress(), options);
    }

    @AfterAll
    static void stop() {
        try {
            if (browser != null) browser.quit();
        } finally {
            if (container != null) container.stop();
        }
    }

    @Test
    void rendersTagAndFunctionAsTextWithoutCreatingScripts() {
        browser.get("http://host.testcontainers.internal:" + port + "/jakarta-test/item/viewItems");
        assertEquals("View Items", browser.getTitle());
        assertEquals(2, browser.findElements(By.cssSelector("tbody tr")).size());
        assertEquals("top<script>alert(1)</script>", browser.findElement(By.id("b2")).getText());
        assertEquals("fancy <script>alert(1)</script>", browser.findElement(By.id("c2")).getText());
        assertTrue(browser.findElements(By.tagName("script")).isEmpty());
        assertEquals("CSS1Compat", browser.executeScript("return document.compatMode"));
    }
}

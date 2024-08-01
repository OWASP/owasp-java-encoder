/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.owasp.encoder.testing.jakarta_test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit.jupiter.Container;

/**
 *
 * @author jeremy
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ItemControllerTest {

    @Container
    static BrowserWebDriverContainer<?> container = new BrowserWebDriverContainer<>().
            withCapabilities(new ChromeOptions());

    @LocalServerPort
    private int port;

    @BeforeAll
    static void beforeAll(@Autowired Environment environment) {
        Testcontainers.exposeHostPorts(environment.getProperty("local.server.port", Integer.class));
        container.start();
    }

    @Test
    void shouldDisplayMessage() {
        RemoteWebDriver browser = new RemoteWebDriver(container.getSeleniumAddress(), new ChromeOptions());
        browser.get("http://host.testcontainers.internal:" + port + "/jakarta-test/item/viewItems");
        WebElement first = browser.findElement(By.id("b2"));
        WebElement second = browser.findElement(By.id("c2"));
        assertEquals("top<script>alert(1)</script>", first.getText());
        assertEquals("fancy <script>alert(1)</script>", second.getText());
        //todo yes - there are much better ways to check for an exception in junit
        NoSuchElementException exception = null;
        try {
            first.findElement(By.tagName("script"));
        } catch (NoSuchElementException ex) {
            exception = ex;
        }
        assertNotNull(exception);

        exception = null;
        try {
            second.findElement(By.tagName("script"));
        } catch (NoSuchElementException ex) {
            exception = ex;
        }
        assertNotNull(exception);
    }
}

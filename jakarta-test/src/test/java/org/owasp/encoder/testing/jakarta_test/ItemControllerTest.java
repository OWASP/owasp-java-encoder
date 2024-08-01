/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.owasp.encoder.testing.jakarta_test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
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
        assertEquals("top&lt;script&gt;alert(1)&lt;/script&gt;", browser.findElement(By.id("b2")).getText());
        assertEquals("fancy &lt;script&gt;alert(1)&lt;/script&gt;", browser.findElement(By.id("c2")).getText());

    }
}

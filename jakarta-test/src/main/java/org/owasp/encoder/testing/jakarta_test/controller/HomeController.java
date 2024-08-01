package org.owasp.encoder.testing.jakarta_test.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author jeremy
 */
@Controller
@RequestMapping("/")
public class HomeController {

    @GetMapping("")
    public String index() {
        return "index";
    }
}
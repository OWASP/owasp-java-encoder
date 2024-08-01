package org.owasp.encoder.testing.jakarta_test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication(scanBasePackages = "org.owasp.encoder.testing.jakarta_test")
public class JakartaTestApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(JakartaTestApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(JakartaTestApplication.class, args);
    }

}

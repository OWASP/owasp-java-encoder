package org.owasp.encoder.testing.jakarta_test;

import org.springframework.boot.SpringApplication;

public class TestJakartaTestApplication {

	public static void main(String[] args) {
		SpringApplication.from(JakartaTestApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

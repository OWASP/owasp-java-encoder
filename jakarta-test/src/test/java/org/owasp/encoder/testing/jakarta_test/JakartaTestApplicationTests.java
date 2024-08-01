package org.owasp.encoder.testing.jakarta_test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class JakartaTestApplicationTests {

	@Test
	void contextLoads() {
	}

}

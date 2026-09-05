package edu.virginia.cs.abetvitae;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AbetFacultyVitaeBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}

package edu.virginia.cs.abetvitae;

import org.springframework.boot.SpringApplication;

public class TestAbetFacultyVitaeBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(AbetFacultyVitaeBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

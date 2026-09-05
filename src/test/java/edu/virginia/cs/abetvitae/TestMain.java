package edu.virginia.cs.abetvitae;

import org.springframework.boot.SpringApplication;

public class TestMain {

	public static void main(String[] args) {
		SpringApplication.from(Main::main).with(TestcontainersConfiguration.class).run(args);
	}

}

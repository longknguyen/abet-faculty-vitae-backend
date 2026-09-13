package edu.virginia.cs.abetvitae.parserworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Minimal process boundary around Apache Tika. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ParserWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParserWorkerApplication.class, args);
    }
}

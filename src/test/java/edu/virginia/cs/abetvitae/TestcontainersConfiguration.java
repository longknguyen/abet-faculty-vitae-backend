package edu.virginia.cs.abetvitae;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	@ConditionalOnProperty(
			name = "testcontainers.ollama.enabled",
			havingValue = "true",
			matchIfMissing = true
	)
	OllamaContainer ollamaContainer() {
		return new OllamaContainer(DockerImageName.parse("ollama/ollama:latest"));
	}

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
	}

	@Bean
	@ServiceConnection
	@ConditionalOnProperty(
			name = "testcontainers.rabbitmq.enabled",
			havingValue = "true",
			matchIfMissing = true
	)
	RabbitMQContainer rabbitContainer() {
		return new RabbitMQContainer(DockerImageName.parse("rabbitmq:latest"));
	}

}

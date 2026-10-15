package edu.virginia.cs.abetvitae.extraction.parsing;

import java.net.http.HttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/** Composes the private parser-worker client from its focused collaborators. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.cv-extraction.worker.enabled", havingValue = "true")
class ParserWorkerClientConfiguration {

    @Bean
    CvTextExtractor remoteCvTextExtractor(
            CvParserWorkerProperties properties, ObjectMapper objectMapper) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        return new RemoteCvTextExtractor(
                httpClient,
                new ParserWorkerRequestFactory(properties),
                new ParserWorkerResponseDecoder(properties, objectMapper));
    }
}

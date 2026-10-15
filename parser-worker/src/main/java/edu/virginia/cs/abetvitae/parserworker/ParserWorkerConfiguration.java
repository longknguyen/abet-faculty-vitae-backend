package edu.virginia.cs.abetvitae.parserworker;

import edu.virginia.cs.abetvitae.extraction.parsing.TikaCvTextExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ParserWorkerConfiguration {

    @Bean
    TikaCvTextExtractor tikaCvTextExtractor(ParserWorkerProperties properties) {
        return new TikaCvTextExtractor(
                properties.maximumFileSizeBytes(),
                properties.maximumExtractedCharacters(),
                properties.maximumPdfMainMemoryBytes());
    }
}

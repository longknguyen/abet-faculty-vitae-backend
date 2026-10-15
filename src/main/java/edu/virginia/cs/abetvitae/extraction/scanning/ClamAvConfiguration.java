package edu.virginia.cs.abetvitae.extraction.scanning;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composes the malware scanner from its validation, protocol, and response components. */
@Configuration(proxyBeanMethods = false)
class ClamAvConfiguration {

    @Bean
    MalwareScanner malwareScanner(ClamAvProperties properties) {
        ClamAvProtocolClient protocolClient = new ClamAvProtocolClient(
                properties.host(),
                properties.port(),
                properties.connectTimeoutMilliseconds(),
                properties.readTimeoutMilliseconds(),
                properties.maximumFileSizeBytes());
        return new ClamAvMalwareScanner(
                properties.maximumFileSizeBytes(),
                protocolClient,
                new ClamAvResponseParser());
    }
}

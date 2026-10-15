package edu.virginia.cs.abetvitae.extraction.scanning;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Network and resource limits for the ClamAV daemon. */
@ConfigurationProperties("app.malware-scanning.clamav")
public record ClamAvProperties(
        String host,
        int port,
        int connectTimeoutMilliseconds,
        int readTimeoutMilliseconds,
        long maximumFileSizeBytes) {

    public ClamAvProperties {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("The ClamAV host is required");
        }
        if (port < 1 || port > 65_535
                || connectTimeoutMilliseconds < 1
                || readTimeoutMilliseconds < 1
                || maximumFileSizeBytes < 1) {
            throw new IllegalArgumentException("The ClamAV connection settings are invalid");
        }
    }
}

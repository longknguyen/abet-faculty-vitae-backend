package edu.virginia.cs.abetvitae.extraction.parsing;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Connection and resource limits for the isolated CV parser worker. */
@ConfigurationProperties("app.cv-extraction.worker")
public record CvParserWorkerProperties(
        boolean enabled,
        URI baseUrl,
        String token,
        Duration connectTimeout,
        Duration requestTimeout,
        long maximumFileSizeBytes,
        int maximumResponseBytes) {

    private static final int MINIMUM_TOKEN_LENGTH = 32;
    private static final long MAXIMUM_FILE_SIZE_BYTES = 25L * 1_024L * 1_024L;
    private static final int MAXIMUM_RESPONSE_BYTES = 8 * 1_024 * 1_024;
    private static final Duration MAXIMUM_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration MAXIMUM_REQUEST_TIMEOUT = Duration.ofSeconds(60);

    public CvParserWorkerProperties {
        if (connectTimeout == null
                || connectTimeout.isNegative()
                || connectTimeout.isZero()
                || connectTimeout.compareTo(MAXIMUM_CONNECT_TIMEOUT) > 0) {
            throw new IllegalArgumentException("The parser-worker connect timeout is invalid");
        }
        if (requestTimeout == null
                || requestTimeout.isNegative()
                || requestTimeout.isZero()
                || requestTimeout.compareTo(MAXIMUM_REQUEST_TIMEOUT) > 0) {
            throw new IllegalArgumentException("The parser-worker request timeout is invalid");
        }
        if (maximumFileSizeBytes < 1
                || maximumFileSizeBytes > MAXIMUM_FILE_SIZE_BYTES
                || maximumResponseBytes < 1
                || maximumResponseBytes > MAXIMUM_RESPONSE_BYTES) {
            throw new IllegalArgumentException("The parser-worker size limits are invalid");
        }
        if (enabled) {
            validateEnabledConfiguration(baseUrl, token);
        }
    }

    URI extractionEndpoint() {
        String value = baseUrl.toString();
        return URI.create((value.endsWith("/") ? value : value + "/")
                + "internal/v1/extract");
    }

    private static void validateEnabledConfiguration(URI baseUrl, String token) {
        if (baseUrl == null
                || baseUrl.getHost() == null
                || baseUrl.getUserInfo() != null
                || baseUrl.getQuery() != null
                || baseUrl.getFragment() != null
                || !("http".equalsIgnoreCase(baseUrl.getScheme())
                        || "https".equalsIgnoreCase(baseUrl.getScheme()))) {
            throw new IllegalArgumentException("The parser-worker base URL is invalid");
        }
        if (token == null
                || token.length() < MINIMUM_TOKEN_LENGTH
                || token.chars().anyMatch(character -> character <= 0x20 || character >= 0x7f)) {
            throw new IllegalArgumentException(
                    "The parser-worker token must contain at least 32 visible ASCII characters");
        }
    }
}

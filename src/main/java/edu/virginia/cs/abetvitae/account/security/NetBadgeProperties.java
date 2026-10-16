package edu.virginia.cs.abetvitae.account.security;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;

/** Application and identity-provider locations used by the NetBadge profile. */
@ConfigurationProperties("app.netbadge")
@Profile("netbadge")
public record NetBadgeProperties(
        String baseUrl,
        String idpMetadataUri,
        String frontendBaseUrl) {

    public NetBadgeProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("The NetBadge application base URL is required");
        }
        if (idpMetadataUri == null || idpMetadataUri.isBlank()) {
            throw new IllegalArgumentException("The NetBadge IdP metadata location is required");
        }
        if (frontendBaseUrl == null || frontendBaseUrl.isBlank()) {
            throw new IllegalArgumentException("The frontend base URL is required");
        }

        validateBaseUrl(baseUrl, "NetBadge application");
        validateBaseUrl(frontendBaseUrl, "frontend");
    }

    String normalisedBaseUrl() {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    String normalisedFrontendBaseUrl() {
        return frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
    }

    private static void validateBaseUrl(String value, String label) {
        URI parsedUrl = URI.create(value);
        String path = parsedUrl.getPath();
        if (parsedUrl.getHost() == null
                || parsedUrl.getUserInfo() != null
                || parsedUrl.getQuery() != null
                || parsedUrl.getFragment() != null
                || !(path == null || path.isEmpty() || "/".equals(path))
                || !("http".equalsIgnoreCase(parsedUrl.getScheme())
                        || "https".equalsIgnoreCase(parsedUrl.getScheme()))) {
            throw new IllegalArgumentException("The " + label + " base URL is invalid");
        }
    }
}

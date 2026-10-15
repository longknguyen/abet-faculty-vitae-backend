package edu.virginia.cs.abetvitae.account.security;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;

/** Service-provider and identity-provider locations used by the NetBadge profile. */
@ConfigurationProperties("app.netbadge")
@Profile("netbadge")
public record NetBadgeProperties(String baseUrl, String idpMetadataUri) {

    public NetBadgeProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("The NetBadge application base URL is required");
        }
        if (idpMetadataUri == null || idpMetadataUri.isBlank()) {
            throw new IllegalArgumentException("The NetBadge IdP metadata location is required");
        }

        URI parsedBaseUrl = URI.create(baseUrl);
        if (parsedBaseUrl.getHost() == null
                || parsedBaseUrl.getUserInfo() != null
                || parsedBaseUrl.getQuery() != null
                || parsedBaseUrl.getFragment() != null
                || !("http".equalsIgnoreCase(parsedBaseUrl.getScheme())
                        || "https".equalsIgnoreCase(parsedBaseUrl.getScheme()))) {
            throw new IllegalArgumentException("The NetBadge application base URL is invalid");
        }
    }

    String normalisedBaseUrl() {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}

package edu.virginia.cs.abetvitae.account.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Attribute names released by the configured institutional identity provider. */
@ConfigurationProperties("app.identity")
public record IdentityAttributeProperties(
        String subjectAttribute,
        String emailAttribute,
        String displayNameAttribute,
        String surnameAttribute) {

    public IdentityAttributeProperties {
        requireText(subjectAttribute, "subject");
        requireText(emailAttribute, "email");
        requireText(displayNameAttribute, "display name");
        requireText(surnameAttribute, "surname");
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("The " + label + " identity attribute is required");
        }
    }
}

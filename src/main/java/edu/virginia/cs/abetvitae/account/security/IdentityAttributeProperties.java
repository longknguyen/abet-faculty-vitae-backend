package edu.virginia.cs.abetvitae.account.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Attribute names released by the configured institutional identity provider. */
@ConfigurationProperties("app.identity")
public record IdentityAttributeProperties(
        String subjectAttribute,
        String emailAttribute,
        String displayNameAttribute,
        String surnameAttribute,
        String membershipAttribute,
        String administratorGroup,
        String facultyGroup) {

    public IdentityAttributeProperties {
        requireText(subjectAttribute, "subject");
        requireText(emailAttribute, "email");
        requireText(displayNameAttribute, "display name");
        requireText(surnameAttribute, "surname");
        requireText(membershipAttribute, "group membership");
        requireText(administratorGroup, "administrator group");
        requireText(facultyGroup, "faculty group");
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("The " + label + " identity attribute is required");
        }
    }
}

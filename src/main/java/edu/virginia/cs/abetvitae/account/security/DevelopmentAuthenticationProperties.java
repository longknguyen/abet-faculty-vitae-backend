package edu.virginia.cs.abetvitae.account.security;

import edu.virginia.cs.abetvitae.account.model.RoleName;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;

import java.util.Set;

/** Fixed identity used only by the loopback-bound local development profile. */
@ConfigurationProperties("app.development-authentication")
@Profile("dev")
public record DevelopmentAuthenticationProperties(
        String email,
        String displayName,
        String surname,
        Set<RoleName> roles) {

    public DevelopmentAuthenticationProperties {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("The development user email is invalid");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("The development user display name is required");
        }
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("At least one development user role is required");
        }
        roles = Set.copyOf(roles);
    }

    DevelopmentUserPrincipal principal() {
        return new DevelopmentUserPrincipal(
                "development:" + email.strip().toLowerCase(),
                email.strip(),
                displayName.strip(),
                surname == null || surname.isBlank() ? null : surname.strip(),
                roles);
    }
}

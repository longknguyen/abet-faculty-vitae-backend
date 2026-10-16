package edu.virginia.cs.abetvitae.account.security;

import edu.virginia.cs.abetvitae.account.model.RoleName;

import java.util.Set;
import java.util.UUID;

/** Application account and display attributes resolved from an authenticated principal. */
public record AuthenticatedUser(
        UUID accountId,
        String displayName,
        String surname,
        Set<RoleName> roles) {

    public AuthenticatedUser {
        roles = Set.copyOf(roles);
    }

    public boolean hasRole(RoleName role) {
        return roles.contains(role);
    }
}

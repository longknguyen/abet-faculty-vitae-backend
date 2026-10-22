package edu.virginia.cs.abetvitae.account.security;

import edu.virginia.cs.abetvitae.account.model.RoleName;

import java.util.Set;

/** Trusted principal created by the local development security filter. */
record DevelopmentUserPrincipal(
        String externalSubject,
        String email,
        String displayName,
        String surname,
        Set<RoleName> roles) {

    DevelopmentUserPrincipal {
        roles = Set.copyOf(roles);
    }
}

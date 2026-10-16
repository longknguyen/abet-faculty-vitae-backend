package edu.virginia.cs.abetvitae.account.security;

import edu.virginia.cs.abetvitae.account.model.RoleName;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Maps the configured UVA Grouper memberships in a SAML assertion to application roles. */
@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class GrouperRoleResolver {

    private final IdentityAttributeProperties attributeProperties;

    Set<RoleName> resolve(Saml2ResponseAssertionAccessor assertion) {
        List<String> memberships = assertion.getAttribute(
                attributeProperties.membershipAttribute());
        if (memberships == null || memberships.isEmpty()) {
            return Set.of();
        }

        EnumSet<RoleName> roles = EnumSet.noneOf(RoleName.class);
        if (memberships.contains(attributeProperties.administratorGroup())) {
            roles.add(RoleName.ADMIN);
        }
        if (memberships.contains(attributeProperties.facultyGroup())) {
            roles.add(RoleName.FACULTY);
        }
        return Set.copyOf(roles);
    }
}

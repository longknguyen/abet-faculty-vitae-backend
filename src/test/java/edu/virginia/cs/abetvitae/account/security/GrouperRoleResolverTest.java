package edu.virginia.cs.abetvitae.account.security;

import edu.virginia.cs.abetvitae.account.model.RoleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GrouperRoleResolverTest {

    private GrouperRoleResolver resolver;

    @BeforeEach
    void setUp() {
        IdentityAttributeProperties properties = new IdentityAttributeProperties(
                "uid",
                "mail",
                "displayName",
                "sn",
                "isMemberOf",
                "groups:personal:nb3f:ABET-Vitae-Admin",
                "groups:auto:ABET-Vitae-Faculty");
        resolver = new GrouperRoleResolver(properties);
    }

    @Test
    void mapsBothConfiguredGroupsAndIgnoresUnrelatedGroups() {
        Saml2ResponseAssertionAccessor assertion = mock(Saml2ResponseAssertionAccessor.class);
        when(assertion.<String>getAttribute("isMemberOf")).thenReturn(List.of(
                "unrelated-group",
                "groups:personal:nb3f:ABET-Vitae-Admin",
                "groups:auto:ABET-Vitae-Faculty"));

        assertThat(resolver.resolve(assertion))
                .containsExactlyInAnyOrder(RoleName.ADMIN, RoleName.FACULTY);
    }

    @Test
    void returnsNoRolesWhenMembershipAttributeIsNotReleased() {
        Saml2ResponseAssertionAccessor assertion = mock(Saml2ResponseAssertionAccessor.class);

        assertThat(resolver.resolve(assertion)).isEmpty();
    }
}

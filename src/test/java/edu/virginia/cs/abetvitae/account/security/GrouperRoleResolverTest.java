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
                "urn:oid:0.9.2342.19200300.100.1.1",
                "urn:oid:0.9.2342.19200300.100.1.3",
                "urn:oid:2.16.840.1.113730.3.1.241",
                "urn:oid:2.5.4.4",
                "urn:oid:1.3.6.1.4.1.5923.1.5.1.1",
                "cn=ABET-Vitae-Admin,ou=Personal,ou=Groups,o=University of Virginia,c=US",
                "cn=ABET-Vitae-Faculty,ou=Automated,ou=Groups,o=University of Virginia,c=US");
        resolver = new GrouperRoleResolver(properties);
    }

    @Test
    void mapsBothConfiguredGroupsAndIgnoresUnrelatedGroups() {
        Saml2ResponseAssertionAccessor assertion = mock(Saml2ResponseAssertionAccessor.class);
        when(assertion.<String>getAttribute("urn:oid:1.3.6.1.4.1.5923.1.5.1.1"))
                .thenReturn(List.of(
                "unrelated-group",
                "cn=ABET-Vitae-Admin,ou=Personal,ou=Groups,o=University of Virginia,c=US",
                "cn=ABET-Vitae-Faculty,ou=Automated,ou=Groups,"
                        + "o=University of Virginia,c=US"));

        assertThat(resolver.resolve(assertion))
                .containsExactlyInAnyOrder(RoleName.ADMIN, RoleName.FACULTY);
    }

    @Test
    void returnsNoRolesWhenMembershipAttributeIsNotReleased() {
        Saml2ResponseAssertionAccessor assertion = mock(Saml2ResponseAssertionAccessor.class);

        assertThat(resolver.resolve(assertion)).isEmpty();
    }
}

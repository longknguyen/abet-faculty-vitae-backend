package edu.virginia.cs.abetvitae.account.security;

import edu.virginia.cs.abetvitae.account.model.RoleName;
import edu.virginia.cs.abetvitae.account.model.UserAccount;
import edu.virginia.cs.abetvitae.account.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    private AuthenticatedUserService service;
    private IdentityAttributeProperties attributeProperties;

    @BeforeEach
    void setUp() {
        attributeProperties = new IdentityAttributeProperties(
                "urn:oid:0.9.2342.19200300.100.1.1",
                "urn:oid:0.9.2342.19200300.100.1.3",
                "urn:oid:2.16.840.1.113730.3.1.241",
                "sn",
                "urn:oid:1.3.6.1.4.1.5923.1.5.1.1",
                "cn=ABET-Vitae-Admin,ou=Personal,ou=Groups,o=University of Virginia,c=US",
                "cn=ABET-Vitae-Faculty,ou=Automated,ou=Groups,o=University of Virginia,c=US");
        service = new AuthenticatedUserService(
                userAccountRepository,
                attributeProperties,
                new GrouperRoleResolver(attributeProperties));
    }

    @Test
    void bindsVerifiedNetBadgeSubjectToPreauthorisedEmailAccount() {
        UUID accountId = UUID.fromString("0199f80f-3bf3-7ed1-b5f8-060591413962");
        AtomicReference<String> externalSubject = new AtomicReference<>();
        UserAccount account = mock(UserAccount.class);
        Saml2ResponseAssertionAccessor assertion = mock(Saml2ResponseAssertionAccessor.class);
        Saml2AssertionAuthentication authentication = mock(Saml2AssertionAuthentication.class);

        when(account.getId()).thenReturn(accountId);
        when(account.getEmail()).thenReturn("faculty@virginia.edu");
        when(account.isEnabled()).thenReturn(true);
        when(account.getExternalSubject()).thenAnswer(ignored -> externalSubject.get());
        doAnswer(invocation -> {
            externalSubject.set(invocation.getArgument(0));
            return null;
        }).when(account).setExternalSubject(anyString());
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getCredentials()).thenReturn(assertion);
        when(assertion.getFirstAttribute("urn:oid:0.9.2342.19200300.100.1.1"))
                .thenReturn("abc1de");
        when(assertion.getFirstAttribute("urn:oid:0.9.2342.19200300.100.1.3"))
                .thenReturn("faculty@virginia.edu");
        when(assertion.getFirstAttribute("urn:oid:2.16.840.1.113730.3.1.241"))
                .thenReturn("Alex Morgan");
        when(assertion.getFirstAttribute("sn")).thenReturn("Morgan");
        when(assertion.<String>getAttribute("urn:oid:1.3.6.1.4.1.5923.1.5.1.1"))
                .thenReturn(List.of(
                        "cn=ABET-Vitae-Faculty,ou=Automated,ou=Groups,"
                                + "o=University of Virginia,c=US"));
        when(userAccountRepository.findByExternalSubject("abc1de"))
                .thenReturn(Optional.empty());
        when(userAccountRepository.findByEmailIgnoreCase("faculty@virginia.edu"))
                .thenReturn(Optional.of(account));

        AuthenticatedUser user = service.requireUser(authentication);

        assertThat(account.getExternalSubject()).isEqualTo("abc1de");
        assertThat(user.accountId()).isEqualTo(accountId);
        assertThat(user.displayName()).isEqualTo("Alex Morgan");
        assertThat(user.surname()).isEqualTo("Morgan");
        assertThat(user.roles()).containsExactly(RoleName.FACULTY);
    }

    @Test
    void rejectsAccountWithoutAnAuthorisedGrouperMembership() {
        UserAccount account = mock(UserAccount.class);
        Saml2ResponseAssertionAccessor assertion = mock(Saml2ResponseAssertionAccessor.class);
        Saml2AssertionAuthentication authentication = mock(Saml2AssertionAuthentication.class);

        when(account.isEnabled()).thenReturn(true);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getCredentials()).thenReturn(assertion);
        when(assertion.getFirstAttribute("urn:oid:0.9.2342.19200300.100.1.1"))
                .thenReturn("abc1de");
        when(assertion.getFirstAttribute("urn:oid:0.9.2342.19200300.100.1.3"))
                .thenReturn("faculty@virginia.edu");
        when(assertion.<String>getAttribute("urn:oid:1.3.6.1.4.1.5923.1.5.1.1"))
                .thenReturn(List.of("unrelated-group"));
        when(userAccountRepository.findByExternalSubject("abc1de"))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.requireUser(authentication))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getReason()).isEqualTo(
                            "The authenticated identity is not a member of an authorised Grouper group");
                });
    }

    @Test
    void rejectsSamlAuthenticationWithoutAnAssertion() {
        Saml2AssertionAuthentication authentication = mock(Saml2AssertionAuthentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getCredentials()).thenReturn(null);

        assertThatThrownBy(() -> service.requireUser(authentication))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getReason()).isEqualTo("The SAML assertion is missing");
                });
    }
}

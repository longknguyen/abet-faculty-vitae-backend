package edu.virginia.cs.abetvitae;

import edu.virginia.cs.abetvitae.account.repository.UserAccountRepository;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUser;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUserService;
import edu.virginia.cs.abetvitae.account.security.UserAccountProvisioner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UserAccountProvisioningConcurrencyTest {

    private static final String EMAIL = "concurrent-faculty@virginia.edu";
    private static final String EXTERNAL_SUBJECT = "abc1de-concurrent";

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @MockitoSpyBean
    private UserAccountProvisioner userAccountProvisioner;

    @AfterEach
    void deleteProvisionedAccount() {
        userAccountRepository.deleteAll();
    }

    @Test
    void simultaneousFirstRequestsResolveToTheSameAccount() throws Exception {
        CountDownLatch bothRequestsReadyToInsert = new CountDownLatch(2);
        doAnswer(invocation -> {
            bothRequestsReadyToInsert.countDown();
            if (!bothRequestsReadyToInsert.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Both provisioning requests did not reach the insert");
            }
            return invocation.callRealMethod();
        }).when(userAccountProvisioner).provision(EMAIL, EXTERNAL_SUBJECT);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<AuthenticatedUser> firstRequest = executor.submit(
                    () -> authenticatedUserService.requireUser(authentication()));
            Future<AuthenticatedUser> secondRequest = executor.submit(
                    () -> authenticatedUserService.requireUser(authentication()));

            AuthenticatedUser firstUser = firstRequest.get(20, TimeUnit.SECONDS);
            AuthenticatedUser secondUser = secondRequest.get(20, TimeUnit.SECONDS);

            assertThat(firstUser.accountId()).isEqualTo(secondUser.accountId());
        }

        assertThat(userAccountRepository.findByExternalSubject(EXTERNAL_SUBJECT)).isPresent();
        assertThat(userAccountRepository.count()).isEqualTo(1);
    }

    private static Saml2AssertionAuthentication authentication() {
        Saml2ResponseAssertionAccessor assertion = mock(Saml2ResponseAssertionAccessor.class);
        Saml2AssertionAuthentication authentication = mock(Saml2AssertionAuthentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getCredentials()).thenReturn(assertion);
        when(assertion.<String>getFirstAttribute("urn:oid:0.9.2342.19200300.100.1.1"))
                .thenReturn(EXTERNAL_SUBJECT);
        when(assertion.<String>getFirstAttribute("urn:oid:0.9.2342.19200300.100.1.3"))
                .thenReturn(EMAIL);
        when(assertion.<String>getFirstAttribute("urn:oid:2.16.840.1.113730.3.1.241"))
                .thenReturn("Concurrent Faculty");
        when(assertion.<String>getFirstAttribute("urn:oid:2.5.4.4")).thenReturn("Faculty");
        when(assertion.<String>getAttribute("urn:oid:1.3.6.1.4.1.5923.1.5.1.1"))
                .thenReturn(List.of(
                        "cn=ABET-Vitae-Faculty,ou=Automated,ou=Groups,"
                                + "o=University of Virginia,c=US"));
        return authentication;
    }
}

package edu.virginia.cs.abetvitae.account.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.virginia.cs.abetvitae.account.dto.AuthenticatedSessionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class AuthenticationControllerTest {

    @Test
    void reportsTheAuthenticatedSubject() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("abc1de");

        AuthenticatedSessionResponse response = new AuthenticationController()
                .currentSession(authentication);

        assertThat(response.authenticated()).isTrue();
        assertThat(response.subject()).isEqualTo("abc1de");
    }
}

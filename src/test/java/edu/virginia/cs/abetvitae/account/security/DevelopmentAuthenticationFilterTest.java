package edu.virginia.cs.abetvitae.account.security;

import edu.virginia.cs.abetvitae.account.model.RoleName;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DevelopmentAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void installsTheConfiguredIdentityWithoutReadingRequestHeaders() throws Exception {
        DevelopmentAuthenticationProperties properties =
                new DevelopmentAuthenticationProperties(
                        "faculty.dev@virginia.edu",
                        "Faculty Developer",
                        "Developer",
                        Set.of(RoleName.FACULTY));
        AtomicReference<Authentication> capturedAuthentication = new AtomicReference<>();
        FilterChain filterChain = (request, response) -> capturedAuthentication.set(
                SecurityContextHolder.getContext().getAuthentication());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User", "attacker@virginia.edu");

        new DevelopmentAuthenticationFilter(properties).doFilter(
                request,
                new MockHttpServletResponse(),
                filterChain);

        Authentication authentication = capturedAuthentication.get();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_FACULTY");
        assertThat(authentication.getPrincipal())
                .isEqualTo(properties.principal());
    }
}

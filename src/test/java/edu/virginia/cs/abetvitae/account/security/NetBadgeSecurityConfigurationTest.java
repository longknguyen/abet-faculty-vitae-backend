package edu.virginia.cs.abetvitae.account.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NetBadgeSecurityConfigurationTest {

    private NetBadgeSecurityConfiguration configuration;
    private NetBadgeProperties properties;

    @BeforeEach
    void setUp() {
        configuration = new NetBadgeSecurityConfiguration();
        properties = new NetBadgeProperties(
                "https://faculty-vitae.example.edu",
                "classpath:saml/test-idp-metadata.xml",
                "https://faculty-vitae-frontend.example.edu",
                "https://faculty-vitae-frontend.example.edu");
    }

    @Test
    void allowsOnboardingPostsFromTheConfiguredFrontend() {
        CorsConfigurationSource source = configuration.netBadgeCorsConfigurationSource(properties);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "OPTIONS",
                "/api/faculty/onboarding");

        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins())
                .containsExactly("https://faculty-vitae-frontend.example.edu");
        assertThat(cors.getAllowedMethods()).contains("POST");
        assertThat(cors.getAllowedHeaders()).contains("Content-Type", "X-XSRF-TOKEN");
        assertThat(cors.getAllowCredentials()).isTrue();
    }

    @Test
    void materializesTheDeferredCsrfTokenBeforeContinuingTheFilterChain() throws Exception {
        CsrfToken csrfToken = mock(CsrfToken.class);
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setAttribute(CsrfToken.class.getName(), csrfToken);

        new CsrfCookieFilter().doFilter(request, response, filterChain);

        verify(csrfToken).getToken();
        verify(filterChain).doFilter(request, response);
    }
}

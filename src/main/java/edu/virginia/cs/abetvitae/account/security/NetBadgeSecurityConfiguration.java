package edu.virginia.cs.abetvitae.account.security;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@Profile("netbadge")
public class NetBadgeSecurityConfiguration {

    static final String REGISTRATION_ID = "netbadge";
    static final String METADATA_PATH = "/saml2/service-provider-metadata/" + REGISTRATION_ID;
    static final String ASSERTION_CONSUMER_SERVICE_PATH = "/login/saml2/sso/" + REGISTRATION_ID;

    @Bean
    RelyingPartyRegistrationRepository netBadgeRelyingPartyRegistrationRepository(
            NetBadgeProperties properties) {
        String normalisedBaseUrl = properties.normalisedBaseUrl();
        String normalisedAssertionConsumerServiceBaseUrl =
                properties.normalisedAssertionConsumerServiceBaseUrl();
        RelyingPartyRegistration registration = RelyingPartyRegistrations
                .fromMetadataLocation(properties.idpMetadataUri())
                .registrationId(REGISTRATION_ID)
                .entityId(normalisedBaseUrl + METADATA_PATH)
                .assertionConsumerServiceLocation(
                        normalisedAssertionConsumerServiceBaseUrl
                                + ASSERTION_CONSUMER_SERVICE_PATH
                )
                .build();

        return new InMemoryRelyingPartyRegistrationRepository(registration);
    }

    @Bean
    AuthenticationSuccessHandler netBadgeAuthenticationSuccessHandler(
            NetBadgeProperties properties) {
        SimpleUrlAuthenticationSuccessHandler successHandler =
                new SimpleUrlAuthenticationSuccessHandler(
                        properties.normalisedFrontendBaseUrl());
        successHandler.setAlwaysUseDefaultTargetUrl(true);
        return successHandler;
    }

    @Bean
    CorsConfigurationSource netBadgeCorsConfigurationSource(NetBadgeProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(properties.normalisedFrontendBaseUrl()));
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Accept", "Content-Type", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    SecurityFilterChain netBadgeSecurityFilterChain(
            HttpSecurity http,
            AuthenticationSuccessHandler netBadgeAuthenticationSuccessHandler,
            CorsConfigurationSource netBadgeCorsConfigurationSource)
            throws Exception {
        http
                .cors(cors -> cors.configurationSource(netBadgeCorsConfigurationSource))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health",
                                "/error",
                                "/saml2/metadata/**",
                                "/saml2/service-provider-metadata/**"
                        )
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                PathPatternRequestMatcher.pathPattern("/api/**")))
                .saml2Login(saml -> saml
                        .successHandler(netBadgeAuthenticationSuccessHandler))
                .saml2Metadata(Customizer.withDefaults())
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }
}

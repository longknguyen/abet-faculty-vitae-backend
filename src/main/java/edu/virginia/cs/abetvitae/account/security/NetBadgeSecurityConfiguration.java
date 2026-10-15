package edu.virginia.cs.abetvitae.account.security;

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
        RelyingPartyRegistration registration = RelyingPartyRegistrations
                .fromMetadataLocation(properties.idpMetadataUri())
                .registrationId(REGISTRATION_ID)
                .entityId(normalisedBaseUrl + METADATA_PATH)
                .assertionConsumerServiceLocation(
                        normalisedBaseUrl + ASSERTION_CONSUMER_SERVICE_PATH
                )
                .build();

        return new InMemoryRelyingPartyRegistrationRepository(registration);
    }

    @Bean
    SecurityFilterChain netBadgeSecurityFilterChain(HttpSecurity http) throws Exception {
        http
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
                .saml2Login(saml2 -> saml2
                        .defaultSuccessUrl("/api/auth/session", false))
                .saml2Metadata(Customizer.withDefaults());

        return http.build();
    }
}

package edu.virginia.cs.abetvitae.account.security;

import edu.virginia.cs.abetvitae.account.model.RoleName;
import edu.virginia.cs.abetvitae.account.model.UserAccount;
import edu.virginia.cs.abetvitae.account.repository.UserAccountRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;

/** Resolves an authorised NetBadge principal to its local application account. */
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class AuthenticatedUserService {

    private final UserAccountRepository userAccountRepository;
    private final IdentityAttributeProperties attributeProperties;
    private final GrouperRoleResolver grouperRoleResolver;

    @Transactional
    public AuthenticatedUser requireUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        PrincipalAttributes attributes = attributes(authentication);
        if (attributes.roles().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The authenticated identity is not a member of an authorised Grouper group");
        }

        UserAccount account = findAccount(attributes)
                .orElseGet(() -> provisionAccount(attributes));
        if (!account.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The application account is disabled");
        }

        bindExternalSubject(account, attributes.externalSubject());
        String displayName = firstNonBlank(attributes.displayName(), localPart(account.getEmail()));
        return new AuthenticatedUser(
                account.getId(),
                displayName,
                blankToNull(attributes.surname()),
                attributes.roles());
    }

    private Optional<UserAccount> findAccount(PrincipalAttributes attributes) {
        if (attributes.externalSubject() != null) {
            Optional<UserAccount> bySubject = userAccountRepository.findByExternalSubject(
                    attributes.externalSubject());
            if (bySubject.isPresent()) {
                return bySubject;
            }
        }
        return userAccountRepository.findByEmailIgnoreCase(attributes.email());
    }

    private UserAccount provisionAccount(PrincipalAttributes attributes) {
        if (attributes.externalSubject() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The identity provider did not release a stable user identifier");
        }
        UserAccount account = UserAccount.fromNetBadgeIdentity(
                attributes.email(),
                attributes.externalSubject());
        return userAccountRepository.save(account);
    }

    private static void bindExternalSubject(UserAccount account, String externalSubject) {
        if (externalSubject == null) {
            return;
        }
        if (account.getExternalSubject() == null) {
            account.setExternalSubject(externalSubject);
        } else if (!account.getExternalSubject().equals(externalSubject)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The authenticated identity does not match the application account");
        }
    }

    private PrincipalAttributes attributes(Authentication authentication) {
        if (authentication instanceof Saml2AssertionAuthentication samlAuthentication) {
            Saml2ResponseAssertionAccessor assertion = samlAuthentication.getCredentials();
            if (assertion == null) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "The SAML assertion is missing");
            }
            String email = requiredAttribute(assertion, attributeProperties.emailAttribute());
            return new PrincipalAttributes(
                    blankToNull(assertion.getFirstAttribute(attributeProperties.subjectAttribute())),
                    email,
                    blankToNull(assertion.getFirstAttribute(
                            attributeProperties.displayNameAttribute())),
                    blankToNull(assertion.getFirstAttribute(
                            attributeProperties.surnameAttribute())),
                    grouperRoleResolver.resolve(assertion));
        }

        String email = authentication.getName();
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The authenticated identity did not supply an email address");
        }
        return new PrincipalAttributes(null, email, null, null, Set.of());
    }

    private static String requiredAttribute(
            Saml2ResponseAssertionAccessor assertion, String attributeName) {
        String value = assertion.getFirstAttribute(attributeName);
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The identity provider did not release a required attribute");
        }
        return value;
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private static String localPart(String email) {
        int separator = email.indexOf('@');
        return separator > 0 ? email.substring(0, separator) : email;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record PrincipalAttributes(
            String externalSubject,
            String email,
            String displayName,
            String surname,
            Set<RoleName> roles) {
    }
}

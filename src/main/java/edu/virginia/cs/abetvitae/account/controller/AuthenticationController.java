package edu.virginia.cs.abetvitae.account.controller;

import edu.virginia.cs.abetvitae.account.dto.AuthenticatedSessionResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reports the identity associated with the current authenticated session. */
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @GetMapping("/session")
    AuthenticatedSessionResponse currentSession(Authentication authentication) {
        return new AuthenticatedSessionResponse(true, authentication.getName());
    }
}

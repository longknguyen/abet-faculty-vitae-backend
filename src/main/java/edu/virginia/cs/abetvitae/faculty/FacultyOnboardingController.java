package edu.virginia.cs.abetvitae.faculty;

import jakarta.validation.Valid;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUser;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUserService;
import edu.virginia.cs.abetvitae.faculty.dto.FacultyOnboardingRequest;
import edu.virginia.cs.abetvitae.faculty.dto.FacultyOnboardingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/faculty/onboarding")
@RequiredArgsConstructor
public class FacultyOnboardingController {

    private final AuthenticatedUserService authenticatedUserService;
    private final FacultyOnboardingService facultyOnboardingService;

    @PostMapping
    public FacultyOnboardingResponse onboard(
            Authentication authentication,
            @Valid @RequestBody FacultyOnboardingRequest request){
        AuthenticatedUser user = authenticatedUserService.requireUser(authentication);
        var result = facultyOnboardingService.onboard(user, request);
        return result.response();
    }
}


package edu.virginia.cs.abetvitae.accreditation.controller;

import edu.virginia.cs.abetvitae.account.security.AuthenticatedUser;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUserService;
import edu.virginia.cs.abetvitae.accreditation.dto.ProvisionReviewCycleResponse;
import edu.virginia.cs.abetvitae.accreditation.dto.ReviewCycleRequest;
import edu.virginia.cs.abetvitae.accreditation.dto.ReviewCycleResponse;
import edu.virginia.cs.abetvitae.accreditation.service.ReviewCycleAdminService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/review-cycles")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ReviewCycleAdminController {

    private final AuthenticatedUserService authenticatedUserService;
    private final ReviewCycleAdminService reviewCycleAdminService;

    @GetMapping
    public List<ReviewCycleResponse> list(Authentication authentication) {
        return reviewCycleAdminService.list(requireUser(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewCycleResponse create(
            Authentication authentication,
            @Valid @RequestBody ReviewCycleRequest request) {
        return reviewCycleAdminService.create(requireUser(authentication), request);
    }

    @PutMapping("/{templateId}")
    public ReviewCycleResponse update(
            Authentication authentication,
            @PathVariable UUID templateId,
            @Valid @RequestBody ReviewCycleRequest request) {
        return reviewCycleAdminService.update(requireUser(authentication), templateId, request);
    }

    @PostMapping("/{templateId}/publish")
    public ReviewCycleResponse publish(
            Authentication authentication,
            @PathVariable UUID templateId) {
        return reviewCycleAdminService.publish(requireUser(authentication), templateId);
    }

    @PostMapping("/{templateId}/retire")
    public ReviewCycleResponse retire(
            Authentication authentication,
            @PathVariable UUID templateId) {
        return reviewCycleAdminService.retire(requireUser(authentication), templateId);
    }

    @PostMapping("/{templateId}/provision")
    public ProvisionReviewCycleResponse provision(
            Authentication authentication,
            @PathVariable UUID templateId) {
        return reviewCycleAdminService.provision(requireUser(authentication), templateId);
    }

    private AuthenticatedUser requireUser(Authentication authentication) {
        return authenticatedUserService.requireUser(authentication);
    }
}

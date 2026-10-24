package edu.virginia.cs.abetvitae.accreditation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.virginia.cs.abetvitae.account.model.RoleName;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUser;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUserService;
import edu.virginia.cs.abetvitae.accreditation.dto.ReviewCycleResponse;
import edu.virginia.cs.abetvitae.accreditation.model.AbetTemplateStatus;
import edu.virginia.cs.abetvitae.accreditation.service.ReviewCycleAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewCycleAdminControllerTest {

    private static final UUID TEMPLATE_ID =
            UUID.fromString("0199f80f-4e52-7785-ad78-da3215e9e746");

    private AuthenticatedUser user;
    private ReviewCycleAdminService service;
    private TestingAuthenticationToken authentication;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthenticatedUserService authenticatedUserService =
                mock(AuthenticatedUserService.class);
        service = mock(ReviewCycleAdminService.class);
        user = new AuthenticatedUser(
                UUID.fromString("0199f80f-3bf3-7ed1-b5f8-060591413962"),
                "Admin User",
                "User",
                Set.of(RoleName.ADMIN));
        authentication = new TestingAuthenticationToken("admin@virginia.edu", null);
        authentication.setAuthenticated(true);
        when(authenticatedUserService.requireUser(authentication)).thenReturn(user);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ReviewCycleAdminController(authenticatedUserService, service)).build();
    }

    @Test
    void listsReviewCyclesForTheAuthenticatedAdministrator() throws Exception {
        when(service.list(user)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/admin/review-cycles").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(TEMPLATE_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    @Test
    void createsAValidatedDraft() throws Exception {
        when(service.create(any(), any())).thenReturn(response());
        String request = new ObjectMapper().writeValueAsString(Map.of(
                "label", "2026–27",
                "departmentName", "Computer Science",
                "requiredSections", List.of("PROFILE"),
                "formattingRules", Map.of(),
                "renderingTemplateName", "faculty-vitae"));

        mockMvc.perform(post("/api/admin/review-cycles")
                        .principal(authentication)
                        .contentType("application/json")
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("2026–27"));
    }

    private static ReviewCycleResponse response() {
        return new ReviewCycleResponse(
                TEMPLATE_ID,
                "2026–27",
                "Computer Science",
                1,
                AbetTemplateStatus.DRAFT,
                List.of("PROFILE"),
                Map.of(),
                "faculty-vitae",
                null,
                0,
                2);
    }
}

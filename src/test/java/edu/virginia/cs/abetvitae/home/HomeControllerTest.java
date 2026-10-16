package edu.virginia.cs.abetvitae.home;

import edu.virginia.cs.abetvitae.account.model.RoleName;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUser;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUserService;
import edu.virginia.cs.abetvitae.home.dto.HomeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HomeControllerTest {

    private AuthenticatedUserService authenticatedUserService;
    private HomeService homeService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authenticatedUserService = mock(AuthenticatedUserService.class);
        homeService = mock(HomeService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new HomeController(authenticatedUserService, homeService)).build();
    }

    @Test
    void returnsFrontendHomeContract() throws Exception {
        UUID accountId = UUID.fromString("0199f80f-3bf3-7ed1-b5f8-060591413962");
        UUID templateId = UUID.fromString("0199f80f-4e52-7785-ad78-da3215e9e746");
        AuthenticatedUser user = new AuthenticatedUser(
                accountId,
                "Alex Morgan",
                "Morgan",
                Set.of(RoleName.FACULTY));
        HomeResponse response = new HomeResponse(
                new HomeResponse.CurrentUser(
                        accountId,
                        "Alex Morgan",
                        "Morgan",
                        Set.of(RoleName.FACULTY)),
                new HomeResponse.Cycle(templateId, "2026–27", "Computer Science"),
                null,
                List.of());
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "faculty@virginia.edu",
                null);
        authentication.setAuthenticated(true);
        when(authenticatedUserService.requireUser(authentication)).thenReturn(user);
        when(homeService.loadHome(user)).thenReturn(response);

        mockMvc.perform(get("/api/v1/home").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentUser.id").value(accountId.toString()))
                .andExpect(jsonPath("$.currentUser.roles[0]").value("FACULTY"))
                .andExpect(jsonPath("$.cycle.id").value(templateId.toString()))
                .andExpect(jsonPath("$.cycle.label").value("2026–27"))
                .andExpect(jsonPath("$.cycle.departmentName").value("Computer Science"))
                .andExpect(jsonPath("$.personalVitae").doesNotExist())
                .andExpect(jsonPath("$.facultyRecords").isArray());
    }
}

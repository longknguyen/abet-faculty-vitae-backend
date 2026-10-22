package edu.virginia.cs.abetvitae;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DevelopmentAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loadsTheFrontendContractAsTheDevelopmentUser() throws Exception {
        mockMvc.perform(get("/api/home"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(jsonPath("$.currentUser.displayName").value("Faculty Developer"))
                .andExpect(jsonPath("$.currentUser.surname").value("Developer"))
                .andExpect(jsonPath("$.currentUser.roles[0]").value("FACULTY"))
                .andExpect(jsonPath("$.onboardingNeeded").value(true));
    }
}

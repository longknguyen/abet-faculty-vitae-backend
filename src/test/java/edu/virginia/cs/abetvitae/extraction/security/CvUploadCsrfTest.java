package edu.virginia.cs.abetvitae.extraction.security;

import edu.virginia.cs.abetvitae.account.security.AuthenticatedUserService;
import edu.virginia.cs.abetvitae.account.security.DevelopmentAuthenticationProperties;
import edu.virginia.cs.abetvitae.account.security.DevelopmentSecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CvUploadCsrfTest.TestUploadController.class,
        properties = {
                "server.address=127.0.0.1",
                "app.development-authentication.email=faculty.dev@virginia.edu",
                "app.development-authentication.display-name=Faculty Developer",
                "app.development-authentication.surname=Developer",
                "app.development-authentication.roles=FACULTY"
        }
)
@ActiveProfiles("dev")
@Import({
        DevelopmentSecurityConfiguration.class,
        CvUploadCsrfTest.TestUploadController.class
})
@EnableConfigurationProperties(DevelopmentAuthenticationProperties.class)
class CvUploadCsrfTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticatedUserService authenticatedUserService;

    @Test
    void rejectsUploadWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/cv-uploads"))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsUploadWithValidCsrfToken() throws Exception {
        mockMvc.perform(post("/api/cv-uploads").with(csrf()))
                .andExpect(status().isOk());
    }

    @RestController
    static class TestUploadController {

        @PostMapping("/api/cv-uploads")
        String upload() {
            return "Upload reached controller";
        }
    }
}
package edu.virginia.cs.abetvitae.extraction.controller;

import edu.virginia.cs.abetvitae.extraction.model.CvUpload;
import edu.virginia.cs.abetvitae.extraction.service.CvUploadService;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
class CvUploadControllerTest {

    private CvUploadService cvUploadService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cvUploadService = mock(CvUploadService.class);
        mockMvc = standaloneSetup(
                new CvUploadController(cvUploadService)
        ).build();
    }

    @Test
    void uploadsCvSuccessfully() throws Exception {
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("faculty@virginia.edu", null);
        authentication.setAuthenticated(true);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "test PDF content".getBytes()
        );

        CvUpload upload = mock(CvUpload.class);

        when(cvUploadService.uploadCv(authentication, file)).thenReturn(upload);

        when(upload.getId()).thenReturn(UUID.randomUUID());
        when(upload.getOriginalFilename()).thenReturn("resume.pdf");
        when(upload.getDetectedMimeType()).thenReturn("application/pdf");
        when(upload.getFileSizeBytes()).thenReturn(16L);
        when(upload.getUploadedAt()).thenReturn(Instant.now());

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/cv-uploads")
                        .file(file)
                        .principal(authentication)
        ).andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("resume.pdf"))
                .andExpect(jsonPath("$.detectedMimeType").value("application/pdf"))
                .andExpect(jsonPath("$.fileSizeBytes").value(16));

        verify(cvUploadService).uploadCv(authentication, file);
    }
}

package edu.virginia.cs.abetvitae.extraction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class CvDocumentInspectorTest {

    private final CvDocumentInspector inspector = new CvDocumentInspector(
            new CvUploadProperties(1024, Duration.ofDays(90))
    );

    @Test
    void acceptsPdfWithSanitisedBasename() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../faculty-vitae.pdf",
                "application/octet-stream",
                "%PDF-1.7 safe fixture".getBytes(StandardCharsets.US_ASCII)
        );

        InspectedCvDocument inspected = inspector.inspect(file);

        assertThat(inspected.filename()).isEqualTo("faculty-vitae.pdf");
        assertThat(inspected.mediaType()).isEqualTo("application/pdf");
    }

    @Test
    void acceptsDocxZipSignatureWithoutTrustingClientMediaType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "faculty-vitae.DOCX",
                "text/plain",
                new byte[]{'P', 'K', 3, 4, 0, 0}
        );

        InspectedCvDocument inspected = inspector.inspect(file);

        assertThat(inspected.mediaType()).isEqualTo(
                "application/vnd.openxmlformats-officedocument."
                        + "wordprocessingml.document"
        );
    }

    @Test
    void rejectsExtensionThatDoesNotMatchSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "faculty-vitae.pdf",
                "application/pdf",
                "not a PDF".getBytes(StandardCharsets.US_ASCII)
        );

        assertThatThrownBy(() -> inspector.inspect(file))
                .isInstanceOf(CvUploadRejectedException.class)
                .hasMessage(
                        "The filename extension does not match the uploaded document."
                );
    }

    @Test
    void rejectsFileAboveConfiguredLimit() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "faculty-vitae.pdf",
                "application/pdf",
                new byte[1025]
        );

        assertThatThrownBy(() -> inspector.inspect(file))
                .isInstanceOf(CvUploadRejectedException.class)
                .hasMessage(
                        "The uploaded CV exceeds the configured size limit."
                );
    }
}

package edu.virginia.cs.abetvitae.document.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedDocumentTest {

    @Test
    void newDocumentAwaitsValidation() {
        GeneratedDocument document = new GeneratedDocument();

        assertThat(document.getValidationStatus())
                .isEqualTo(DocumentValidationStatus.PENDING);
    }
}

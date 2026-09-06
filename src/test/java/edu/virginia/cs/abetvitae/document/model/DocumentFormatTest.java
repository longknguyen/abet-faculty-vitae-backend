package edu.virginia.cs.abetvitae.document.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentFormatTest {

    @Test
    void valuesMatchPersistenceContract() {
        assertThat(DocumentFormat.values()).containsExactly(
                DocumentFormat.PDF,
                DocumentFormat.DOCX
        );
    }
}

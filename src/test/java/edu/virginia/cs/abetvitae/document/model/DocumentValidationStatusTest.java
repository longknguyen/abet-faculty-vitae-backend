package edu.virginia.cs.abetvitae.document.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentValidationStatusTest {

    @Test
    void valuesMatchPersistenceContract() {
        assertThat(DocumentValidationStatus.values()).containsExactly(
                DocumentValidationStatus.PENDING,
                DocumentValidationStatus.VALID,
                DocumentValidationStatus.INVALID
        );
    }
}

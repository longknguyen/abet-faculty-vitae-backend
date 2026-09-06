package edu.virginia.cs.abetvitae.extraction.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractionRunStatusTest {

    @Test
    void valuesMatchPersistenceContract() {
        assertThat(ExtractionRunStatus.values()).containsExactly(
                ExtractionRunStatus.RUNNING,
                ExtractionRunStatus.SUCCEEDED,
                ExtractionRunStatus.FAILED
        );
    }
}

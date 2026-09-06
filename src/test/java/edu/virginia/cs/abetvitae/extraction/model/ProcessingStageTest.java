package edu.virginia.cs.abetvitae.extraction.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessingStageTest {

    @Test
    void valuesMatchPersistenceContract() {
        assertThat(ProcessingStage.values()).containsExactly(
                ProcessingStage.QUEUED,
                ProcessingStage.VALIDATING,
                ProcessingStage.EXTRACTING_TEXT,
                ProcessingStage.STRUCTURING_WITH_AI,
                ProcessingStage.CREATING_REVIEW,
                ProcessingStage.COMPLETE
        );
    }
}

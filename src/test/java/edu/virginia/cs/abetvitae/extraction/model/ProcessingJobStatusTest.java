package edu.virginia.cs.abetvitae.extraction.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessingJobStatusTest {

    @Test
    void valuesMatchPersistenceContract() {
        assertThat(ProcessingJobStatus.values()).containsExactly(
                ProcessingJobStatus.QUEUED,
                ProcessingJobStatus.RUNNING,
                ProcessingJobStatus.SUCCEEDED,
                ProcessingJobStatus.FAILED
        );
    }
}

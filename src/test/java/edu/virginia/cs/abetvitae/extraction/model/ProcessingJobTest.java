package edu.virginia.cs.abetvitae.extraction.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessingJobTest {

    @Test
    void newJobStartsQueuedAtTheQueuedStage() {
        ProcessingJob job = new ProcessingJob();

        assertThat(job.getStatus()).isEqualTo(ProcessingJobStatus.QUEUED);
        assertThat(job.getCurrentStage()).isEqualTo(ProcessingStage.QUEUED);
    }
}

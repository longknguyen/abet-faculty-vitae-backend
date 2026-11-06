package edu.virginia.cs.abetvitae.extraction.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessingJobTest {

    @Test
    void newJobStartsQueuedAtTheQueuedStage() {
        ProcessingJob job = new ProcessingJob();

        assertThat(job.getStatus()).isEqualTo(ProcessingJobStatus.QUEUED);
        assertThat(job.getCurrentStage()).isEqualTo(ProcessingStage.QUEUED);
    }

    @Test
    void jobTransitionsThroughExtractionAndMapping() {
        ProcessingJob job = new ProcessingJob();
        Instant startedAt = Instant.parse("2026-11-05T12:00:00Z");

        job.start(startedAt);
        job.advanceTo(ProcessingStage.EXTRACTING_TEXT);
        job.advanceTo(ProcessingStage.STRUCTURING_WITH_AI);

        assertThat(job.getStatus()).isEqualTo(ProcessingJobStatus.RUNNING);
        assertThat(job.getStartedAt()).isEqualTo(startedAt);
        assertThat(job.hasReachedStructuredMapping()).isTrue();
    }

    @Test
    void failedJobRetainsSafeFailureAndCompletionTime() {
        ProcessingJob job = new ProcessingJob();
        Instant failedAt = Instant.parse("2026-11-05T12:00:00Z");
        job.start(failedAt.minusSeconds(5));

        job.fail(failedAt, "The CV could not be processed.");

        assertThat(job.getStatus()).isEqualTo(ProcessingJobStatus.FAILED);
        assertThat(job.getCompletedAt()).isEqualTo(failedAt);
        assertThat(job.getErrorMessage())
                .isEqualTo("The CV could not be processed.");
    }
}

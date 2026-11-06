package edu.virginia.cs.abetvitae.extraction.service;

import edu.virginia.cs.abetvitae.extraction.model.ProcessingJobStatus;
import edu.virginia.cs.abetvitae.extraction.model.ProcessingStage;
import java.time.Instant;
import java.util.UUID;

public record CvProcessingStatus(
        UUID processingJobId,
        ProcessingJobStatus status,
        ProcessingStage currentStage,
        String errorMessage,
        Instant queuedAt,
        Instant startedAt,
        Instant completedAt) {
}

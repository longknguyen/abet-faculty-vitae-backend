package edu.virginia.cs.abetvitae.extraction.messaging;

import edu.virginia.cs.abetvitae.extraction.model.ProcessingJob;
import edu.virginia.cs.abetvitae.extraction.repository.ProcessingJobRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.cv-processing.enabled",
        havingValue = "true"
)
class CvProcessingFailureRecorder {

    private static final String RETRIES_EXHAUSTED =
            "CV processing services were unavailable after multiple attempts.";

    private final ProcessingJobRepository processingJobRepository;

    @Transactional
    public void recordRetriesExhausted(UUID processingJobId) {
        ProcessingJob job = processingJobRepository
                .findByIdForUpdate(processingJobId)
                .orElse(null);
        if (job != null && !job.hasReachedStructuredMapping()) {
            job.fail(Instant.now(), RETRIES_EXHAUSTED);
        }
    }
}

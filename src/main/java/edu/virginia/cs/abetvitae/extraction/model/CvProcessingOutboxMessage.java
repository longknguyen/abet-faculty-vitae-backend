package edu.virginia.cs.abetvitae.extraction.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/** Durable intent to publish one processing job after its upload transaction commits. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "cv_processing_outbox")
public class CvProcessingOutboxMessage {

    private static final int MAXIMUM_ERROR_LENGTH = 500;
    private static final int MAXIMUM_BACKOFF_EXPONENT = 9;
    private static final Duration MAXIMUM_BACKOFF = Duration.ofMinutes(5);

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processing_job_id", nullable = false, unique = true)
    private ProcessingJob processingJob;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", length = MAXIMUM_ERROR_LENGTH)
    private String lastError;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    public static CvProcessingOutboxMessage create(ProcessingJob processingJob) {
        CvProcessingOutboxMessage message = new CvProcessingOutboxMessage();
        message.processingJob = java.util.Objects.requireNonNull(processingJob);
        message.nextAttemptAt = Instant.now();
        return message;
    }

    public void markPublished(Instant now) {
        publishedAt = java.util.Objects.requireNonNull(now);
        lastError = null;
    }

    public void recordFailure(Instant now, String safeError) {
        java.util.Objects.requireNonNull(now);
        attemptCount++;
        int exponent = Math.min(attemptCount - 1, MAXIMUM_BACKOFF_EXPONENT);
        Duration delay = Duration.ofSeconds(1L << exponent);
        nextAttemptAt = now.plus(delay.compareTo(MAXIMUM_BACKOFF) > 0
                ? MAXIMUM_BACKOFF
                : delay);
        lastError = truncate(safeError);
    }

    private static String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "Message publication failed";
        }
        return value.length() <= MAXIMUM_ERROR_LENGTH
                ? value
                : value.substring(0, MAXIMUM_ERROR_LENGTH);
    }
}

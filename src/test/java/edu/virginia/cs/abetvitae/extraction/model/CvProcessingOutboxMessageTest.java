package edu.virginia.cs.abetvitae.extraction.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CvProcessingOutboxMessageTest {

    @Test
    void schedulesBoundedExponentialPublicationRetries() {
        CvProcessingOutboxMessage message =
                CvProcessingOutboxMessage.create(new ProcessingJob());
        Instant now = Instant.parse("2026-11-05T12:00:00Z");

        message.recordFailure(now, "Broker unavailable");
        assertThat(message.getAttemptCount()).isOne();
        assertThat(message.getNextAttemptAt()).isEqualTo(now.plusSeconds(1));

        for (int attempt = 2; attempt <= 20; attempt++) {
            message.recordFailure(now, "Broker unavailable");
        }

        assertThat(Duration.between(now, message.getNextAttemptAt()))
                .isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void clearsFailureWhenPublicationIsConfirmed() {
        CvProcessingOutboxMessage message =
                CvProcessingOutboxMessage.create(new ProcessingJob());
        Instant failedAt = Instant.parse("2026-11-05T12:00:00Z");
        Instant publishedAt = failedAt.plusSeconds(5);
        message.recordFailure(failedAt, "Broker unavailable");

        message.markPublished(publishedAt);

        assertThat(message.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(message.getLastError()).isNull();
    }
}

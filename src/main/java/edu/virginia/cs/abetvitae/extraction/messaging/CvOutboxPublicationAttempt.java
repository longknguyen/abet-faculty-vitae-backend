package edu.virginia.cs.abetvitae.extraction.messaging;

import edu.virginia.cs.abetvitae.extraction.model.CvProcessingOutboxMessage;
import edu.virginia.cs.abetvitae.extraction.repository.CvProcessingOutboxRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "app.cv-processing.enabled",
        havingValue = "true"
)
class CvOutboxPublicationAttempt {

    private static final String SAFE_PUBLICATION_ERROR =
            "RabbitMQ did not confirm the processing message";

    private final CvProcessingOutboxRepository outboxRepository;
    private final CvJobMessagePublisher messagePublisher;

    @Transactional
    public void publish(UUID outboxMessageId) {
        CvProcessingOutboxMessage message = outboxRepository
                .findById(outboxMessageId)
                .orElse(null);
        Instant now = Instant.now();
        if (message == null
                || message.getPublishedAt() != null
                || message.getNextAttemptAt().isAfter(now)) {
            return;
        }

        try {
            messagePublisher.publish(message.getProcessingJob().getId());
            message.markPublished(now);
        } catch (RuntimeException exception) {
            log.warn(
                    "CV processing message publication failed for outbox entry {}",
                    outboxMessageId,
                    exception
            );
            message.recordFailure(now, SAFE_PUBLICATION_ERROR);
        }
    }
}

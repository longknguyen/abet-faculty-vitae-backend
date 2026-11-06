package edu.virginia.cs.abetvitae.extraction.messaging;

import edu.virginia.cs.abetvitae.extraction.repository.CvProcessingOutboxRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "app.cv-processing.enabled",
        havingValue = "true"
)
class CvProcessingOutboxPoller {

    private final CvProcessingOutboxRepository outboxRepository;
    private final CvOutboxPublicationAttempt publicationAttempt;

    @Scheduled(fixedDelayString = "${app.cv-processing.outbox-poll-interval}")
    public void publishPendingMessages() {
        List<UUID> messageIds = outboxRepository
                .findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAt(
                        Instant.now()
                )
                .stream()
                .map(message -> message.getId())
                .toList();
        for (UUID messageId : messageIds) {
            try {
                publicationAttempt.publish(messageId);
            } catch (RuntimeException exception) {
                log.error(
                        "Could not update CV processing outbox entry {}",
                        messageId,
                        exception
                );
            }
        }
    }
}

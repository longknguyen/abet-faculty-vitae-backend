package edu.virginia.cs.abetvitae.extraction.messaging;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "app.cv-processing.enabled",
        havingValue = "true"
)
class CvProcessingDeadLetterRecoverer implements MessageRecoverer {

    private final CvProcessingFailureRecorder failureRecorder;

    @Override
    public void recover(Message message, Throwable cause) {
        UUID processingJobId = parseJobId(message);
        if (processingJobId != null) {
            try {
                failureRecorder.recordRetriesExhausted(processingJobId);
            } catch (RuntimeException exception) {
                log.error(
                        "Could not record exhausted retries for processing job {}",
                        processingJobId,
                        exception
                );
            }
        }
        new RejectAndDontRequeueRecoverer(
                "CV processing retries were exhausted"
        ).recover(message, cause);
    }

    private static UUID parseJobId(Message message) {
        byte[] body = message.getBody();
        if (body.length != 36) {
            return null;
        }
        try {
            return UUID.fromString(new String(body, StandardCharsets.US_ASCII));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}

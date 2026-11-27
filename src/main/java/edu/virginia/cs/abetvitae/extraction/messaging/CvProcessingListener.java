package edu.virginia.cs.abetvitae.extraction.messaging;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.cv-processing.enabled",
        havingValue = "true"
)
class CvProcessingListener {

    private final CvProcessingService processingService;

    @RabbitListener(
            queues = CvProcessingQueues.QUEUE,
            containerFactory = "cvProcessingListenerContainerFactory"
    )
    public void process(String processingJobId) {
        UUID parsedJobId;
        try {
            parsedJobId = UUID.fromString(processingJobId);
        } catch (IllegalArgumentException exception) {
            throw new AmqpRejectAndDontRequeueException(
                    "Invalid CV processing message",
                    exception
            );
        }
        processingService.process(parsedJobId);
    }
}

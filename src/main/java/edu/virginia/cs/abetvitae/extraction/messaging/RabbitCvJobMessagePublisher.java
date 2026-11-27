package edu.virginia.cs.abetvitae.extraction.messaging;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.cv-processing.enabled",
        havingValue = "true"
)
final class RabbitCvJobMessagePublisher implements CvJobMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final CvProcessingProperties properties;

    RabbitCvJobMessagePublisher(
            RabbitTemplate rabbitTemplate,
            CvProcessingProperties properties
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(UUID processingJobId) {
        String messageId = processingJobId.toString();
        CorrelationData correlationData = new CorrelationData(messageId);
        rabbitTemplate.convertAndSend(
                CvProcessingQueues.EXCHANGE,
                CvProcessingQueues.ROUTING_KEY,
                messageId,
                message -> {
                    message.getMessageProperties().setDeliveryMode(
                            MessageDeliveryMode.PERSISTENT
                    );
                    message.getMessageProperties().setContentType(
                            MediaType.TEXT_PLAIN_VALUE
                    );
                    message.getMessageProperties().setMessageId(messageId);
                    return message;
                },
                correlationData
        );

        try {
            CorrelationData.Confirm confirm = correlationData.getFuture().get(
                    properties.publisherConfirmTimeout().toMillis(),
                    TimeUnit.MILLISECONDS
            );
            if (!confirm.ack() || correlationData.getReturned() != null) {
                throw new CvMessagePublicationException(
                        "RabbitMQ did not route and confirm the processing message"
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CvMessagePublicationException(
                    "Processing message publication was interrupted",
                    exception
            );
        } catch (ExecutionException | TimeoutException exception) {
            throw new CvMessagePublicationException(
                    "RabbitMQ did not confirm the processing message",
                    exception
            );
        }
    }
}

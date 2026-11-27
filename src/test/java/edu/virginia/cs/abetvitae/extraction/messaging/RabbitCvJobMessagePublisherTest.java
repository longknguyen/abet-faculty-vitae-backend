package edu.virginia.cs.abetvitae.extraction.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitCvJobMessagePublisherTest {

    @Test
    void publishesJobIdentifierAfterBrokerAcknowledgement() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        completeConfirmation(rabbitTemplate, true);
        RabbitCvJobMessagePublisher publisher = new RabbitCvJobMessagePublisher(
                rabbitTemplate,
                properties()
        );
        UUID jobId = UUID.randomUUID();

        publisher.publish(jobId);

        verify(rabbitTemplate).convertAndSend(
                eq(CvProcessingQueues.EXCHANGE),
                eq(CvProcessingQueues.ROUTING_KEY),
                eq(jobId.toString()),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );
    }

    @Test
    void rejectsNegativeBrokerConfirmation() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        completeConfirmation(rabbitTemplate, false);
        RabbitCvJobMessagePublisher publisher = new RabbitCvJobMessagePublisher(
                rabbitTemplate,
                properties()
        );

        assertThatThrownBy(() -> publisher.publish(UUID.randomUUID()))
                .isInstanceOf(CvMessagePublicationException.class)
                .hasMessage(
                        "RabbitMQ did not route and confirm the processing message"
                );
    }

    private static void completeConfirmation(
            RabbitTemplate rabbitTemplate,
            boolean acknowledged
    ) {
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(4);
            correlationData.getFuture().complete(
                    new CorrelationData.Confirm(acknowledged, null)
            );
            return null;
        }).when(rabbitTemplate).convertAndSend(
                eq(CvProcessingQueues.EXCHANGE),
                eq(CvProcessingQueues.ROUTING_KEY),
                any(String.class),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );
    }

    private static CvProcessingProperties properties() {
        return new CvProcessingProperties(
                true,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                3,
                Duration.ofSeconds(1),
                2,
                Duration.ofSeconds(10)
        );
    }
}

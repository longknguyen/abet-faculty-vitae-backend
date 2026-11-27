package edu.virginia.cs.abetvitae.extraction.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class CvProcessingDeadLetterRecovererTest {

    @Test
    void recordsExhaustedRetriesBeforeRejectingForDeadLettering() {
        CvProcessingFailureRecorder recorder =
                mock(CvProcessingFailureRecorder.class);
        CvProcessingDeadLetterRecoverer recoverer =
                new CvProcessingDeadLetterRecoverer(recorder);
        UUID jobId = UUID.randomUUID();
        Message message = new Message(
                jobId.toString().getBytes(StandardCharsets.US_ASCII),
                new MessageProperties()
        );

        assertThatThrownBy(() -> recoverer.recover(
                message,
                new CvProcessingRetryableException(
                        "Worker unavailable",
                        new IllegalStateException()
                )
        )).isInstanceOf(RuntimeException.class)
                .hasMessage("CV processing retries were exhausted");

        verify(recorder).recordRetriesExhausted(jobId);
    }

    @Test
    void rejectsMalformedMessageWithoutUpdatingAJob() {
        CvProcessingFailureRecorder recorder =
                mock(CvProcessingFailureRecorder.class);
        CvProcessingDeadLetterRecoverer recoverer =
                new CvProcessingDeadLetterRecoverer(recorder);
        Message message = new Message(
                "invalid".getBytes(StandardCharsets.US_ASCII),
                new MessageProperties()
        );

        assertThatThrownBy(() -> recoverer.recover(
                message,
                new IllegalArgumentException()
        )).isInstanceOf(RuntimeException.class);

        verify(recorder, never()).recordRetriesExhausted(
                org.mockito.ArgumentMatchers.any()
        );
    }
}

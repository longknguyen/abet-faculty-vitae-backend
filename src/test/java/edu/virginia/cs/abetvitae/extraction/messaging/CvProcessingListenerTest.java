package edu.virginia.cs.abetvitae.extraction.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

class CvProcessingListenerTest {

    @Test
    void delegatesValidJobIdentifier() {
        CvProcessingService service = mock(CvProcessingService.class);
        CvProcessingListener listener = new CvProcessingListener(service);
        UUID jobId = UUID.randomUUID();

        listener.process(jobId.toString());

        verify(service).process(jobId);
    }

    @Test
    void rejectsMalformedMessageWithoutCallingProcessingService() {
        CvProcessingService service = mock(CvProcessingService.class);
        CvProcessingListener listener = new CvProcessingListener(service);

        assertThatThrownBy(() -> listener.process("not-a-job-id"))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessage("Invalid CV processing message");
    }
}

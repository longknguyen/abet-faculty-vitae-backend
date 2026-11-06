package edu.virginia.cs.abetvitae.extraction.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.virginia.cs.abetvitae.extraction.model.CvProcessingOutboxMessage;
import edu.virginia.cs.abetvitae.extraction.model.ProcessingJob;
import edu.virginia.cs.abetvitae.extraction.repository.CvProcessingOutboxRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CvOutboxPublicationAttemptTest {

    @Test
    void marksMessagePublishedOnlyAfterPublisherReturns() {
        CvProcessingOutboxRepository repository =
                mock(CvProcessingOutboxRepository.class);
        CvJobMessagePublisher publisher = mock(CvJobMessagePublisher.class);
        ProcessingJob job = mock(ProcessingJob.class);
        UUID jobId = UUID.randomUUID();
        when(job.getId()).thenReturn(jobId);
        CvProcessingOutboxMessage message =
                CvProcessingOutboxMessage.create(job);
        UUID messageId = UUID.randomUUID();
        when(repository.findById(messageId)).thenReturn(Optional.of(message));
        CvOutboxPublicationAttempt attempt =
                new CvOutboxPublicationAttempt(repository, publisher);

        attempt.publish(messageId);

        verify(publisher).publish(jobId);
        assertThat(message.getPublishedAt()).isNotNull();
        assertThat(message.getAttemptCount()).isZero();
    }

    @Test
    void schedulesRetryWithoutPersistingExceptionDetails() {
        CvProcessingOutboxRepository repository =
                mock(CvProcessingOutboxRepository.class);
        CvJobMessagePublisher publisher = mock(CvJobMessagePublisher.class);
        ProcessingJob job = mock(ProcessingJob.class);
        UUID jobId = UUID.randomUUID();
        when(job.getId()).thenReturn(jobId);
        CvProcessingOutboxMessage message =
                CvProcessingOutboxMessage.create(job);
        UUID messageId = UUID.randomUUID();
        when(repository.findById(messageId)).thenReturn(Optional.of(message));
        org.mockito.Mockito.doThrow(
                new IllegalStateException("sensitive broker details")
        ).when(publisher).publish(jobId);
        CvOutboxPublicationAttempt attempt =
                new CvOutboxPublicationAttempt(repository, publisher);

        attempt.publish(messageId);

        assertThat(message.getPublishedAt()).isNull();
        assertThat(message.getAttemptCount()).isOne();
        assertThat(message.getLastError()).isEqualTo(
                "RabbitMQ did not confirm the processing message"
        );
    }
}

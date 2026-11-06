package edu.virginia.cs.abetvitae.extraction.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.virginia.cs.abetvitae.extraction.model.CvUpload;
import edu.virginia.cs.abetvitae.extraction.model.ExtractionRun;
import edu.virginia.cs.abetvitae.extraction.model.ExtractionRunStatus;
import edu.virginia.cs.abetvitae.extraction.model.ProcessingJob;
import edu.virginia.cs.abetvitae.extraction.model.ProcessingStage;
import edu.virginia.cs.abetvitae.extraction.parsing.CvTextExtractionException;
import edu.virginia.cs.abetvitae.extraction.parsing.CvTextExtractionFailure;
import edu.virginia.cs.abetvitae.extraction.parsing.ExtractedCvText;
import edu.virginia.cs.abetvitae.extraction.repository.ExtractionRunRepository;
import edu.virginia.cs.abetvitae.extraction.repository.ProcessingJobRepository;
import edu.virginia.cs.abetvitae.extraction.scanning.MalwareDetectedException;
import edu.virginia.cs.abetvitae.extraction.service.CvUploadProperties;
import edu.virginia.cs.abetvitae.extraction.service.SecureCvTextExtractionService;
import edu.virginia.cs.abetvitae.extraction.storage.PrivateCvStorage;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class CvProcessingServiceTest {

    private static final byte[] CONTENT =
            "%PDF-safe fixture".getBytes(StandardCharsets.US_ASCII);
    private static final UUID JOB_ID = UUID.randomUUID();

    private ProcessingJobRepository jobRepository;
    private ExtractionRunRepository runRepository;
    private PrivateCvStorage storage;
    private SecureCvTextExtractionService extractionService;
    private ProcessingJob job;
    private CvUpload upload;
    private CvProcessingService service;

    @BeforeEach
    void setUp() throws Exception {
        jobRepository = mock(ProcessingJobRepository.class);
        runRepository = mock(ExtractionRunRepository.class);
        storage = mock(PrivateCvStorage.class);
        extractionService = mock(SecureCvTextExtractionService.class);
        job = mock(ProcessingJob.class);
        upload = mock(CvUpload.class);

        when(jobRepository.findByIdForUpdate(JOB_ID))
                .thenReturn(Optional.of(job));
        when(job.getId()).thenReturn(JOB_ID);
        when(job.getCvUpload()).thenReturn(upload);
        when(job.hasReachedStructuredMapping()).thenReturn(false);
        when(runRepository.existsByProcessingJobIdAndStatus(
                JOB_ID,
                ExtractionRunStatus.SUCCEEDED
        )).thenReturn(false);
        when(runRepository.findFirstByProcessingJobIdOrderByRunNumberDesc(
                JOB_ID
        )).thenReturn(Optional.empty());
        when(upload.getTemporaryStorageKey()).thenReturn(UUID.randomUUID().toString());
        when(upload.getFileSizeBytes()).thenReturn((long) CONTENT.length);
        when(upload.getFileSha256()).thenReturn(sha256(CONTENT));
        when(upload.getOriginalFilename()).thenReturn("faculty-vitae.pdf");
        when(upload.getExpiresAt()).thenReturn(Instant.now().plus(Duration.ofDays(1)));
        when(storage.copyTo(
                eq(upload.getTemporaryStorageKey()),
                any(OutputStream.class),
                eq(1024L)
        )).thenAnswer(invocation -> {
            OutputStream output = invocation.getArgument(1);
            output.write(CONTENT);
            return (long) CONTENT.length;
        });
        service = new CvProcessingService(
                jobRepository,
                runRepository,
                storage,
                extractionService,
                new CvUploadProperties(1024, Duration.ofDays(90))
        );
    }

    @Test
    void verifiesScansAndExtractsBeforeAdvancingToMapping() throws Exception {
        when(extractionService.scanAndExtract(
                any(),
                eq("faculty-vitae.pdf")
        )).thenReturn(new ExtractedCvText(
                "application/pdf",
                "Extracted CV text",
                Map.of()
        ));

        service.process(JOB_ID);

        InOrder order = inOrder(storage, extractionService, job);
        order.verify(storage).copyTo(
                eq(upload.getTemporaryStorageKey()),
                any(OutputStream.class),
                eq(1024L)
        );
        order.verify(job).advanceTo(ProcessingStage.EXTRACTING_TEXT);
        order.verify(extractionService).scanAndExtract(
                any(),
                eq("faculty-vitae.pdf")
        );
        order.verify(job).advanceTo(ProcessingStage.STRUCTURING_WITH_AI);

        ArgumentCaptor<ExtractionRun> runCaptor =
                ArgumentCaptor.forClass(ExtractionRun.class);
        verify(runRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus())
                .isEqualTo(ExtractionRunStatus.SUCCEEDED);
        assertThat(runCaptor.getValue().getExtractedText())
                .isEqualTo("Extracted CV text");
    }

    @Test
    void ignoresDuplicateDeliveryAfterExtractionSucceeded() throws Exception {
        when(job.hasReachedStructuredMapping()).thenReturn(true);

        service.process(JOB_ID);

        verify(storage, never()).copyTo(any(), any(), anyLong());
        verify(runRepository, never()).save(any());
    }

    @Test
    void infectedDocumentFailsWithoutCallingParserAgain() {
        when(extractionService.scanAndExtract(any(), any()))
                .thenThrow(new MalwareDetectedException("Test.Signature"));

        service.process(JOB_ID);

        ArgumentCaptor<ExtractionRun> runCaptor =
                ArgumentCaptor.forClass(ExtractionRun.class);
        verify(runRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus())
                .isEqualTo(ExtractionRunStatus.FAILED);
        assertThat(runCaptor.getValue().getErrorMessage())
                .isEqualTo("The uploaded CV was rejected by malware scanning.");
        verify(job).fail(
                any(Instant.class),
                eq("The uploaded CV was rejected by malware scanning.")
        );
    }

    @Test
    void parserOutageIsRetryable() {
        when(extractionService.scanAndExtract(any(), any()))
                .thenThrow(new CvTextExtractionException(
                        CvTextExtractionFailure.WORKER_UNAVAILABLE,
                        "private worker detail"
                ));

        assertThatThrownBy(() -> service.process(JOB_ID))
                .isInstanceOf(CvProcessingRetryableException.class)
                .hasMessage("The isolated parser is unavailable");

        verify(job, never()).fail(any(), any());
    }

    @Test
    void integrityMismatchFailsClosedBeforeScanning() {
        when(upload.getFileSha256()).thenReturn("0".repeat(64));

        service.process(JOB_ID);

        verify(extractionService, never()).scanAndExtract(any(), any());
        verify(job).fail(
                any(Instant.class),
                eq("The uploaded CV could not be verified.")
        );
    }

    private static String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content)
        );
    }
}

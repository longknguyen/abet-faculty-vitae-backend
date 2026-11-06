package edu.virginia.cs.abetvitae.extraction.messaging;

import edu.virginia.cs.abetvitae.extraction.model.CvUpload;
import edu.virginia.cs.abetvitae.accreditation.model.FacultyCycleRecord;
import edu.virginia.cs.abetvitae.accreditation.repository.FacultyCycleRecordRepository;
import edu.virginia.cs.abetvitae.extraction.mapping.CvMappingException;
import edu.virginia.cs.abetvitae.extraction.mapping.CvStructuredMapper;
import edu.virginia.cs.abetvitae.extraction.mapping.MappedReviewSection;
import edu.virginia.cs.abetvitae.extraction.mapping.StructuredCvDraft;
import edu.virginia.cs.abetvitae.extraction.mapping.StructuredDraftPersistenceService;
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
import edu.virginia.cs.abetvitae.extraction.scanning.MalwareScanFailure;
import edu.virginia.cs.abetvitae.extraction.scanning.MalwareScanningException;
import edu.virginia.cs.abetvitae.extraction.service.CvUploadProperties;
import edu.virginia.cs.abetvitae.extraction.service.SecureCvTextExtractionService;
import edu.virginia.cs.abetvitae.extraction.storage.PrivateCvStorage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Runs one idempotent, fail-closed scan and text-extraction transaction. */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.cv-processing.enabled",
        havingValue = "true"
)
class CvProcessingService {

    private static final String INTEGRITY_FAILURE =
            "The uploaded CV could not be verified.";
    private static final String MALWARE_FAILURE =
            "The uploaded CV was rejected by malware scanning.";
    private static final String SIZE_FAILURE =
            "The uploaded CV exceeds the processing size limit.";
    private static final String FORMAT_FAILURE =
            "The uploaded CV format is not supported.";
    private static final String ENCRYPTED_FAILURE =
            "Password-protected CVs are not supported.";
    private static final String TEXT_LIMIT_FAILURE =
            "The uploaded CV contains too much text to process.";
    private static final String NO_TEXT_FAILURE =
            "No readable text was found in the uploaded CV.";
    private static final String MALFORMED_FAILURE =
            "The uploaded CV could not be read safely.";
    private static final String MAPPING_FAILURE =
            "The uploaded CV could not be organized automatically.";
    private static final String PARSER_NAME = "isolated-parser-worker";

    private final ProcessingJobRepository processingJobRepository;
    private final ExtractionRunRepository extractionRunRepository;
    private final FacultyCycleRecordRepository facultyCycleRecordRepository;
    private final PrivateCvStorage privateCvStorage;
    private final SecureCvTextExtractionService extractionService;
    private final CvStructuredMapper structuredMapper;
    private final StructuredDraftPersistenceService draftPersistenceService;
    private final CvUploadProperties uploadProperties;

    @Transactional
    public void process(UUID processingJobId) {
        ProcessingJob job = processingJobRepository
                .findByIdForUpdate(processingJobId)
                .orElse(null);
        if (job == null || processingAlreadyCompleted(job)) {
            return;
        }

        Instant startedAt = Instant.now();
        job.start(startedAt);
        ExtractionRun run = ExtractionRun.start(
                job,
                nextRunNumber(job),
                job.getCvUpload().getExpiresAt()
        );
        extractionRunRepository.save(run);

        Path stagedFile = null;
        try {
            stagedFile = stageAndVerify(job.getCvUpload());
            job.advanceTo(ProcessingStage.EXTRACTING_TEXT);
            ExtractedCvText extracted = extractionService.scanAndExtract(
                    stagedFile,
                    job.getCvUpload().getOriginalFilename()
            );
            Instant completedAt = Instant.now();
            run.completeTextExtraction(extracted.text(), completedAt);
            job.advanceTo(ProcessingStage.STRUCTURING_WITH_AI);
            StructuredCvDraft draft = structuredMapper.map(extracted);
            FacultyCycleRecord facultyCycleRecord = lockFacultyCycleRecord(
                    job.getCvUpload()
            );
            job.advanceTo(ProcessingStage.CREATING_REVIEW);
            draftPersistenceService.createDraft(
                    run,
                    facultyCycleRecord,
                    job.getCvUpload().getUploadedByUser(),
                    draft
            );
            run.recordMapping(
                    structuredOutput(draft),
                    PARSER_NAME,
                    extracted.detectedMediaType(),
                    draft.provenance().mapperName(),
                    draft.provenance().mapperVersion(),
                    draft.provenance().modelProvider(),
                    draft.provenance().modelName(),
                    Instant.now()
            );
            job.complete(Instant.now());
        } catch (MalwareDetectedException exception) {
            failPermanently(job, run, MALWARE_FAILURE);
        } catch (CvUploadIntegrityException exception) {
            failPermanently(job, run, INTEGRITY_FAILURE);
        } catch (MalwareScanningException exception) {
            handleScanningFailure(job, run, exception);
        } catch (CvTextExtractionException exception) {
            handleExtractionFailure(job, run, exception);
        } catch (CvMappingException exception) {
            failPermanently(job, run, MAPPING_FAILURE);
        } catch (IOException exception) {
            throw retryable("Private CV storage is unavailable", exception);
        } finally {
            deleteStagedFile(stagedFile);
        }
    }

    private FacultyCycleRecord lockFacultyCycleRecord(CvUpload upload) {
        UUID facultyCycleRecordId = upload.getFacultyCycleRecord().getId();
        return facultyCycleRecordRepository
                .findByIdForUpdate(facultyCycleRecordId)
                .orElseThrow(CvUploadIntegrityException::new);
    }

    private static ObjectNode structuredOutput(StructuredCvDraft draft) {
        ObjectNode output = JsonNodeFactory.instance.objectNode();
        for (MappedReviewSection section : draft.sections()) {
            ArrayNode entries = output.putArray(section.sectionType().name());
            section.entries().forEach(entries::add);
        }
        return output;
    }

    private boolean processingAlreadyCompleted(ProcessingJob job) {
        return job.hasReachedStructuredMapping()
                || extractionRunRepository.existsByProcessingJobIdAndStatus(
                        job.getId(),
                        ExtractionRunStatus.SUCCEEDED
                );
    }

    private int nextRunNumber(ProcessingJob job) {
        return extractionRunRepository
                .findFirstByProcessingJobIdOrderByRunNumberDesc(job.getId())
                .map(previous -> previous.getRunNumber() + 1)
                .orElse(1);
    }

    private Path stageAndVerify(CvUpload upload) throws IOException {
        Path stagedFile = createOwnerOnlyTemporaryFile();
        MessageDigest digest = sha256();
        long copiedBytes;
        try (OutputStream fileOutput = Files.newOutputStream(
                stagedFile,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        ); DigestOutputStream digestOutput = new DigestOutputStream(
                fileOutput,
                digest
        )) {
            copiedBytes = privateCvStorage.copyTo(
                    upload.getTemporaryStorageKey(),
                    digestOutput,
                    uploadProperties.maximumFileSizeBytes()
            );
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(stagedFile);
            throw exception;
        }

        String copiedSha256 = HexFormat.of().formatHex(digest.digest());
        if (copiedBytes != upload.getFileSizeBytes()
                || !copiedSha256.equals(upload.getFileSha256())) {
            Files.deleteIfExists(stagedFile);
            throw new CvUploadIntegrityException();
        }
        return stagedFile;
    }

    private static Path createOwnerOnlyTemporaryFile() throws IOException {
        try {
            return Files.createTempFile(
                    "cv-processing-",
                    ".upload",
                    PosixFilePermissions.asFileAttribute(
                            PosixFilePermissions.fromString("rw-------")
                    )
            );
        } catch (UnsupportedOperationException exception) {
            throw new IOException(
                    "CV processing requires owner-only temporary files",
                    exception
            );
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void deleteStagedFile(Path stagedFile) {
        if (stagedFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(stagedFile);
        } catch (IOException exception) {
            throw retryable("Temporary CV cleanup failed", exception);
        }
    }

    private static void handleScanningFailure(
            ProcessingJob job,
            ExtractionRun run,
            MalwareScanningException exception
    ) {
        if (exception.getFailure() == MalwareScanFailure.FILE_TOO_LARGE) {
            failPermanently(job, run, SIZE_FAILURE);
        } else if (exception.getFailure() == MalwareScanFailure.INVALID_SOURCE) {
            failPermanently(job, run, INTEGRITY_FAILURE);
        } else {
            throw retryable("Malware scanning is unavailable", exception);
        }
    }

    private static void handleExtractionFailure(
            ProcessingJob job,
            ExtractionRun run,
            CvTextExtractionException exception
    ) {
        String safeFailure = permanentExtractionFailure(exception.getFailure());
        if (safeFailure == null) {
            throw retryable("The isolated parser is unavailable", exception);
        }
        failPermanently(job, run, safeFailure);
    }

    private static String permanentExtractionFailure(
            CvTextExtractionFailure failure
    ) {
        return switch (failure) {
            case FILE_TOO_LARGE -> SIZE_FAILURE;
            case UNSUPPORTED_EXTENSION, UNSUPPORTED_MEDIA_TYPE,
                    TYPE_MISMATCH -> FORMAT_FAILURE;
            case ENCRYPTED_DOCUMENT -> ENCRYPTED_FAILURE;
            case TEXT_LIMIT_EXCEEDED -> TEXT_LIMIT_FAILURE;
            case NO_TEXT -> NO_TEXT_FAILURE;
            case INVALID_SOURCE, MALFORMED_OR_SUSPICIOUS_DOCUMENT ->
                    MALFORMED_FAILURE;
            case IO_FAILURE, WORKER_UNAVAILABLE, WORKER_TIMEOUT,
                    INVALID_WORKER_RESPONSE -> null;
        };
    }

    private static void failPermanently(
            ProcessingJob job,
            ExtractionRun run,
            String safeFailure
    ) {
        Instant now = Instant.now();
        run.fail(safeFailure, now);
        job.fail(now, safeFailure);
    }

    private static CvProcessingRetryableException retryable(
            String message,
            Throwable cause
    ) {
        return new CvProcessingRetryableException(message, cause);
    }
}

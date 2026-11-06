package edu.virginia.cs.abetvitae.extraction.service;

import edu.virginia.cs.abetvitae.extraction.model.ProcessingJobStatus;
import java.time.Instant;
import java.util.UUID;

/** Upload metadata and the durable job created in the same transaction. */
public record CvUploadReceipt(
        UUID uploadId,
        UUID processingJobId,
        ProcessingJobStatus processingStatus,
        String originalFilename,
        String detectedMimeType,
        long fileSizeBytes,
        Instant uploadedAt) {
}

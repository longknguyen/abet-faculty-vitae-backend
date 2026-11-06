package edu.virginia.cs.abetvitae.extraction.controller;

import edu.virginia.cs.abetvitae.extraction.model.ProcessingJobStatus;
import edu.virginia.cs.abetvitae.extraction.service.CvUploadReceipt;
import edu.virginia.cs.abetvitae.extraction.service.CvUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/cv-uploads")
@RequiredArgsConstructor
public class CvUploadController {

    private final CvUploadService cvUploadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CvUploadResponse> uploadCv(
            Authentication authentication,
            @RequestPart("file") MultipartFile file
    ) {
        CvUploadReceipt receipt = cvUploadService.uploadCv(
                authentication,
                file
        );
        CvUploadResponse response = new CvUploadResponse(
                receipt.uploadId(),
                receipt.processingJobId(),
                receipt.processingStatus(),
                receipt.originalFilename(),
                receipt.detectedMimeType(),
                receipt.fileSizeBytes(),
                receipt.uploadedAt()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    public record CvUploadResponse(
            UUID id,
            UUID processingJobId,
            ProcessingJobStatus processingStatus,
            String originalFilename,
            String detectedMimeType,
            long fileSizeBytes,
            java.time.Instant uploadedAt
    ) {
    }
}

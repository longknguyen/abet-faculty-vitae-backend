package edu.virginia.cs.abetvitae.extraction.service;

import edu.virginia.cs.abetvitae.account.model.UserAccount;
import edu.virginia.cs.abetvitae.accreditation.model.FacultyCycleRecord;
import edu.virginia.cs.abetvitae.extraction.model.CvUpload;
import edu.virginia.cs.abetvitae.extraction.model.ProcessingJob;
import edu.virginia.cs.abetvitae.extraction.repository.CvUploadRepository;
import edu.virginia.cs.abetvitae.extraction.repository.ProcessingJobRepository;
import edu.virginia.cs.abetvitae.extraction.storage.StoredCvFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CvUploadPersistenceService {

    private final CvUploadRepository cvUploadRepository;
    private final ProcessingJobRepository processingJobRepository;

    @Transactional
    public CvUpload saveUploadAndQueueJob(
            FacultyCycleRecord facultyCycleRecord,
            UserAccount uploadedByUser,
            String originalFilename,
            String detectedMimeType,
            StoredCvFile storedFile,
            Instant expiresAt
    ) {
        CvUpload upload = CvUpload.create(
                facultyCycleRecord,
                uploadedByUser,
                originalFilename,
                storedFile.storageKey(),
                detectedMimeType,
                storedFile.fileSizeBytes(),
                storedFile.sha256(),
                expiresAt
        );

        CvUpload savedUpload = cvUploadRepository.save(upload);

        ProcessingJob job = ProcessingJob.create(savedUpload);
        processingJobRepository.save(job);

        return savedUpload;
    }
}
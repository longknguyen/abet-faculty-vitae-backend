package edu.virginia.cs.abetvitae.extraction.service;

import edu.virginia.cs.abetvitae.account.security.AuthenticatedUser;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUserService;
import edu.virginia.cs.abetvitae.extraction.model.ProcessingJob;
import edu.virginia.cs.abetvitae.extraction.repository.ProcessingJobRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CvProcessingStatusService {

    private final AuthenticatedUserService authenticatedUserService;
    private final ProcessingJobRepository processingJobRepository;

    @Transactional(readOnly = true)
    public CvProcessingStatus getStatus(
            Authentication authentication,
            UUID processingJobId
    ) {
        AuthenticatedUser authenticatedUser =
                authenticatedUserService.requireUser(authentication);
        ProcessingJob job = processingJobRepository
                .findWithUploadAndOwnerById(processingJobId)
                .filter(candidate -> candidate.getCvUpload()
                        .getUploadedByUser()
                        .getId()
                        .equals(authenticatedUser.accountId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "CV processing job was not found"
                ));
        return new CvProcessingStatus(
                job.getId(),
                job.getStatus(),
                job.getCurrentStage(),
                job.getErrorMessage(),
                job.getQueuedAt(),
                job.getStartedAt(),
                job.getCompletedAt()
        );
    }
}

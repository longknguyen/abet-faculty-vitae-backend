package edu.virginia.cs.abetvitae.extraction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.virginia.cs.abetvitae.account.model.RoleName;
import edu.virginia.cs.abetvitae.account.model.UserAccount;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUser;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUserService;
import edu.virginia.cs.abetvitae.extraction.model.CvUpload;
import edu.virginia.cs.abetvitae.extraction.model.ProcessingJob;
import edu.virginia.cs.abetvitae.extraction.model.ProcessingJobStatus;
import edu.virginia.cs.abetvitae.extraction.repository.ProcessingJobRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

class CvProcessingStatusServiceTest {

    @Test
    void returnsStatusOnlyToUploadOwner() {
        AuthenticatedUserService authenticatedUserService =
                mock(AuthenticatedUserService.class);
        ProcessingJobRepository repository =
                mock(ProcessingJobRepository.class);
        Authentication authentication = mock(Authentication.class);
        UUID accountId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(
                accountId,
                "Faculty User",
                "User",
                Set.of(RoleName.FACULTY)
        );
        ProcessingJob job = jobOwnedBy(accountId);
        job.setId(jobId);
        when(authenticatedUserService.requireUser(authentication))
                .thenReturn(user);
        when(repository.findWithUploadAndOwnerById(jobId))
                .thenReturn(Optional.of(job));
        CvProcessingStatusService service = new CvProcessingStatusService(
                authenticatedUserService,
                repository
        );

        CvProcessingStatus status = service.getStatus(authentication, jobId);

        assertThat(status.processingJobId()).isEqualTo(jobId);
        assertThat(status.status()).isEqualTo(ProcessingJobStatus.QUEUED);
    }

    @Test
    void hidesAnotherFacultyMembersJob() {
        AuthenticatedUserService authenticatedUserService =
                mock(AuthenticatedUserService.class);
        ProcessingJobRepository repository =
                mock(ProcessingJobRepository.class);
        Authentication authentication = mock(Authentication.class);
        UUID accountId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(
                accountId,
                "Faculty User",
                "User",
                Set.of(RoleName.FACULTY)
        );
        when(authenticatedUserService.requireUser(authentication))
                .thenReturn(user);
        ProcessingJob anotherFacultyJob = jobOwnedBy(UUID.randomUUID());
        when(repository.findWithUploadAndOwnerById(jobId))
                .thenReturn(Optional.of(anotherFacultyJob));
        CvProcessingStatusService service = new CvProcessingStatusService(
                authenticatedUserService,
                repository
        );

        assertThatThrownBy(() -> service.getStatus(authentication, jobId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    private static ProcessingJob jobOwnedBy(UUID accountId) {
        UserAccount owner = mock(UserAccount.class);
        when(owner.getId()).thenReturn(accountId);
        CvUpload upload = mock(CvUpload.class);
        when(upload.getUploadedByUser()).thenReturn(owner);
        return ProcessingJob.create(upload);
    }
}

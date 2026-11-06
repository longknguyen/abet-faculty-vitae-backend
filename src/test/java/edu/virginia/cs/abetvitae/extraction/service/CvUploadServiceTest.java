package edu.virginia.cs.abetvitae.extraction.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.virginia.cs.abetvitae.account.model.RoleName;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUser;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUserService;
import edu.virginia.cs.abetvitae.account.repository.UserAccountRepository;
import edu.virginia.cs.abetvitae.accreditation.repository.AbetTemplateRepository;
import edu.virginia.cs.abetvitae.accreditation.repository.FacultyCycleRecordRepository;
import edu.virginia.cs.abetvitae.extraction.storage.PrivateCvStorage;
import edu.virginia.cs.abetvitae.faculty.repository.ProfessorRepository;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

class CvUploadServiceTest {

    @Test
    void rejectsAdminWhenUploadingCv() throws Exception {
        AuthenticatedUserService authenticatedUserService =
                mock(AuthenticatedUserService.class);
        UserAccountRepository userAccountRepository =
                mock(UserAccountRepository.class);
        ProfessorRepository professorRepository =
                mock(ProfessorRepository.class);
        AbetTemplateRepository abetTemplateRepository =
                mock(AbetTemplateRepository.class);
        FacultyCycleRecordRepository facultyCycleRecordRepository =
                mock(FacultyCycleRecordRepository.class);
        PrivateCvStorage privateCvStorage =
                mock(PrivateCvStorage.class);
        CvUploadPersistenceService persistenceService =
                mock(CvUploadPersistenceService.class);

        Authentication authentication = mock(Authentication.class);
        MultipartFile file = mock(MultipartFile.class);

        AuthenticatedUser admin = new AuthenticatedUser(
                UUID.randomUUID(),
                "Admin User",
                "User",
                Set.of(RoleName.ADMIN)
        );

        when(authenticatedUserService.requireUser(authentication))
                .thenReturn(admin);

        CvUploadService service = new CvUploadService(
                authenticatedUserService,
                userAccountRepository,
                professorRepository,
                abetTemplateRepository,
                facultyCycleRecordRepository,
                privateCvStorage,
                persistenceService,
                new CvDocumentInspector(new CvUploadProperties(
                        10 * 1024 * 1024,
                        Duration.ofDays(90)
                )),
                new CvUploadProperties(
                        10 * 1024 * 1024,
                        Duration.ofDays(90)
                )
        );

        assertThatThrownBy(() -> service.uploadCv(authentication, file))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only faculty members can upload CVs.");

        verify(userAccountRepository, never()).findById(admin.accountId());
        verify(privateCvStorage, never()).store(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(persistenceService, never()).saveUploadAndQueueJob(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}


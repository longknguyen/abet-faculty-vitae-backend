package edu.virginia.cs.abetvitae.extraction.service;
import edu.virginia.cs.abetvitae.account.model.RoleName;
import edu.virginia.cs.abetvitae.account.model.UserAccount;
import edu.virginia.cs.abetvitae.account.repository.UserAccountRepository;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUser;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUserService;
import edu.virginia.cs.abetvitae.accreditation.model.AbetTemplate;
import edu.virginia.cs.abetvitae.accreditation.model.AbetTemplateStatus;
import edu.virginia.cs.abetvitae.accreditation.model.FacultyCycleRecord;
import edu.virginia.cs.abetvitae.accreditation.repository.AbetTemplateRepository;
import edu.virginia.cs.abetvitae.accreditation.repository.FacultyCycleRecordRepository;
import edu.virginia.cs.abetvitae.extraction.storage.PrivateCvStorage;
import edu.virginia.cs.abetvitae.extraction.storage.StoredCvFile;
import edu.virginia.cs.abetvitae.faculty.model.Professor;
import edu.virginia.cs.abetvitae.faculty.repository.ProfessorRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
class CvUploadServiceTest {

    @Test
    void rejectsAdminWhenUploadingCv() {
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
                persistenceService
        );

        assertThatThrownBy(() -> service.uploadCv(authentication, file))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only faculty members can upload CVs.");

        verify(userAccountRepository, never()).findById(admin.accountId());
        verifyNoInteractions(privateCvStorage, persistenceService);
    }

    @Test
    void rejectsEmptyFile() {
        TestSetup setup = createFacultyService();
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(true);
        when(file.getSize()).thenReturn(0L);

        assertThatThrownBy(() -> setup.service().uploadCv(
                setup.authentication(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Please select a file to upload.");

        verifyNoInteractions(setup.storage(), setup.persistenceService());
    }

    @Test
    void rejectsFileLargerThan10MB() {
        TestSetup setup = createFacultyService();
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(10L * 1024 * 1024 + 1);

        assertThatThrownBy(() -> setup.service().uploadCv(
                setup.authentication(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The uploaded file exceeds the 10 MB limit.");

        verifyNoInteractions(setup.storage(), setup.persistenceService());
    }

    private TestSetup createFacultyService() {
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
        PrivateCvStorage storage = mock(PrivateCvStorage.class);
        CvUploadPersistenceService persistenceService =
                mock(CvUploadPersistenceService.class);

        Authentication authentication = mock(Authentication.class);

        UUID accountId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();

        AuthenticatedUser faculty = new AuthenticatedUser(
                accountId,
                "Faculty User",
                "User",
                Set.of(RoleName.FACULTY)
        );

        UserAccount account = mock(UserAccount.class);
        when(account.getId()).thenReturn(accountId);

        Professor professor = mock(Professor.class);
        when(professor.getId()).thenReturn(professorId);

        AbetTemplate template = mock(AbetTemplate.class);
        when(template.getId()).thenReturn(templateId);

        FacultyCycleRecord record = mock(FacultyCycleRecord.class);

        when(authenticatedUserService.requireUser(authentication))
                .thenReturn(faculty);
        when(userAccountRepository.findById(accountId))
                .thenReturn(Optional.of(account));
        when(professorRepository.findByUserAccountId(accountId))
                .thenReturn(Optional.of(professor));
        when(abetTemplateRepository
                .findFirstByStatusOrderByPublishedAtDescRevisionNumberDesc(
                        AbetTemplateStatus.PUBLISHED))
                .thenReturn(Optional.of(template));
        when(facultyCycleRecordRepository
                .findByProfessorIdAndAbetTemplateId(professorId, templateId))
                .thenReturn(Optional.of(record));

        CvUploadService service = new CvUploadService(
                authenticatedUserService,
                userAccountRepository,
                professorRepository,
                abetTemplateRepository,
                facultyCycleRecordRepository,
                storage,
                persistenceService
        );

        return new TestSetup(
                service,
                authentication,
                storage,
                persistenceService,
                facultyCycleRecordRepository,
                professorId,
                templateId,
                account,
                record
        );
    }

    @Test
    void rejectsFileWithInvalidPdfContent() throws IOException {
        TestSetup setup = createFacultyService();
        MultipartFile file = mock(MultipartFile.class);

        byte[] content = "This is not a PDF document."
                .getBytes(StandardCharsets.UTF_8);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn((long) content.length);
        when(file.getOriginalFilename()).thenReturn("fake.pdf");
        when(file.getInputStream())
                .thenReturn(new ByteArrayInputStream(content));

        assertThatThrownBy(() -> setup.service().uploadCv(
                setup.authentication(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The uploaded file does not appear to be a valid PDF.");

        verifyNoInteractions(setup.storage(), setup.persistenceService());
    }

    @Test
    void rejectsUnsupportedFileExtension() {
        TestSetup setup = createFacultyService();
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100L);
        when(file.getOriginalFilename()).thenReturn("notPDFFile.exe");

        assertThatThrownBy(() -> setup.service().uploadCv(
                setup.authentication(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only PDF and DOCX files are allowed.");

        verifyNoInteractions(setup.storage(), setup.persistenceService());
    }

    @Test
    void deletesStoredFileWhenDatabaseSaveFails() throws Exception {
        TestSetup setup = createFacultyService();

        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100L);
        when(file.getOriginalFilename()).thenReturn("resume.pdf");

        when(file.getInputStream()).thenReturn(
                new ByteArrayInputStream("%PDF-1.4".getBytes(StandardCharsets.UTF_8)));

        StoredCvFile storedFile = new StoredCvFile(
                "private/cv-123",
                100L,
                "abc123"
        );

        when(setup.storage().store(any(), anyLong(), anyString()))
                .thenReturn(storedFile);

        when(setup.persistenceService().saveUploadAndQueueJob(
                any(), any(), anyString(), anyString(), any(), any()))
                .thenThrow(new RuntimeException("Database save failed"));

        assertThatThrownBy(() -> setup.service().uploadCv(
                setup.authentication(), file))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database save failed");

        verify(setup.storage()).delete("private/cv-123");
    }

    @Test
    void savesUploadToAuthenticatedFacultyMembersCycle() throws Exception {
        TestSetup setup = createFacultyService();

        MultipartFile file = mock(MultipartFile.class);
        byte[] content = "%PDF-1.4".getBytes(StandardCharsets.UTF_8);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn((long) content.length);
        when(file.getOriginalFilename()).thenReturn("resume.pdf");
        when(file.getInputStream())
                .thenReturn(new ByteArrayInputStream(content));

        StoredCvFile storedFile = new StoredCvFile(
                "private/cv-ownership-test",
                content.length,
                "abc123"
        );

        when(setup.storage().store(any(), anyLong(), anyString()))
                .thenReturn(storedFile);

        setup.service().uploadCv(setup.authentication(), file);

        verify(setup.facultyCycleRecordRepository())
                .findByProfessorIdAndAbetTemplateId(
                        setup.professorId(),
                        setup.templateId()
                );

        verify(setup.persistenceService()).saveUploadAndQueueJob(
                eq(setup.facultyCycleRecord()),
                eq(setup.account()),
                eq("resume.pdf"),
                eq("application/pdf"),
                eq(storedFile),
                any()
        );
    }
    
    private record TestSetup(
            CvUploadService service,
            Authentication authentication,
            PrivateCvStorage storage,
            CvUploadPersistenceService persistenceService,
            FacultyCycleRecordRepository facultyCycleRecordRepository,
            UUID professorId,
            UUID templateId,
            UserAccount account,
            FacultyCycleRecord facultyCycleRecord
    ) {
    }
}


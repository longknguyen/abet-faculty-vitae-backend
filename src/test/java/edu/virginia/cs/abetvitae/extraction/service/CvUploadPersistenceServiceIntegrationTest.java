
package edu.virginia.cs.abetvitae.extraction.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import edu.virginia.cs.abetvitae.TestcontainersConfiguration;
import edu.virginia.cs.abetvitae.account.model.UserAccount;
import edu.virginia.cs.abetvitae.account.repository.UserAccountRepository;
import edu.virginia.cs.abetvitae.accreditation.model.AbetTemplate;
import edu.virginia.cs.abetvitae.accreditation.model.FacultyCycleRecord;
import edu.virginia.cs.abetvitae.accreditation.repository.AbetTemplateRepository;
import edu.virginia.cs.abetvitae.accreditation.repository.FacultyCycleRecordRepository;
import edu.virginia.cs.abetvitae.extraction.model.CvUpload;
import edu.virginia.cs.abetvitae.extraction.model.ProcessingJob;
import edu.virginia.cs.abetvitae.extraction.model.ProcessingJobStatus;
import edu.virginia.cs.abetvitae.extraction.repository.CvUploadRepository;
import edu.virginia.cs.abetvitae.extraction.repository.ProcessingJobRepository;
import edu.virginia.cs.abetvitae.extraction.storage.StoredCvFile;
import edu.virginia.cs.abetvitae.faculty.model.Professor;
import edu.virginia.cs.abetvitae.faculty.repository.ProfessorRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
class CvUploadPersistenceServiceIntegrationTest {

    @Autowired
    private CvUploadPersistenceService persistenceService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private AbetTemplateRepository abetTemplateRepository;

    @Autowired
    private FacultyCycleRecordRepository facultyCycleRecordRepository;

    @Autowired
    private CvUploadRepository cvUploadRepository;

    @MockitoSpyBean
    private ProcessingJobRepository processingJobRepository;

    @Test
    @Transactional
    void savesUploadAndQueuesProcessingJob() {
        FacultyCycleRecord record = createFacultyCycleRecord();

        StoredCvFile storedFile = new StoredCvFile(
                UUID.randomUUID().toString(),
                1024,
                "a".repeat(64)
        );

        Instant expiresAt = Instant.now().plusSeconds(90L * 24 * 60 * 60);

        CvUpload savedUpload = persistenceService.saveUploadAndQueueJob(
                record,
                record.getProfessor().getUserAccount(),
                "resume.pdf",
                "application/pdf",
                storedFile,
                expiresAt
        );

        CvUpload persistedUpload = cvUploadRepository.findById(savedUpload.getId())
                .orElseThrow();

        ProcessingJob persistedJob = processingJobRepository
                .findByCvUploadId(savedUpload.getId())
                .orElseThrow();

        assertEquals("resume.pdf", persistedUpload.getOriginalFilename());
        assertEquals(
                storedFile.storageKey(),
                persistedUpload.getTemporaryStorageKey()
        );
        assertEquals(expiresAt, persistedUpload.getExpiresAt());

        assertEquals(savedUpload.getId(), persistedJob.getCvUpload().getId());
        assertEquals(ProcessingJobStatus.QUEUED, persistedJob.getStatus());
    }

    @Test
    void rollsBackUploadWhenProcessingJobSaveFails() {
        FacultyCycleRecord record = createFacultyCycleRecord();

        StoredCvFile storedFile = new StoredCvFile(
                UUID.randomUUID().toString(),
                1024,
                "b".repeat(64)
        );

        Instant expiresAt = Instant.now().plusSeconds(90L * 24 * 60 * 60);
        long uploadsBefore = cvUploadRepository.count();

        doAnswer(invocation -> {
            cvUploadRepository.flush();
            throw new IllegalStateException(
                    "Simulated processing job save failure"
            );
        }).when(processingJobRepository).save(any(ProcessingJob.class));

        assertThrows(
                DataAccessException.class,
                () -> persistenceService.saveUploadAndQueueJob(
                        record,
                        record.getProfessor().getUserAccount(),
                        "resume.pdf",
                        "application/pdf",
                        storedFile,
                        expiresAt
                )
        );

        assertEquals(uploadsBefore, cvUploadRepository.count());
    }

    private FacultyCycleRecord createFacultyCycleRecord() {
        String uniqueId = UUID.randomUUID().toString();

        UserAccount account = userAccountRepository.save(
                UserAccount.fromNetBadgeIdentity(
                        "cv-upload-test-" + uniqueId + "@example.com",
                        "cv-upload-test-" + uniqueId
                )
        );

        Professor professor = professorRepository.save(
                Professor.createForAccount(
                        account,
                        "Test",
                        null,
                        "Professor",
                        null,
                        "Test Professor"
                )
        );

        AbetTemplate template = abetTemplateRepository.save(
                AbetTemplate.createDraft(
                        "2026-" + uniqueId,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        LocalDate.of(2026, 11, 30),
                        "Computer Science",
                        1,
                        JsonNodeFactory.instance.arrayNode(),
                        JsonNodeFactory.instance.objectNode(),
                        "test-template"
                )
        );

        return facultyCycleRecordRepository.save(
                FacultyCycleRecord.create(professor, template)
        );
    }
}

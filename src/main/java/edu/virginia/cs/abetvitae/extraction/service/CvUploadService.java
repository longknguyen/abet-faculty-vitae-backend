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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CvUploadService {

    private final AuthenticatedUserService authenticatedUserService;
    private final UserAccountRepository userAccountRepository;
    private final ProfessorRepository professorRepository;
    private final AbetTemplateRepository abetTemplateRepository;
    private final FacultyCycleRecordRepository facultyCycleRecordRepository;
    private final PrivateCvStorage privateCvStorage;
    private final CvUploadPersistenceService cvUploadPersistenceService;
    private final CvDocumentInspector documentInspector;
    private final CvUploadProperties uploadProperties;

    public CvUploadReceipt uploadCv(
            Authentication authentication,
            MultipartFile file
    ) {
        // 1. Verify the authenticated faculty member.
        UserAccount account = requireFacultyAccount(authentication);
        Professor professor = requireProfessor(account);

        // 2. Find the current ABET review cycle and faculty record.
        AbetTemplate template = requirePublishedTemplate();
        FacultyCycleRecord facultyCycleRecord =
                requireFacultyCycleRecord(professor, template);

        // 3. Perform bounded screening without parsing the untrusted document.
        InspectedCvDocument inspectedDocument = documentInspector.inspect(file);

        // 4. Store the file privately.
        StoredCvFile storedFile = storeFile(
                file,
                inspectedDocument.mediaType()
        );

        // 5. Save the upload and processing job together.
        try {
            return cvUploadPersistenceService.saveUploadAndQueueJob(
                    facultyCycleRecord,
                    account,
                    inspectedDocument.filename(),
                    inspectedDocument.mediaType(),
                    storedFile,
                    calculateExpirationDate()
            );
        } catch (RuntimeException exception) {
            // Remove the stored file if the database transaction fails.
            try {
                privateCvStorage.delete(storedFile.storageKey());
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }

            throw exception;
        }
    }

    private UserAccount requireFacultyAccount(
            Authentication authentication
    ) {
        AuthenticatedUser user =
                authenticatedUserService.requireUser(authentication);

        if (!user.hasRole(RoleName.FACULTY)) {
            throw new AccessDeniedException(
                    "Only faculty members can upload CVs."
            );
        }

        return userAccountRepository.findById(user.accountId())
                .orElseThrow(() -> new IllegalStateException(
                        "The authenticated user account could not be found."
                ));
    }

    private Professor requireProfessor(UserAccount account) {
        return professorRepository.findByUserAccountId(account.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "No faculty profile is associated with this account."
                ));
    }

    private AbetTemplate requirePublishedTemplate() {
        return abetTemplateRepository
                .findFirstByStatusOrderByPublishedAtDescRevisionNumberDesc(
                        AbetTemplateStatus.PUBLISHED
                )
                .orElseThrow(() -> new IllegalStateException(
                        "No published ABET template is available."
                ));
    }

    private FacultyCycleRecord requireFacultyCycleRecord(
            Professor professor,
            AbetTemplate template
    ) {
        return facultyCycleRecordRepository
                .findByProfessorIdAndAbetTemplateId(
                        professor.getId(),
                        template.getId()
                )
                .orElseThrow(() -> new IllegalStateException(
                        "No faculty record exists for the current ABET cycle."
                ));
    }

    private StoredCvFile storeFile(
            MultipartFile file,
            String detectedMimeType
    ) {
        try (InputStream inputStream = file.getInputStream()) {
            return privateCvStorage.store(
                    inputStream,
                    uploadProperties.maximumFileSizeBytes(),
                    detectedMimeType
            );
        } catch (IOException exception) {
            throw CvUploadRejectedException.storageUnavailable(exception);
        }
    }

    private Instant calculateExpirationDate() {
        return Instant.now().plus(uploadProperties.retentionPeriod());
    }
}

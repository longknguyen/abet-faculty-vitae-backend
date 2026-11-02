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
import edu.virginia.cs.abetvitae.extraction.model.CvUpload;
import edu.virginia.cs.abetvitae.extraction.storage.PrivateCvStorage;
import edu.virginia.cs.abetvitae.extraction.storage.StoredCvFile;
import edu.virginia.cs.abetvitae.faculty.model.Professor;
import edu.virginia.cs.abetvitae.faculty.repository.ProfessorRepository;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CvUploadService {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(".pdf", ".docx");

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final AuthenticatedUserService authenticatedUserService;
    private final UserAccountRepository userAccountRepository;
    private final ProfessorRepository professorRepository;
    private final AbetTemplateRepository abetTemplateRepository;
    private final FacultyCycleRecordRepository facultyCycleRecordRepository;
    private final PrivateCvStorage privateCvStorage;
    private final CvUploadPersistenceService cvUploadPersistenceService;

    private final Tika tika = new Tika();

    public CvUpload uploadCv(
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

        // 3. Validate the uploaded file.
        validateFileSize(file);
        String originalFilename = validateFileExtension(file);
        String detectedMimeType = detectMimeType(file);

        validateDetectedMimeType(originalFilename, detectedMimeType);

        // 4. Store the file privately.
        StoredCvFile storedFile = storeFile(file, detectedMimeType);

        // 5. Save the upload and processing job together.
        try {
            return cvUploadPersistenceService.saveUploadAndQueueJob(
                    facultyCycleRecord,
                    account,
                    originalFilename,
                    detectedMimeType,
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

    private void validateFileSize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select a file to upload."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "The uploaded file exceeds the 10 MB limit."
            );
        }
    }

    private String validateFileExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException(
                    "The uploaded file must have a filename."
            );
        }

        String normalizedFilename = filename.toLowerCase();
        int lastDot = normalizedFilename.lastIndexOf('.');

        if (lastDot < 0) {
            throw new IllegalArgumentException(
                    "Only PDF and DOCX files are allowed."
            );
        }

        String extension = normalizedFilename.substring(lastDot);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Only PDF and DOCX files are allowed."
            );
        }

        if (filename.length() > 255) {
            throw new IllegalArgumentException(
                    "The filename must be 255 characters or fewer."
            );
        }

        return filename;
    }

    private String detectMimeType(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return tika.detect(inputStream, file.getOriginalFilename());
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "Unable to inspect the uploaded file.",
                    exception
            );
        }
    }

    private void validateDetectedMimeType(
            String filename,
            String detectedMimeType
    ) {
        String normalizedFilename = filename.toLowerCase();

        if (normalizedFilename.endsWith(".pdf")
                && !"application/pdf".equals(detectedMimeType)) {
            throw new IllegalArgumentException(
                    "The uploaded file does not appear to be a valid PDF."
            );
        }

        if (normalizedFilename.endsWith(".docx")
                && !"application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                .equals(detectedMimeType)) {
            throw new IllegalArgumentException(
                    "The uploaded file does not appear to be a valid DOCX file."
            );
        }
    }

    private StoredCvFile storeFile(
            MultipartFile file,
            String detectedMimeType
    ) {
        try (InputStream inputStream = file.getInputStream()) {
            return privateCvStorage.store(
                    inputStream,
                    MAX_FILE_SIZE,
                    detectedMimeType
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to store the uploaded CV.",
                    exception
            );
        }
    }

    private Instant calculateExpirationDate() {
        return Instant.now().plus(90, ChronoUnit.DAYS);
    }
}
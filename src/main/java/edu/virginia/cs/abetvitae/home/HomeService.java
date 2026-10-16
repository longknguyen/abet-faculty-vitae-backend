package edu.virginia.cs.abetvitae.home;

import edu.virginia.cs.abetvitae.account.model.RoleName;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUser;
import edu.virginia.cs.abetvitae.accreditation.model.AbetTemplate;
import edu.virginia.cs.abetvitae.accreditation.model.AbetTemplateStatus;
import edu.virginia.cs.abetvitae.accreditation.model.FacultyCycleRecord;
import edu.virginia.cs.abetvitae.accreditation.repository.AbetTemplateRepository;
import edu.virginia.cs.abetvitae.accreditation.repository.FacultyCycleRecordRepository;
import edu.virginia.cs.abetvitae.extraction.model.CvUpload;
import edu.virginia.cs.abetvitae.extraction.model.ProcessingJob;
import edu.virginia.cs.abetvitae.extraction.repository.CvUploadRepository;
import edu.virginia.cs.abetvitae.extraction.repository.ProcessingJobRepository;
import edu.virginia.cs.abetvitae.faculty.model.Education;
import edu.virginia.cs.abetvitae.faculty.model.Experience;
import edu.virginia.cs.abetvitae.faculty.model.Professor;
import edu.virginia.cs.abetvitae.faculty.repository.EducationRepository;
import edu.virginia.cs.abetvitae.faculty.repository.ExperienceRepository;
import edu.virginia.cs.abetvitae.faculty.repository.ProfessorRepository;
import edu.virginia.cs.abetvitae.home.dto.HomeResponse;
import edu.virginia.cs.abetvitae.review.model.ExtractionReview;
import edu.virginia.cs.abetvitae.review.model.ExtractionReviewItem;
import edu.virginia.cs.abetvitae.review.model.ExtractionReviewStatus;
import edu.virginia.cs.abetvitae.review.model.ReviewSectionType;
import edu.virginia.cs.abetvitae.review.repository.ExtractionReviewItemRepository;
import edu.virginia.cs.abetvitae.review.repository.ExtractionReviewRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class HomeService {

    private static final int MAXIMUM_PREVIEW_LINES = 8;

    private final AbetTemplateRepository abetTemplateRepository;
    private final FacultyCycleRecordRepository facultyCycleRecordRepository;
    private final ProfessorRepository professorRepository;
    private final EducationRepository educationRepository;
    private final ExperienceRepository experienceRepository;
    private final CvUploadRepository cvUploadRepository;
    private final ProcessingJobRepository processingJobRepository;
    private final ExtractionReviewRepository extractionReviewRepository;
    private final ExtractionReviewItemRepository extractionReviewItemRepository;

    @Transactional(readOnly = true)
    public HomeResponse loadHome(AuthenticatedUser user) {
        AbetTemplate template = currentTemplate();
        Optional<Professor> professor = professorRepository.findByUserAccountId(user.accountId());
        FacultyCycleRecord personalRecord = personalRecord(user, professor, template);

        return new HomeResponse(
                currentUser(user, professor),
                new HomeResponse.Cycle(
                        template.getId(),
                        template.getReviewCycle(),
                        template.getDepartmentName()),
                personalRecord == null ? null : personalVitae(personalRecord),
                user.hasRole(RoleName.ADMIN) ? facultyRecords(template) : List.of());
    }

    private AbetTemplate currentTemplate() {
        return abetTemplateRepository
                .findFirstByStatusOrderByPublishedAtDescRevisionNumberDesc(
                        AbetTemplateStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "No published ABET template is configured"));
    }

    private FacultyCycleRecord personalRecord(
            AuthenticatedUser user,
            Optional<Professor> professor,
            AbetTemplate template) {
        if (!user.hasRole(RoleName.FACULTY)) {
            return null;
        }
        Professor linkedProfessor = professor.orElseThrow(() -> new ResponseStatusException(
                HttpStatus.CONFLICT,
                "The faculty account is not linked to a professor record"));
        return facultyCycleRecordRepository
                .findByProfessorIdAndAbetTemplateId(linkedProfessor.getId(), template.getId())
                .orElse(null);
    }

    private static HomeResponse.CurrentUser currentUser(
            AuthenticatedUser user,
            Optional<Professor> professor) {
        String displayName = professor.map(Professor::getDisplayName).orElse(user.displayName());
        String surname = professor.map(Professor::getFamilyName).orElse(user.surname());
        return new HomeResponse.CurrentUser(
                user.accountId(),
                displayName,
                surname,
                user.roles());
    }

    private HomeResponse.PersonalVitae personalVitae(FacultyCycleRecord record) {
        return new HomeResponse.PersonalVitae(
                record.getId(),
                record.getStatus(),
                record.getContentRevision(),
                sourceCv(record.getId()),
                nextReviewItem(record.getId()),
                previewSections(record.getProfessor().getId()));
    }

    private HomeResponse.SourceCv sourceCv(UUID facultyCycleRecordId) {
        Optional<CvUpload> upload = cvUploadRepository
                .findFirstByFacultyCycleRecordIdAndPurgedAtIsNullOrderByUploadedAtDesc(
                        facultyCycleRecordId);
        if (upload.isEmpty()) {
            return null;
        }
        Optional<ProcessingJob> job = processingJobRepository.findByCvUploadId(upload.get().getId());
        return job.map(value -> new HomeResponse.SourceCv(
                upload.get().getId(),
                upload.get().getOriginalFilename(),
                value.getStatus(),
                value.getCurrentStage())).orElse(null);
    }

    private HomeResponse.NextReviewItem nextReviewItem(UUID facultyCycleRecordId) {
        Optional<ExtractionReview> review = extractionReviewRepository
                .findFirstByExtractionRunProcessingJobCvUploadFacultyCycleRecordIdAndStatusOrderByCreatedAtDesc(
                        facultyCycleRecordId,
                        ExtractionReviewStatus.DRAFT);
        if (review.isEmpty()) {
            return null;
        }
        Optional<ExtractionReviewItem> item = extractionReviewItemRepository
                .findFirstByExtractionReviewIdOrderByDisplayOrderAsc(review.get().getId());
        return item.map(value -> new HomeResponse.NextReviewItem(
                review.get().getId(),
                value.getId(),
                value.getSectionType(),
                sectionTitle(value.getSectionType()),
                detail(value))).orElse(null);
    }

    private List<HomeResponse.PreviewSection> previewSections(UUID professorId) {
        List<HomeResponse.PreviewSection> sections = new ArrayList<>();
        List<String> education = educationRepository
                .findAllByProfessorIdOrderByDisplayOrderAsc(professorId)
                .stream()
                .map(HomeService::educationLine)
                .limit(MAXIMUM_PREVIEW_LINES)
                .toList();
        addSection(sections, "Education", education);

        List<String> experience = experienceRepository
                .findAllByProfessorIdOrderByDisplayOrderAsc(professorId)
                .stream()
                .map(HomeService::experienceLine)
                .limit(MAXIMUM_PREVIEW_LINES)
                .toList();
        addSection(sections, "Professional experience", experience);
        return List.copyOf(sections);
    }

    private List<HomeResponse.FacultyRecord> facultyRecords(AbetTemplate template) {
        Map<UUID, FacultyCycleRecord> recordsByProfessor = new HashMap<>();
        facultyCycleRecordRepository.findAllByAbetTemplateId(template.getId())
                .forEach(record -> recordsByProfessor.put(record.getProfessor().getId(), record));

        return professorRepository.findAllByOrderByFamilyNameAscGivenNameAsc()
                .stream()
                .map(professor -> facultyRecord(professor, recordsByProfessor.get(professor.getId())))
                .toList();
    }

    private static HomeResponse.FacultyRecord facultyRecord(
            Professor professor,
            FacultyCycleRecord record) {
        return new HomeResponse.FacultyRecord(
                professor.getId(),
                record == null ? null : record.getId(),
                professor.getDisplayName(),
                record == null ? null : record.getStatus(),
                record == null ? null : record.getUpdatedAt(),
                record == null ? null : record.getLockVersion());
    }

    private static void addSection(
            List<HomeResponse.PreviewSection> sections,
            String heading,
            List<String> lines) {
        if (!lines.isEmpty()) {
            sections.add(new HomeResponse.PreviewSection(heading, lines));
        }
    }

    private static String educationLine(Education education) {
        String year = education.getGraduationYear() == null
                ? ""
                : " · " + education.getGraduationYear();
        return education.getDegree() + ", " + education.getDiscipline()
                + " · " + education.getInstitution() + year;
    }

    private static String experienceLine(Experience experience) {
        String end = experience.isCurrent() || experience.getEndYear() == null
                ? "present"
                : Integer.toString(experience.getEndYear());
        return experience.getPositionTitle() + " · " + experience.getOrganisation()
                + " · " + experience.getStartYear() + "–" + end;
    }

    private static String sectionTitle(ReviewSectionType section) {
        return switch (section) {
            case PROFILE -> "Review profile details";
            case EDUCATION -> "Review education";
            case EXPERIENCE -> "Review professional experience";
            case CREDENTIAL -> "Review credentials";
            case PROFESSIONAL_DEVELOPMENT -> "Review professional development";
            case SERVICE -> "Review service contributions";
            case PUBLICATION_PRESENTATION -> "Review publications and presentations";
        };
    }

    private static String detail(ExtractionReviewItem item) {
        return item.getSourceText() == null || item.getSourceText().isBlank()
                ? "Review the extracted suggestion."
                : item.getSourceText().strip();
    }
}

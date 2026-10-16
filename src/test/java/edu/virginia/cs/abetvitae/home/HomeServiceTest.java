package edu.virginia.cs.abetvitae.home;

import edu.virginia.cs.abetvitae.account.model.RoleName;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUser;
import edu.virginia.cs.abetvitae.accreditation.model.AbetTemplate;
import edu.virginia.cs.abetvitae.accreditation.model.AbetTemplateStatus;
import edu.virginia.cs.abetvitae.accreditation.model.FacultyCycleRecord;
import edu.virginia.cs.abetvitae.accreditation.repository.AbetTemplateRepository;
import edu.virginia.cs.abetvitae.accreditation.repository.FacultyCycleRecordRepository;
import edu.virginia.cs.abetvitae.extraction.repository.CvUploadRepository;
import edu.virginia.cs.abetvitae.extraction.repository.ProcessingJobRepository;
import edu.virginia.cs.abetvitae.faculty.model.Professor;
import edu.virginia.cs.abetvitae.faculty.repository.EducationRepository;
import edu.virginia.cs.abetvitae.faculty.repository.ExperienceRepository;
import edu.virginia.cs.abetvitae.faculty.repository.ProfessorRepository;
import edu.virginia.cs.abetvitae.home.dto.HomeResponse;
import edu.virginia.cs.abetvitae.review.repository.ExtractionReviewItemRepository;
import edu.virginia.cs.abetvitae.review.repository.ExtractionReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    private static final UUID ACCOUNT_ID = UUID.fromString(
            "0199f80f-3bf3-7ed1-b5f8-060591413962");
    private static final UUID PROFESSOR_ID = UUID.fromString(
            "0199f80f-4e52-7785-ad78-da3215e9e746");
    private static final UUID TEMPLATE_ID = UUID.fromString(
            "0199f80f-6075-7264-90bb-7312387c670c");
    private static final UUID RECORD_ID = UUID.fromString(
            "0199f80f-6d8c-718b-b450-2bd7cb187901");

    @Mock
    private AbetTemplateRepository abetTemplateRepository;
    @Mock
    private FacultyCycleRecordRepository facultyCycleRecordRepository;
    @Mock
    private ProfessorRepository professorRepository;
    @Mock
    private EducationRepository educationRepository;
    @Mock
    private ExperienceRepository experienceRepository;
    @Mock
    private CvUploadRepository cvUploadRepository;
    @Mock
    private ProcessingJobRepository processingJobRepository;
    @Mock
    private ExtractionReviewRepository extractionReviewRepository;
    @Mock
    private ExtractionReviewItemRepository extractionReviewItemRepository;

    private HomeService service;
    private AbetTemplate template;
    private Professor professor;

    @BeforeEach
    void setUp() {
        service = new HomeService(
                abetTemplateRepository,
                facultyCycleRecordRepository,
                professorRepository,
                educationRepository,
                experienceRepository,
                cvUploadRepository,
                processingJobRepository,
                extractionReviewRepository,
                extractionReviewItemRepository);
        template = template();
        professor = professor();
        when(abetTemplateRepository
                .findFirstByStatusOrderByPublishedAtDescRevisionNumberDesc(
                        AbetTemplateStatus.PUBLISHED))
                .thenReturn(Optional.of(template));
        when(professorRepository.findByUserAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(professor));
    }

    @Test
    void provisionsFirstFacultyCycleRecordBeforeReturningHome() {
        when(facultyCycleRecordRepository.findByProfessorIdAndAbetTemplateId(
                PROFESSOR_ID,
                TEMPLATE_ID)).thenReturn(Optional.empty());
        when(abetTemplateRepository.findByIdForUpdate(TEMPLATE_ID))
                .thenReturn(Optional.of(template));
        FacultyCycleRecord savedRecord = record();
        when(facultyCycleRecordRepository.save(any(FacultyCycleRecord.class)))
                .thenReturn(savedRecord);
        when(cvUploadRepository
                .findFirstByFacultyCycleRecordIdAndPurgedAtIsNullOrderByUploadedAtDesc(
                        RECORD_ID)).thenReturn(Optional.empty());
        when(extractionReviewRepository
                .findFirstByExtractionRunProcessingJobCvUploadFacultyCycleRecordIdAndStatusOrderByCreatedAtDesc(
                        RECORD_ID,
                        edu.virginia.cs.abetvitae.review.model.ExtractionReviewStatus.DRAFT))
                .thenReturn(Optional.empty());
        when(educationRepository.findAllByProfessorIdOrderByDisplayOrderAsc(PROFESSOR_ID))
                .thenReturn(List.of());
        when(experienceRepository.findAllByProfessorIdOrderByDisplayOrderAsc(PROFESSOR_ID))
                .thenReturn(List.of());

        HomeResponse response = service.loadHome(facultyUser());

        assertThat(response.cycle().label()).isEqualTo("2026–27");
        assertThat(response.cycle().departmentName()).isEqualTo("Computer Science");
        assertThat(response.personalVitae().facultyCycleRecordId()).isEqualTo(RECORD_ID);
        assertThat(response.personalVitae().sourceCv()).isNull();
        assertThat(response.facultyRecords()).isEmpty();
        verify(abetTemplateRepository).findByIdForUpdate(TEMPLATE_ID);
    }

    private static AuthenticatedUser facultyUser() {
        return new AuthenticatedUser(
                ACCOUNT_ID,
                "Alex Morgan",
                "Morgan",
                Set.of(RoleName.FACULTY));
    }

    private static AbetTemplate template() {
        AbetTemplate value = mock(AbetTemplate.class);
        when(value.getId()).thenReturn(TEMPLATE_ID);
        when(value.getReviewCycle()).thenReturn("2026–27");
        when(value.getDepartmentName()).thenReturn("Computer Science");
        return value;
    }

    private static Professor professor() {
        Professor value = mock(Professor.class);
        when(value.getId()).thenReturn(PROFESSOR_ID);
        when(value.getFamilyName()).thenReturn("Morgan");
        when(value.getDisplayName()).thenReturn("Alex Morgan");
        return value;
    }

    private FacultyCycleRecord record() {
        FacultyCycleRecord value = FacultyCycleRecord.create(professor, template);
        value.setId(RECORD_ID);
        return value;
    }
}

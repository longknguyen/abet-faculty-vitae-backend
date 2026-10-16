package edu.virginia.cs.abetvitae.home;

import edu.virginia.cs.abetvitae.account.model.RoleName;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUser;
import edu.virginia.cs.abetvitae.accreditation.model.AbetTemplate;
import edu.virginia.cs.abetvitae.accreditation.model.AbetTemplateStatus;
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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    private static final UUID ACCOUNT_ID = UUID.fromString(
            "0199f80f-3bf3-7ed1-b5f8-060591413962");
    private static final UUID PROFESSOR_ID = UUID.fromString(
            "0199f80f-4e52-7785-ad78-da3215e9e746");
    private static final UUID TEMPLATE_ID = UUID.fromString(
            "0199f80f-6075-7264-90bb-7312387c670c");

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
    }

    @Test
    void returnsFirstTimeFacultyHomeWithoutCreatingARecord() {
        AbetTemplate template = template();
        Professor professor = professor();
        when(abetTemplateRepository
                .findFirstByStatusOrderByPublishedAtDescRevisionNumberDesc(
                        AbetTemplateStatus.PUBLISHED))
                .thenReturn(Optional.of(template));
        when(professorRepository.findByUserAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(professor));
        when(facultyCycleRecordRepository.findByProfessorIdAndAbetTemplateId(
                PROFESSOR_ID,
                TEMPLATE_ID)).thenReturn(Optional.empty());

        HomeResponse response = service.loadHome(facultyUser());

        assertThat(response.cycle().label()).isEqualTo("2026–27");
        assertThat(response.cycle().departmentName()).isEqualTo("Computer Science");
        assertThat(response.personalVitae()).isNull();
        assertThat(response.facultyRecords()).isEmpty();
    }

    @Test
    void returnsAnEmptyHomeWhenNoReviewCycleIsConfigured() {
        when(abetTemplateRepository
                .findFirstByStatusOrderByPublishedAtDescRevisionNumberDesc(
                        AbetTemplateStatus.PUBLISHED))
                .thenReturn(Optional.empty());
        when(professorRepository.findByUserAccountId(ACCOUNT_ID))
                .thenReturn(Optional.empty());

        HomeResponse response = service.loadHome(facultyUser());

        assertThat(response.cycle()).isNull();
        assertThat(response.personalVitae()).isNull();
        assertThat(response.facultyRecords()).isEmpty();
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
}

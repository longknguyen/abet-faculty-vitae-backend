package edu.virginia.cs.abetvitae.accreditation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.virginia.cs.abetvitae.account.model.RoleName;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUser;
import edu.virginia.cs.abetvitae.accreditation.dto.ProvisionReviewCycleResponse;
import edu.virginia.cs.abetvitae.accreditation.dto.ReviewCycleRequest;
import edu.virginia.cs.abetvitae.accreditation.model.AbetTemplate;
import edu.virginia.cs.abetvitae.accreditation.model.AbetTemplateStatus;
import edu.virginia.cs.abetvitae.accreditation.model.FacultyCycleRecord;
import edu.virginia.cs.abetvitae.accreditation.model.FacultyCycleStatus;
import edu.virginia.cs.abetvitae.accreditation.repository.AbetTemplateRepository;
import edu.virginia.cs.abetvitae.accreditation.repository.FacultyCycleRecordRepository;
import edu.virginia.cs.abetvitae.faculty.model.Professor;
import edu.virginia.cs.abetvitae.faculty.repository.ProfessorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReviewCycleAdminServiceTest {

    private static final UUID TEMPLATE_ID =
            UUID.fromString("0199f80f-4e52-7785-ad78-da3215e9e746");
    private static final UUID CURRENT_TEMPLATE_ID =
            UUID.fromString("0199f80f-4e52-7785-ad78-da3215e9e747");
    private static final UUID FIRST_PROFESSOR_ID =
            UUID.fromString("0199f80f-4e52-7785-ad78-da3215e9e748");
    private static final UUID SECOND_PROFESSOR_ID =
            UUID.fromString("0199f80f-4e52-7785-ad78-da3215e9e749");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AbetTemplateRepository templateRepository;
    private FacultyCycleRecordRepository facultyCycleRecordRepository;
    private ProfessorRepository professorRepository;
    private ReviewCycleAdminService service;

    @BeforeEach
    void setUp() {
        templateRepository = mock(AbetTemplateRepository.class);
        facultyCycleRecordRepository = mock(FacultyCycleRecordRepository.class);
        professorRepository = mock(ProfessorRepository.class);
        ReviewCycleJsonMapper jsonMapper = new ReviewCycleJsonMapper();
        service = new ReviewCycleAdminService(
                templateRepository,
                facultyCycleRecordRepository,
                professorRepository,
                new ReviewCycleDefinitionValidator(jsonMapper),
                jsonMapper);
        when(professorRepository.count()).thenReturn(2L);
    }

    @Test
    void rejectsEveryAdministrationOperationWithoutTheAdministratorRole() {
        AuthenticatedUser faculty = user(RoleName.FACULTY);

        assertThatThrownBy(() -> service.list(faculty))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("administrator role is required");
        assertThatThrownBy(() -> service.create(faculty, request()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("administrator role is required");
        assertThatThrownBy(() -> service.update(faculty, TEMPLATE_ID, request()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("administrator role is required");
        assertThatThrownBy(() -> service.publish(faculty, TEMPLATE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("administrator role is required");
        assertThatThrownBy(() -> service.retire(faculty, TEMPLATE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("administrator role is required");
        assertThatThrownBy(() -> service.provision(faculty, TEMPLATE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("administrator role is required");
        verifyNoInteractions(templateRepository, facultyCycleRecordRepository);
    }

    @Test
    void createsTheNextDraftRevision() {
        when(templateRepository.findMaximumRevisionNumber("2026–27")).thenReturn(2);
        when(templateRepository.saveAndFlush(any(AbetTemplate.class)))
                .thenAnswer(invocation -> {
                    AbetTemplate saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", TEMPLATE_ID);
                    return saved;
                });
        when(facultyCycleRecordRepository.countByAbetTemplateId(TEMPLATE_ID)).thenReturn(0L);

        var response = service.create(user(RoleName.ADMIN), request());

        assertThat(response.id()).isEqualTo(TEMPLATE_ID);
        assertThat(response.revisionNumber()).isEqualTo(3);
        assertThat(response.status()).isEqualTo(AbetTemplateStatus.DRAFT);
        assertThat(response.requiredSections()).containsExactly("PROFILE", "EDUCATION");
    }

    @Test
    void publishingRetiresThePreviousCurrentCycleAtomically() {
        AbetTemplate draft = template(AbetTemplateStatus.DRAFT, TEMPLATE_ID);
        AbetTemplate current = template(AbetTemplateStatus.PUBLISHED, CURRENT_TEMPLATE_ID);
        when(templateRepository.findByIdForUpdate(TEMPLATE_ID)).thenReturn(Optional.of(draft));
        when(templateRepository.findAllByStatusForUpdate(AbetTemplateStatus.PUBLISHED))
                .thenReturn(List.of(current));

        var response = service.publish(user(RoleName.ADMIN), TEMPLATE_ID);

        assertThat(response.status()).isEqualTo(AbetTemplateStatus.PUBLISHED);
        assertThat(response.publishedAt()).isNotNull();
        assertThat(current.getStatus()).isEqualTo(AbetTemplateStatus.RETIRED);
        verify(templateRepository).flush();
    }

    @Test
    void editsOnlyDraftDefinitions() {
        AbetTemplate draft = template(AbetTemplateStatus.DRAFT, TEMPLATE_ID);
        when(templateRepository.findByIdForUpdate(TEMPLATE_ID)).thenReturn(Optional.of(draft));
        when(templateRepository.saveAndFlush(draft)).thenReturn(draft);

        var response = service.update(user(RoleName.ADMIN), TEMPLATE_ID,
                new ReviewCycleRequest(
                        "2027–28",
                        "Electrical Engineering",
                        List.of("PROFILE"),
                        Map.of("pageLimit", 3),
                        "updated-vitae"));

        assertThat(response.label()).isEqualTo("2027–28");
        assertThat(response.departmentName()).isEqualTo("Electrical Engineering");
        assertThat(response.renderingTemplateName()).isEqualTo("updated-vitae");
        assertThat(response.status()).isEqualTo(AbetTemplateStatus.DRAFT);
    }

    @Test
    void retiresThePublishedCycleWithoutChangingFacultyRecords() {
        AbetTemplate published = template(AbetTemplateStatus.PUBLISHED, TEMPLATE_ID);
        when(templateRepository.findByIdForUpdate(TEMPLATE_ID))
                .thenReturn(Optional.of(published));

        var response = service.retire(user(RoleName.ADMIN), TEMPLATE_ID);

        assertThat(response.status()).isEqualTo(AbetTemplateStatus.RETIRED);
        verify(facultyCycleRecordRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidLifecycleTransitions() {
        AbetTemplate retired = template(AbetTemplateStatus.RETIRED, TEMPLATE_ID);
        when(templateRepository.findByIdForUpdate(TEMPLATE_ID)).thenReturn(Optional.of(retired));

        assertThatThrownBy(() -> service.publish(user(RoleName.ADMIN), TEMPLATE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only draft review cycles can be published");
        assertThatThrownBy(() -> service.retire(user(RoleName.ADMIN), TEMPLATE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only a published review cycle can be retired");
        assertThatThrownBy(() -> service.update(user(RoleName.ADMIN), TEMPLATE_ID, request()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only draft review cycles can be edited");
        assertThatThrownBy(() -> service.provision(user(RoleName.ADMIN), TEMPLATE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("only be provisioned for a published review cycle");
    }

    @Test
    void repeatedProvisioningCreatesOnlyMissingFacultyRecords() {
        AbetTemplate published = template(AbetTemplateStatus.PUBLISHED, TEMPLATE_ID);
        Professor first = professor(FIRST_PROFESSOR_ID);
        Professor second = professor(SECOND_PROFESSOR_ID);
        FacultyCycleRecord existing = FacultyCycleRecord.create(first, published);
        existing.setStatus(FacultyCycleStatus.VERIFIED);
        when(templateRepository.findByIdForUpdate(TEMPLATE_ID))
                .thenReturn(Optional.of(published));
        when(professorRepository.findAllByOrderByFamilyNameAscGivenNameAsc())
                .thenReturn(List.of(first, second));
        when(facultyCycleRecordRepository.findAllByAbetTemplateId(TEMPLATE_ID))
                .thenReturn(List.of(existing))
                .thenReturn(List.of(existing, FacultyCycleRecord.create(second, published)));

        ProvisionReviewCycleResponse firstRun =
                service.provision(user(RoleName.ADMIN), TEMPLATE_ID);
        ProvisionReviewCycleResponse secondRun =
                service.provision(user(RoleName.ADMIN), TEMPLATE_ID);

        assertThat(firstRun.createdCount()).isEqualTo(1);
        assertThat(firstRun.provisionedFacultyCount()).isEqualTo(2);
        assertThat(secondRun.createdCount()).isZero();
        assertThat(existing.getStatus()).isEqualTo(FacultyCycleStatus.VERIFIED);
        verify(facultyCycleRecordRepository).saveAllAndFlush(any());
    }

    private ReviewCycleRequest request() {
        return new ReviewCycleRequest(
                "2026–27",
                "Computer Science",
                List.of("PROFILE", "EDUCATION"),
                Map.of("pageLimit", 2),
                "faculty-vitae");
    }

    private AbetTemplate template(AbetTemplateStatus status, UUID id) {
        AbetTemplate template = AbetTemplate.createDraft(
                "2026–27",
                "Computer Science",
                1,
                objectMapper.createArrayNode().add("PROFILE"),
                objectMapper.createObjectNode(),
                "faculty-vitae");
        ReflectionTestUtils.setField(template, "id", id);
        if (status == AbetTemplateStatus.PUBLISHED) {
            template.publish(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        } else if (status == AbetTemplateStatus.RETIRED) {
            template.publish(java.time.Instant.parse("2026-01-01T00:00:00Z"));
            template.retire();
        }
        return template;
    }

    private static Professor professor(UUID id) {
        Professor professor = mock(Professor.class);
        when(professor.getId()).thenReturn(id);
        return professor;
    }

    private static AuthenticatedUser user(RoleName role) {
        return new AuthenticatedUser(
                UUID.fromString("0199f80f-3bf3-7ed1-b5f8-060591413962"),
                "Test User",
                "User",
                Set.of(role));
    }
}

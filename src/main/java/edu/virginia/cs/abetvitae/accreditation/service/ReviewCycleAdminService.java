package edu.virginia.cs.abetvitae.accreditation.service;

import com.fasterxml.jackson.databind.JsonNode;
import edu.virginia.cs.abetvitae.account.model.RoleName;
import edu.virginia.cs.abetvitae.account.security.AuthenticatedUser;
import edu.virginia.cs.abetvitae.accreditation.dto.ProvisionReviewCycleResponse;
import edu.virginia.cs.abetvitae.accreditation.dto.ReviewCycleRequest;
import edu.virginia.cs.abetvitae.accreditation.dto.ReviewCycleResponse;
import edu.virginia.cs.abetvitae.accreditation.model.AbetTemplate;
import edu.virginia.cs.abetvitae.accreditation.model.AbetTemplateStatus;
import edu.virginia.cs.abetvitae.accreditation.model.FacultyCycleRecord;
import edu.virginia.cs.abetvitae.accreditation.repository.AbetTemplateRepository;
import edu.virginia.cs.abetvitae.accreditation.repository.FacultyCycleRecordRepository;
import edu.virginia.cs.abetvitae.faculty.model.Professor;
import edu.virginia.cs.abetvitae.faculty.repository.ProfessorRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ReviewCycleAdminService {

    private final AbetTemplateRepository templateRepository;
    private final FacultyCycleRecordRepository facultyCycleRecordRepository;
    private final ProfessorRepository professorRepository;
    private final ReviewCycleDefinitionValidator definitionValidator;
    private final ReviewCycleJsonMapper jsonMapper;

    @Transactional(readOnly = true)
    public List<ReviewCycleResponse> list(AuthenticatedUser user) {
        requireAdministrator(user);
        long totalFacultyCount = professorRepository.count();
        return templateRepository.findAllByOrderByReviewCycleDescRevisionNumberDesc()
                .stream()
                .map(template -> response(template, totalFacultyCount))
                .toList();
    }

    @Transactional
    public ReviewCycleResponse create(
            AuthenticatedUser user,
            ReviewCycleRequest request) {
        requireAdministrator(user);
        ReviewCycleDefinitionValidator.ValidatedReviewCycleDefinition definition =
                definitionValidator.validate(request);
        int revisionNumber = templateRepository.findMaximumRevisionNumber(definition.label()) + 1;
        AbetTemplate template = AbetTemplate.createDraft(
                definition.label(),
                definition.departmentName(),
                revisionNumber,
                definition.requiredSections(),
                definition.formattingRules(),
                definition.renderingTemplateName());
        return response(saveDraft(template), professorRepository.count());
    }

    @Transactional
    public ReviewCycleResponse update(
            AuthenticatedUser user,
            UUID templateId,
            ReviewCycleRequest request) {
        requireAdministrator(user);
        ReviewCycleDefinitionValidator.ValidatedReviewCycleDefinition definition =
                definitionValidator.validate(request);
        AbetTemplate template = lockedTemplate(templateId);
        requireStatus(template, AbetTemplateStatus.DRAFT, "Only draft review cycles can be edited");
        template.updateDraft(
                definition.label(),
                definition.departmentName(),
                definition.requiredSections(),
                definition.formattingRules(),
                definition.renderingTemplateName());
        return response(saveDraft(template), professorRepository.count());
    }

    @Transactional
    public ReviewCycleResponse publish(AuthenticatedUser user, UUID templateId) {
        requireAdministrator(user);
        AbetTemplate template = lockedTemplate(templateId);
        requireStatus(template, AbetTemplateStatus.DRAFT,
                "Only draft review cycles can be published");

        templateRepository.findAllByStatusForUpdate(AbetTemplateStatus.PUBLISHED)
                .stream()
                .filter(current -> !current.getId().equals(template.getId()))
                .forEach(AbetTemplate::retire);
        template.publish(Instant.now());
        try {
            templateRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Another review cycle was published concurrently", exception);
        }
        return response(template, professorRepository.count());
    }

    @Transactional
    public ReviewCycleResponse retire(AuthenticatedUser user, UUID templateId) {
        requireAdministrator(user);
        AbetTemplate template = lockedTemplate(templateId);
        requireStatus(template, AbetTemplateStatus.PUBLISHED,
                "Only a published review cycle can be retired");
        template.retire();
        return response(template, professorRepository.count());
    }

    @Transactional
    public ProvisionReviewCycleResponse provision(
            AuthenticatedUser user,
            UUID templateId) {
        requireAdministrator(user);
        AbetTemplate template = lockedTemplate(templateId);
        requireStatus(template, AbetTemplateStatus.PUBLISHED,
                "Faculty records can only be provisioned for a published review cycle");

        Set<UUID> existingProfessorIds = new HashSet<>();
        facultyCycleRecordRepository.findAllByAbetTemplateId(templateId)
                .forEach(record -> existingProfessorIds.add(record.getProfessor().getId()));
        List<Professor> professors = professorRepository.findAllByOrderByFamilyNameAscGivenNameAsc();
        List<FacultyCycleRecord> missingRecords = professors.stream()
                .filter(professor -> !existingProfessorIds.contains(professor.getId()))
                .map(professor -> FacultyCycleRecord.create(professor, template))
                .toList();
        if (!missingRecords.isEmpty()) {
            facultyCycleRecordRepository.saveAllAndFlush(missingRecords);
        }
        return new ProvisionReviewCycleResponse(
                templateId,
                missingRecords.size(),
                existingProfessorIds.size() + missingRecords.size(),
                professors.size());
    }

    private AbetTemplate saveDraft(AbetTemplate template) {
        try {
            return templateRepository.saveAndFlush(template);
        } catch (DataIntegrityViolationException exception) {
            throw conflict("A review-cycle revision with these values already exists", exception);
        }
    }

    private AbetTemplate lockedTemplate(UUID templateId) {
        return templateRepository.findByIdForUpdate(templateId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "The review cycle could not be found"));
    }

    private ReviewCycleResponse response(AbetTemplate template, long totalFacultyCount) {
        return new ReviewCycleResponse(
                template.getId(),
                template.getReviewCycle(),
                template.getDepartmentName(),
                template.getRevisionNumber(),
                template.getStatus(),
                sectionNames(template.getRequiredSections()),
                jsonMapper.toApiValue(template.getFormattingRules()),
                template.getRenderingTemplateName(),
                template.getPublishedAt(),
                facultyCycleRecordRepository.countByAbetTemplateId(template.getId()),
                totalFacultyCount);
    }

    private static List<String> sectionNames(JsonNode sections) {
        return sections.valueStream().map(JsonNode::asText).toList();
    }

    private static void requireAdministrator(AuthenticatedUser user) {
        if (user == null || !user.hasRole(RoleName.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "An administrator role is required to manage review cycles");
        }
    }

    private static void requireStatus(
            AbetTemplate template,
            AbetTemplateStatus requiredStatus,
            String reason) {
        if (template.getStatus() != requiredStatus) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, reason);
        }
    }

    private static ResponseStatusException conflict(String reason, Throwable cause) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason, cause);
    }
}

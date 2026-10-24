package edu.virginia.cs.abetvitae.accreditation.dto;

import edu.virginia.cs.abetvitae.accreditation.model.AbetTemplateStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReviewCycleResponse(
        UUID id,
        String label,
        String departmentName,
        int revisionNumber,
        AbetTemplateStatus status,
        List<String> requiredSections,
        Map<String, Object> formattingRules,
        String renderingTemplateName,
        Instant publishedAt,
        long provisionedFacultyCount,
        long totalFacultyCount) {

    public ReviewCycleResponse {
        requiredSections = List.copyOf(requiredSections);
        formattingRules = JsonObjectCopy.copy(formattingRules);
    }

    @Override
    public List<String> requiredSections() {
        return List.copyOf(requiredSections);
    }

    @Override
    public Map<String, Object> formattingRules() {
        return JsonObjectCopy.copy(formattingRules);
    }
}

package edu.virginia.cs.abetvitae.accreditation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record ReviewCycleRequest(
        @NotBlank @Size(max = 50) String label,
        @NotBlank @Size(max = 200) String departmentName,
        @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 100) String> requiredSections,
        @NotNull Map<String, Object> formattingRules,
        @NotBlank @Size(max = 200) String renderingTemplateName) {

    public ReviewCycleRequest {
        requiredSections = requiredSections == null ? null : List.copyOf(requiredSections);
        formattingRules = JsonObjectCopy.copy(formattingRules);
    }

    @Override
    public List<String> requiredSections() {
        return requiredSections == null ? null : List.copyOf(requiredSections);
    }

    @Override
    public Map<String, Object> formattingRules() {
        return JsonObjectCopy.copy(formattingRules);
    }
}

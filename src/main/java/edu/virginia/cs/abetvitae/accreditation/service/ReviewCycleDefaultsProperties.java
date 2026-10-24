package edu.virginia.cs.abetvitae.accreditation.service;

import edu.virginia.cs.abetvitae.review.model.ReviewSectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties("app.review-cycle-defaults")
public record ReviewCycleDefaultsProperties(
        @NotBlank String departmentName,
        @NotBlank String renderingTemplateName,
        @NotEmpty List<ReviewSectionType> requiredSections) {

    public ReviewCycleDefaultsProperties {
        requiredSections = requiredSections == null ? null : List.copyOf(requiredSections);
    }

    @Override
    public List<ReviewSectionType> requiredSections() {
        return requiredSections == null ? null : List.copyOf(requiredSections);
    }
}

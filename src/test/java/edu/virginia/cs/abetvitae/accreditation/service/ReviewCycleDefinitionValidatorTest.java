package edu.virginia.cs.abetvitae.accreditation.service;

import edu.virginia.cs.abetvitae.accreditation.dto.ReviewCycleRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewCycleDefinitionValidatorTest {

    private final ReviewCycleDefinitionValidator validator =
            new ReviewCycleDefinitionValidator(new ReviewCycleJsonMapper());

    @Test
    void normalisesAndValidatesAReviewCycleDefinition() {
        ReviewCycleRequest request = new ReviewCycleRequest(
                " 2026–27 ",
                " Computer Science ",
                List.of("profile", "EDUCATION"),
                Map.of("pageLimit", 2, "font", "Arial"),
                " faculty-vitae ");

        ReviewCycleDefinitionValidator.ValidatedReviewCycleDefinition result =
                validator.validate(request);

        assertThat(result.label()).isEqualTo("2026–27");
        assertThat(result.departmentName()).isEqualTo("Computer Science");
        assertThat(result.requiredSections().toString())
                .isEqualTo("[\"PROFILE\",\"EDUCATION\"]");
        assertThat(result.renderingTemplateName()).isEqualTo("faculty-vitae");
    }

    @Test
    void rejectsUnknownAndDuplicateRequiredSections() {
        assertThatThrownBy(() -> validator.validate(request(List.of("UNKNOWN"), Map.of())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unsupported required section");

        assertThatThrownBy(() -> validator.validate(
                request(List.of("PROFILE", "profile"), Map.of())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("must not contain duplicates");
    }

    @Test
    void rejectsNullFormattingRuleValues() {
        assertThatThrownBy(() -> validator.validate(
                request(List.of("PROFILE"),
                        Map.of("font", Arrays.asList("Arial", null)))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("must not contain null values");
    }

    private ReviewCycleRequest request(
            List<String> sections,
            Map<String, Object> rules) {
        return new ReviewCycleRequest(
                "2026–27",
                "Computer Science",
                sections,
                rules,
                "faculty-vitae");
    }
}

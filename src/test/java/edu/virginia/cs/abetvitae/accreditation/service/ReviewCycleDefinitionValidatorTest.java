package edu.virginia.cs.abetvitae.accreditation.service;

import edu.virginia.cs.abetvitae.accreditation.dto.ReviewCycleRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewCycleDefinitionValidatorTest {

    private final ReviewCycleDefinitionValidator validator =
            new ReviewCycleDefinitionValidator();

    @Test
    void normalisesAndValidatesAReviewCycleDefinition() {
        ReviewCycleRequest request = new ReviewCycleRequest(
                " 2026–27 ",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2027, 6, 30),
                LocalDate.of(2027, 3, 15));

        ReviewCycleDefinitionValidator.ValidatedReviewCycleDefinition result =
                validator.validate(request);

        assertThat(result.label()).isEqualTo("2026–27");
        assertThat(result.startDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2027, 6, 30));
        assertThat(result.submissionDeadline()).isEqualTo(LocalDate.of(2027, 3, 15));
    }

    @Test
    void rejectsAnInvalidReviewPeriodOrDeadline() {
        ReviewCycleRequest reversedPeriod = new ReviewCycleRequest(
                "2026–27",
                LocalDate.of(2027, 6, 30),
                LocalDate.of(2026, 7, 1),
                null);
        ReviewCycleRequest lateDeadline = new ReviewCycleRequest(
                "2026–27",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2027, 6, 30),
                LocalDate.of(2027, 7, 1));

        assertThatThrownBy(() -> validator.validate(reversedPeriod))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("end date");
        assertThatThrownBy(() -> validator.validate(lateDeadline))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("deadline");
    }
}

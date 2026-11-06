package edu.virginia.cs.abetvitae.faculty.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FacultyOnboardingRequestTest {

    @Test
    void rejectsSuffixLongerThanProfessorColumn() {
        FacultyOnboardingRequest request = new FacultyOnboardingRequest(
                "Alex",
                null,
                "Morgan",
                "x".repeat(51),
                "Alex Morgan");

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(request))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactly("suffix");
        }
    }
}

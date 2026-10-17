package edu.virginia.cs.abetvitae.faculty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FacultyOnboardingRequest(
        @NotBlank @Size(max=100) String givenName,
        @Size(max=100) String middleName,
        @NotBlank @Size(max=100) String familyName,
        @Size(max=100) String suffix
) {

}


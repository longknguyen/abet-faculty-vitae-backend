package edu.virginia.cs.abetvitae.faculty.dto;

import edu.virginia.cs.abetvitae.faculty.model.Professor;

import java.util.UUID;

public record FacultyOnboardingResponse(UUID professorId, String displayName, String email) {
    public static FacultyOnboardingResponse from (Professor professor) {
        return new FacultyOnboardingResponse(professor.getId(),professor.getDisplayName(),professor.getEmail());
    }
}


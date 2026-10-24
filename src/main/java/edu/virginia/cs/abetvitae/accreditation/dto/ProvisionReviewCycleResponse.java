package edu.virginia.cs.abetvitae.accreditation.dto;

import java.util.UUID;

public record ProvisionReviewCycleResponse(
        UUID reviewCycleId,
        int createdCount,
        long provisionedFacultyCount,
        long totalFacultyCount) {
}

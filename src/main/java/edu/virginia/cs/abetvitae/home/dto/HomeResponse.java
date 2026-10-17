package edu.virginia.cs.abetvitae.home.dto;

import edu.virginia.cs.abetvitae.account.model.RoleName;
import edu.virginia.cs.abetvitae.accreditation.model.FacultyCycleStatus;
import edu.virginia.cs.abetvitae.extraction.model.ProcessingJobStatus;
import edu.virginia.cs.abetvitae.extraction.model.ProcessingStage;
import edu.virginia.cs.abetvitae.review.model.ReviewSectionType;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record HomeResponse(
        CurrentUser currentUser,
        Cycle cycle,
        PersonalVitae personalVitae,
        List<FacultyRecord> facultyRecords,
        boolean onboardingNeeded) {

    public HomeResponse {
        facultyRecords = List.copyOf(facultyRecords);
    }

    public record CurrentUser(
            UUID id,
            String displayName,
            String surname,
            Set<RoleName> roles) {

        public CurrentUser {
            roles = Set.copyOf(roles);
        }
    }

    public record Cycle(UUID id, String label, String departmentName) {
    }

    public record PersonalVitae(
            UUID facultyCycleRecordId,
            FacultyCycleStatus status,
            int contentRevision,
            SourceCv sourceCv,
            NextReviewItem nextReviewItem,
            List<PreviewSection> previewSections) {

        public PersonalVitae {
            previewSections = List.copyOf(previewSections);
        }
    }

    public record SourceCv(
            UUID uploadId,
            String originalFilename,
            ProcessingJobStatus jobStatus,
            ProcessingStage processingStage) {
    }

    public record NextReviewItem(
            UUID reviewId,
            UUID reviewItemId,
            ReviewSectionType section,
            String title,
            String detail) {
    }

    public record PreviewSection(String heading, List<String> lines) {

        public PreviewSection {
            lines = List.copyOf(lines);
        }
    }

    public record FacultyRecord(
            UUID professorId,
            UUID facultyCycleRecordId,
            String displayName,
            FacultyCycleStatus status,
            Instant updatedAt,
            Long version) {
    }
}

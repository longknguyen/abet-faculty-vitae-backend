package edu.virginia.cs.abetvitae.accreditation.service;

import edu.virginia.cs.abetvitae.accreditation.dto.ReviewCycleRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Component
class ReviewCycleDefinitionValidator {

    ValidatedReviewCycleDefinition validate(ReviewCycleRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw invalidRequest("Review cycle end date must be on or after its start date");
        }
        if (request.submissionDeadline() != null
                && (request.submissionDeadline().isBefore(request.startDate())
                || request.submissionDeadline().isAfter(request.endDate()))) {
            throw invalidRequest("Submission deadline must fall within the review period");
        }
        return new ValidatedReviewCycleDefinition(
                request.label().strip(),
                request.startDate(),
                request.endDate(),
                request.submissionDeadline());
    }

    private static ResponseStatusException invalidRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }

    record ValidatedReviewCycleDefinition(
            String label,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate submissionDeadline) {
    }
}

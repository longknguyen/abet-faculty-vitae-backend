package edu.virginia.cs.abetvitae.review.repository;

import edu.virginia.cs.abetvitae.review.model.ExtractionReview;
import edu.virginia.cs.abetvitae.review.model.ExtractionReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExtractionReviewRepository extends JpaRepository<ExtractionReview, UUID> {

    Optional<ExtractionReview> findFirstByExtractionRunProcessingJobCvUploadFacultyCycleRecordIdAndStatusOrderByCreatedAtDesc(
            UUID facultyCycleRecordId,
            ExtractionReviewStatus status);
}

package edu.virginia.cs.abetvitae.review.repository;

import edu.virginia.cs.abetvitae.review.model.ExtractionReviewItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExtractionReviewItemRepository
        extends JpaRepository<ExtractionReviewItem, UUID> {

    Optional<ExtractionReviewItem> findFirstByExtractionReviewIdOrderByDisplayOrderAsc(
            UUID extractionReviewId);
}

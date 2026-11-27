package edu.virginia.cs.abetvitae.extraction.repository;

import edu.virginia.cs.abetvitae.extraction.model.CvProcessingOutboxMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CvProcessingOutboxRepository
        extends JpaRepository<CvProcessingOutboxMessage, UUID> {

    List<CvProcessingOutboxMessage>
            findTop50ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAt(
                    Instant now
            );
}

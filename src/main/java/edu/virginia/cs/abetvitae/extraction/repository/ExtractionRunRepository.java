package edu.virginia.cs.abetvitae.extraction.repository;

import edu.virginia.cs.abetvitae.extraction.model.ExtractionRun;
import edu.virginia.cs.abetvitae.extraction.model.ExtractionRunStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExtractionRunRepository
        extends JpaRepository<ExtractionRun, UUID> {

    boolean existsByProcessingJobIdAndStatus(
            UUID processingJobId,
            ExtractionRunStatus status
    );

    Optional<ExtractionRun> findFirstByProcessingJobIdOrderByRunNumberDesc(
            UUID processingJobId
    );
}

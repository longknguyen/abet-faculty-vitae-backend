package edu.virginia.cs.abetvitae.extraction.repository;

import edu.virginia.cs.abetvitae.extraction.model.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, UUID> {

    Optional<ProcessingJob> findByCvUploadId(UUID cvUploadId);
}

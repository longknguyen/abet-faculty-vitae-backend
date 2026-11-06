package edu.virginia.cs.abetvitae.extraction.repository;

import edu.virginia.cs.abetvitae.extraction.model.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

@Repository
public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, UUID> {

    Optional<ProcessingJob> findByCvUploadId(UUID cvUploadId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from ProcessingJob job where job.id = :id")
    Optional<ProcessingJob> findByIdForUpdate(@Param("id") UUID id);
}

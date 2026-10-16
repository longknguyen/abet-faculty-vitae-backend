package edu.virginia.cs.abetvitae.extraction.repository;

import edu.virginia.cs.abetvitae.extraction.model.CvUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CvUploadRepository extends JpaRepository<CvUpload, UUID> {

    Optional<CvUpload> findFirstByFacultyCycleRecordIdAndPurgedAtIsNullOrderByUploadedAtDesc(
            UUID facultyCycleRecordId);
}

package edu.virginia.cs.abetvitae.accreditation.repository;

import edu.virginia.cs.abetvitae.accreditation.model.FacultyCycleRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

@Repository
public interface FacultyCycleRecordRepository extends JpaRepository<FacultyCycleRecord, UUID> {

    @EntityGraph(attributePaths = {"professor", "abetTemplate"})
    Optional<FacultyCycleRecord> findByProfessorIdAndAbetTemplateId(
            UUID professorId,
            UUID abetTemplateId);

    long countByAbetTemplateId(UUID abetTemplateId);

    @EntityGraph(attributePaths = "professor")
    List<FacultyCycleRecord> findAllByAbetTemplateId(UUID abetTemplateId);

    @EntityGraph(attributePaths = {"professor", "professor.userAccount", "abetTemplate"})
    Optional<FacultyCycleRecord> findDetailedById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select record from FacultyCycleRecord record where record.id = :id")
    Optional<FacultyCycleRecord> findByIdForUpdate(@Param("id") UUID id);
}

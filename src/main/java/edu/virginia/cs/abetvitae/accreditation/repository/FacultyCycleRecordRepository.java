package edu.virginia.cs.abetvitae.accreditation.repository;

import edu.virginia.cs.abetvitae.accreditation.model.FacultyCycleRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FacultyCycleRecordRepository extends JpaRepository<FacultyCycleRecord, UUID> {

    @EntityGraph(attributePaths = {"professor", "abetTemplate"})
    Optional<FacultyCycleRecord> findByProfessorIdAndAbetTemplateId(
            UUID professorId,
            UUID abetTemplateId);

    @EntityGraph(attributePaths = "professor")
    List<FacultyCycleRecord> findAllByAbetTemplateId(UUID abetTemplateId);

    @EntityGraph(attributePaths = {"professor", "professor.userAccount", "abetTemplate"})
    Optional<FacultyCycleRecord> findDetailedById(UUID id);
}

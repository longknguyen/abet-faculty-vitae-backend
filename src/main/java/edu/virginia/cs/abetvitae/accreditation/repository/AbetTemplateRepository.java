package edu.virginia.cs.abetvitae.accreditation.repository;

import edu.virginia.cs.abetvitae.accreditation.model.AbetTemplate;
import edu.virginia.cs.abetvitae.accreditation.model.AbetTemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AbetTemplateRepository extends JpaRepository<AbetTemplate, UUID> {

    Optional<AbetTemplate> findFirstByStatusOrderByPublishedAtDescRevisionNumberDesc(
            AbetTemplateStatus status);

    List<AbetTemplate> findAllByOrderByReviewCycleDescRevisionNumberDesc();

    Optional<AbetTemplate> findFirstByOrderByReviewCycleDescRevisionNumberDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select template from AbetTemplate template where template.id = :id")
    Optional<AbetTemplate> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select template from AbetTemplate template where template.status = :status")
    List<AbetTemplate> findAllByStatusForUpdate(@Param("status") AbetTemplateStatus status);

    @Query("""
            select coalesce(max(template.revisionNumber), 0)
            from AbetTemplate template
            where lower(template.reviewCycle) = lower(:reviewCycle)
            """)
    int findMaximumRevisionNumber(@Param("reviewCycle") String reviewCycle);
}

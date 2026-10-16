package edu.virginia.cs.abetvitae.accreditation.repository;

import edu.virginia.cs.abetvitae.accreditation.model.AbetTemplate;
import edu.virginia.cs.abetvitae.accreditation.model.AbetTemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AbetTemplateRepository extends JpaRepository<AbetTemplate, UUID> {

    Optional<AbetTemplate> findFirstByStatusOrderByPublishedAtDescRevisionNumberDesc(
            AbetTemplateStatus status);
}

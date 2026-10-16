package edu.virginia.cs.abetvitae.faculty.repository;

import edu.virginia.cs.abetvitae.faculty.model.Experience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExperienceRepository extends JpaRepository<Experience, UUID> {

    List<Experience> findAllByProfessorIdOrderByDisplayOrderAsc(UUID professorId);
}

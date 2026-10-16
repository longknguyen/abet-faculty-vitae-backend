package edu.virginia.cs.abetvitae.faculty.repository;

import edu.virginia.cs.abetvitae.faculty.model.Education;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EducationRepository extends JpaRepository<Education, UUID> {

    List<Education> findAllByProfessorIdOrderByDisplayOrderAsc(UUID professorId);
}

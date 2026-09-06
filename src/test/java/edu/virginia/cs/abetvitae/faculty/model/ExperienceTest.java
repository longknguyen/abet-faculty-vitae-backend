package edu.virginia.cs.abetvitae.faculty.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExperienceTest {

    @Test
    void newExperienceIsNotCurrentByDefault() {
        Experience experience = new Experience();

        assertThat(experience.isCurrent()).isFalse();
    }

    @Test
    void experienceRetainsEmploymentDetails() {
        Professor professor = new Professor();
        Experience experience = new Experience();
        experience.setProfessor(professor);
        experience.setExperienceType(ExperienceType.ACADEMIC);
        experience.setOrganisation("University of Virginia");
        experience.setAcademicRank("Professor");
        experience.setPositionTitle("Professor of Computer Science");
        experience.setStartYear(2020);
        experience.setEndYear(2025);
        experience.setCurrent(true);
        experience.setEmploymentType(EmploymentType.FULL_TIME);
        experience.setDisplayOrder(2);

        assertThat(experience.getProfessor()).isSameAs(professor);
        assertThat(experience.getExperienceType()).isEqualTo(ExperienceType.ACADEMIC);
        assertThat(experience.getOrganisation()).isEqualTo("University of Virginia");
        assertThat(experience.getAcademicRank()).isEqualTo("Professor");
        assertThat(experience.getPositionTitle()).isEqualTo("Professor of Computer Science");
        assertThat(experience.getStartYear()).isEqualTo(2020);
        assertThat(experience.getEndYear()).isEqualTo(2025);
        assertThat(experience.isCurrent()).isTrue();
        assertThat(experience.getEmploymentType()).isEqualTo(EmploymentType.FULL_TIME);
        assertThat(experience.getDisplayOrder()).isEqualTo(2);
    }
}

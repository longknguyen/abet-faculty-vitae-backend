package edu.virginia.cs.abetvitae.faculty.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EducationTest {

    @Test
    void educationRetainsAcademicDetails() {
        Professor professor = new Professor();
        Education education = new Education();
        education.setProfessor(professor);
        education.setDegree("PhD");
        education.setDiscipline("Computer Science");
        education.setInstitution("University of Virginia");
        education.setGraduationYear(2020);
        education.setDisplayOrder(1);

        assertThat(education.getProfessor()).isSameAs(professor);
        assertThat(education.getDegree()).isEqualTo("PhD");
        assertThat(education.getDiscipline()).isEqualTo("Computer Science");
        assertThat(education.getInstitution()).isEqualTo("University of Virginia");
        assertThat(education.getGraduationYear()).isEqualTo(2020);
        assertThat(education.getDisplayOrder()).isEqualTo(1);
    }
}

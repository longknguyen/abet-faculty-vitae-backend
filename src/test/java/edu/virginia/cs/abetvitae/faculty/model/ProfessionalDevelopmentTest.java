package edu.virginia.cs.abetvitae.faculty.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalDevelopmentTest {

    @Test
    void professionalDevelopmentRetainsActivityDetails() {
        Professor professor = new Professor();
        LocalDate startDate = LocalDate.of(2026, 6, 1);
        LocalDate endDate = LocalDate.of(2026, 6, 2);
        ProfessionalDevelopment activity = new ProfessionalDevelopment();
        activity.setProfessor(professor);
        activity.setActivityName("ABET Workshop");
        activity.setProvider("ABET");
        activity.setStartDate(startDate);
        activity.setEndDate(endDate);
        activity.setDescription("Assessment and accreditation workshop.");
        activity.setDisplayOrder(4);

        assertThat(activity.getProfessor()).isSameAs(professor);
        assertThat(activity.getActivityName()).isEqualTo("ABET Workshop");
        assertThat(activity.getProvider()).isEqualTo("ABET");
        assertThat(activity.getStartDate()).isEqualTo(startDate);
        assertThat(activity.getEndDate()).isEqualTo(endDate);
        assertThat(activity.getDescription())
                .isEqualTo("Assessment and accreditation workshop.");
        assertThat(activity.getDisplayOrder()).isEqualTo(4);
    }
}

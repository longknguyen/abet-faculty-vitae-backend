package edu.virginia.cs.abetvitae.faculty.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ContributionTest {

    @Test
    void contributionRetainsPublicationDetails() {
        Professor professor = new Professor();
        LocalDate date = LocalDate.of(2026, 4, 15);
        Contribution contribution = new Contribution();
        contribution.setProfessor(professor);
        contribution.setContributionType(ContributionType.PUBLICATION_PRESENTATION);
        contribution.setTitle("Reliable CV Extraction");
        contribution.setCitation("A. Faculty, Reliable CV Extraction, 2026");
        contribution.setVenueOrOrganisation("ABET Symposium");
        contribution.setContributionDate(date);
        contribution.setDescription("A presentation about structured CV extraction.");
        contribution.setDisplayOrder(3);

        assertThat(contribution.getProfessor()).isSameAs(professor);
        assertThat(contribution.getContributionType())
                .isEqualTo(ContributionType.PUBLICATION_PRESENTATION);
        assertThat(contribution.getTitle()).isEqualTo("Reliable CV Extraction");
        assertThat(contribution.getCitation())
                .isEqualTo("A. Faculty, Reliable CV Extraction, 2026");
        assertThat(contribution.getVenueOrOrganisation()).isEqualTo("ABET Symposium");
        assertThat(contribution.getContributionDate()).isEqualTo(date);
        assertThat(contribution.getDescription())
                .isEqualTo("A presentation about structured CV extraction.");
        assertThat(contribution.getDisplayOrder()).isEqualTo(3);
    }
}

package edu.virginia.cs.abetvitae.faculty.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContributionTypeTest {

    @Test
    void valuesMatchPersistenceContract() {
        assertThat(ContributionType.values()).containsExactly(
                ContributionType.SERVICE,
                ContributionType.PUBLICATION_PRESENTATION
        );
    }
}

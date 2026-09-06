package edu.virginia.cs.abetvitae.faculty.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExperienceTypeTest {

    @Test
    void valuesMatchPersistenceContract() {
        assertThat(ExperienceType.values()).containsExactly(
                ExperienceType.ACADEMIC,
                ExperienceType.INDUSTRY,
                ExperienceType.OTHER
        );
    }
}

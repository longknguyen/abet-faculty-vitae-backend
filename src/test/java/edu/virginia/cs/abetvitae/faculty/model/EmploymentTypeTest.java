package edu.virginia.cs.abetvitae.faculty.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmploymentTypeTest {

    @Test
    void valuesMatchPersistenceContract() {
        assertThat(EmploymentType.values()).containsExactly(
                EmploymentType.FULL_TIME,
                EmploymentType.PART_TIME
        );
    }
}

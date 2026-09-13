package edu.virginia.cs.abetvitae.accreditation.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FacultyCycleStatusTest {

    @Test
    void valuesMatchPersistenceContract() {
        assertThat(FacultyCycleStatus.values()).containsExactly(
                FacultyCycleStatus.DRAFT,
                FacultyCycleStatus.SUBMITTED,
                FacultyCycleStatus.VERIFIED
        );
    }
}

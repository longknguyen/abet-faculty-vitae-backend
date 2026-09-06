package edu.virginia.cs.abetvitae.faculty.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialTypeTest {

    @Test
    void valuesMatchPersistenceContract() {
        assertThat(CredentialType.values()).containsExactly(
                CredentialType.LICENCE,
                CredentialType.AWARD,
                CredentialType.CERTIFICATION,
                CredentialType.OTHER
        );
    }
}

package edu.virginia.cs.abetvitae.account.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleNameTest {

    @Test
    void valuesMatchPersistenceContract() {
        assertThat(RoleName.values()).containsExactly(
                RoleName.ADMIN,
                RoleName.FACULTY
        );
    }
}

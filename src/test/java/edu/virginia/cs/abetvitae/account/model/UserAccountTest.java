package edu.virginia.cs.abetvitae.account.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountTest {

    @Test
    void newAccountIsEnabledWithNoRoles() {
        UserAccount account = new UserAccount();

        assertThat(account.isEnabled()).isTrue();
        assertThat(account.getRoles()).isEmpty();
    }

    @Test
    void accountCanHaveBothAdministrativeAndFacultyRoles() {
        UserAccount account = new UserAccount();

        account.getRoles().add(RoleName.ADMIN);
        account.getRoles().add(RoleName.FACULTY);

        assertThat(account.getRoles())
                .containsExactlyInAnyOrder(RoleName.ADMIN, RoleName.FACULTY);
    }
}

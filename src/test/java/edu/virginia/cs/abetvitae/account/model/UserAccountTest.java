package edu.virginia.cs.abetvitae.account.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountTest {

    @Test
    void newAccountIsEnabled() {
        UserAccount account = new UserAccount();

        assertThat(account.isEnabled()).isTrue();
    }
}

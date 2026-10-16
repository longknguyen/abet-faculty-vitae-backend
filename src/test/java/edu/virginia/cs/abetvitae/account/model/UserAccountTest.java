package edu.virginia.cs.abetvitae.account.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountTest {

    @Test
    void createsAccountFromNormalisedNetBadgeIdentity() {
        UserAccount account = UserAccount.fromNetBadgeIdentity(
                "  Faculty@Virginia.edu  ",
                "  abc1de  ");

        assertThat(account.getEmail()).isEqualTo("faculty@virginia.edu");
        assertThat(account.getExternalSubject()).isEqualTo("abc1de");
        assertThat(account.isEnabled()).isTrue();
    }

    @Test
    void newAccountIsEnabled() {
        UserAccount account = new UserAccount();

        assertThat(account.isEnabled()).isTrue();
    }
}

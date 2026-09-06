package edu.virginia.cs.abetvitae.accredidation.model;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import edu.virginia.cs.abetvitae.account.model.UserAccount;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FacultyCycleRecordTest {

    @Test
    void newRecordStartsAsFirstDraftRevision() {
        FacultyCycleRecord record = new FacultyCycleRecord();

        assertThat(record.getStatus()).isEqualTo(FacultyCycleStatus.DRAFT);
        assertThat(record.getContentRevision()).isEqualTo(1);
    }

    @Test
    void markingContentChangedCreatesDraftAndClearsVerification() {
        FacultyCycleRecord record = new FacultyCycleRecord();
        record.setContentRevision(4);
        record.setStatus(FacultyCycleStatus.VERIFIED);
        record.setSubmittedAt(Instant.parse("2026-09-01T12:00:00Z"));
        record.setVerifiedByUser(mock(UserAccount.class));
        record.setVerifiedAt(Instant.parse("2026-09-02T12:00:00Z"));
        record.setProfileSnapshot(JsonNodeFactory.instance.objectNode().put("name", "Ada"));

        record.markContentChanged();

        assertThat(record.getContentRevision()).isEqualTo(5);
        assertThat(record.getStatus()).isEqualTo(FacultyCycleStatus.DRAFT);
        assertThat(record.getSubmittedAt()).isNull();
        assertThat(record.getVerifiedByUser()).isNull();
        assertThat(record.getVerifiedAt()).isNull();
        assertThat(record.getProfileSnapshot()).isNull();
    }
}

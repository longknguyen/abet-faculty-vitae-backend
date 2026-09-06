package edu.virginia.cs.abetvitae.extraction.model;

import edu.virginia.cs.abetvitae.account.model.UserAccount;
import edu.virginia.cs.abetvitae.accredidation.model.FacultyCycleRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CvUploadTest {

    @Test
    void uploadRetainsItsFacultyCycleAndFileMetadata() {
        FacultyCycleRecord facultyCycleRecord = mock(FacultyCycleRecord.class);
        UserAccount uploader = mock(UserAccount.class);
        Instant expiresAt = Instant.parse("2026-09-08T12:00:00Z");
        CvUpload upload = new CvUpload();
        upload.setFacultyCycleRecord(facultyCycleRecord);
        upload.setUploadedByUser(uploader);
        upload.setOriginalFilename("faculty-cv.pdf");
        upload.setTemporaryStorageKey("uploads/temporary/faculty-cv.pdf");
        upload.setDetectedMimeType("application/pdf");
        upload.setFileSizeBytes(4096L);
        upload.setFileSha256("a".repeat(64));
        upload.setExpiresAt(expiresAt);

        assertThat(upload.getFacultyCycleRecord()).isSameAs(facultyCycleRecord);
        assertThat(upload.getUploadedByUser()).isSameAs(uploader);
        assertThat(upload.getOriginalFilename()).isEqualTo("faculty-cv.pdf");
        assertThat(upload.getTemporaryStorageKey())
                .isEqualTo("uploads/temporary/faculty-cv.pdf");
        assertThat(upload.getDetectedMimeType()).isEqualTo("application/pdf");
        assertThat(upload.getFileSizeBytes()).isEqualTo(4096L);
        assertThat(upload.getFileSha256()).isEqualTo("a".repeat(64));
        assertThat(upload.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void purgedUploadCanClearItsTemporaryStorageKey() {
        CvUpload upload = new CvUpload();
        Instant purgedAt = Instant.parse("2026-09-09T12:00:00Z");
        upload.setTemporaryStorageKey(null);
        upload.setPurgedAt(purgedAt);

        assertThat(upload.getTemporaryStorageKey()).isNull();
        assertThat(upload.getPurgedAt()).isEqualTo(purgedAt);
    }
}

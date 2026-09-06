package edu.virginia.cs.abetvitae.extraction.model;

import edu.virginia.cs.abetvitae.account.model.UserAccount;
import edu.virginia.cs.abetvitae.accredidation.model.FacultyCycleRecord;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "cv_upload",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_cv_upload_temporary_storage_key",
                columnNames = "temporary_storage_key"
        )
)
public class CvUpload {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_cycle_record_id", nullable = false)
    private FacultyCycleRecord facultyCycleRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private UserAccount uploadedByUser;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "temporary_storage_key", nullable = true, length = 500)
    private String temporaryStorageKey;

    @Column(name = "detected_mime_type", nullable = false, length = 100)
    private String detectedMimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "file_sha256", nullable = false, length = 64)
    private String fileSha256;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "purged_at")
    private Instant purgedAt;
}

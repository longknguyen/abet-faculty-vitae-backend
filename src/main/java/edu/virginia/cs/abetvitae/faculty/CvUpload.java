package edu.virginia.cs.abetvitae.faculty;

import edu.virginia.cs.abetvitae.account.model.UserAccount;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "cv_upload")
public class CvUpload {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private UserAccount uploadedByUser;

    @Column(name = "original_filename", nullable = false, length = 20)
    private String originalFilename;

    @Column(name = "temporary_storage_key", nullable = false, length = 500)
    private String temporaryStorageKey;

    @Column(name = "detected_mime_type", nullable = false, length = 100)
    private String detectedMimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "file_sha256", nullable = false, length = 64)
    private String fileSha256;

    @CreationTimestamp
    @Column(name = "uploaded_At", nullable = false, updatable = false)
    private Instant uploadedAt;
}

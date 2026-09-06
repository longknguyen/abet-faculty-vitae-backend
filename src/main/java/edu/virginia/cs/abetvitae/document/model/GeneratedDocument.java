package edu.virginia.cs.abetvitae.document.model;

import com.fasterxml.jackson.databind.JsonNode;
import edu.virginia.cs.abetvitae.account.model.UserAccount;
import edu.virginia.cs.abetvitae.accredidation.model.FacultyCycleRecord;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "generated_document",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_generated_document_cycle_format",
                columnNames = {
                        "faculty_cycle_record_id",
                        "document_format"
                }
        )
)
public class GeneratedDocument {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_cycle_record_id", nullable = false)
    private FacultyCycleRecord facultyCycleRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generated_by_user_id", nullable = false)
    private UserAccount generatedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_format", nullable = false, length = 10)
    private DocumentFormat documentFormat;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "storage_key", length = 500)
    private String storageKey;

    @Column(name = "file_sha256", nullable = false, length = 64)
    private String fileSha256;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "source_content_revision", nullable = false)
    private int sourceContentRevision;

    @Column(name = "page_count")
    private Integer pageCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 20)
    private DocumentValidationStatus validationStatus =
            DocumentValidationStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_details", columnDefinition = "jsonb")
    private JsonNode validationDetails;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "purged_at")
    private Instant purgedAt;
}

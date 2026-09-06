package edu.virginia.cs.abetvitae.extraction.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "extraction_run",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_extraction_run_number",
                columnNames = {"processing_job_id", "run_number"}
        )
)
public class ExtractionRun {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processing_job_id", nullable = false)
    private ProcessingJob processingJob;

    @Column(name = "run_number", nullable = false)
    private int runNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExtractionRunStatus status;

    @Column(name = "extracted_text", columnDefinition = "text")
    private String extractedText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "structured_output", columnDefinition = "jsonb")
    private JsonNode structuredOutput;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "payload_expires_at")
    private Instant payloadExpiresAt;

    @Column(name = "payload_purged_at")
    private Instant payloadPurgedAt;
}
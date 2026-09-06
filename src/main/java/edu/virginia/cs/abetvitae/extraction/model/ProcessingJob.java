package edu.virginia.cs.abetvitae.extraction.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "processing_job")
public class ProcessingJob {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cv_upload_id", nullable = false, unique = true)
    private CvUpload cvUpload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcessingJobStatus status = ProcessingJobStatus.QUEUED;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage", nullable = false, length = 40)
    private ProcessingStage currentStage = ProcessingStage.QUEUED;

    @CreationTimestamp
    @Column(name = "queued_at", nullable = false, updatable = false)
    private Instant queuedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;
}

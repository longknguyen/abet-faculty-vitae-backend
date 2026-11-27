package edu.virginia.cs.abetvitae.review.model;

import edu.virginia.cs.abetvitae.account.model.UserAccount;
import edu.virginia.cs.abetvitae.accreditation.model.FacultyCycleRecord;
import edu.virginia.cs.abetvitae.extraction.model.ExtractionRun;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "extraction_review")
public class ExtractionReview {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "extraction_run_id", nullable = false, unique = true)
    private ExtractionRun extractionRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_cycle_record_id", nullable = false)
    private FacultyCycleRecord facultyCycleRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewed_by_user_id", nullable = false)
    private UserAccount reviewedByUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExtractionReviewStatus status = ExtractionReviewStatus.DRAFT;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    public static ExtractionReview createDraft(
            ExtractionRun extractionRun,
            FacultyCycleRecord facultyCycleRecord,
            UserAccount reviewedByUser
    ) {
        ExtractionReview review = new ExtractionReview();
        review.extractionRun = java.util.Objects.requireNonNull(extractionRun);
        review.facultyCycleRecord = java.util.Objects.requireNonNull(
                facultyCycleRecord
        );
        review.reviewedByUser = java.util.Objects.requireNonNull(reviewedByUser);
        return review;
    }
}

package edu.virginia.cs.abetvitae.accredidation.model;

import com.fasterxml.jackson.databind.JsonNode;
import edu.virginia.cs.abetvitae.account.model.UserAccount;
import edu.virginia.cs.abetvitae.faculty.model.Professor;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "faculty_cycle_record",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_faculty_cycle_professor_template",
                columnNames = {"professor_id", "abet_template_id"}
        )
)
public class FacultyCycleRecord {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "abet_template_id", nullable = false)
    private AbetTemplate abetTemplate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FacultyCycleStatus status = FacultyCycleStatus.DRAFT;

    @Column(name = "content_revision", nullable = false)
    private int contentRevision = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profile_snapshot", columnDefinition = "jsonb")
    private JsonNode profileSnapshot;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private UserAccount verifiedByUser;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void markContentChanged() {
        contentRevision++;
        status = FacultyCycleStatus.DRAFT;
        submittedAt = null;
        verifiedByUser = null;
        verifiedAt = null;
        profileSnapshot = null;
    }
}

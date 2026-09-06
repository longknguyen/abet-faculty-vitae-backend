package edu.virginia.cs.abetvitae.review.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "extraction_review_item")
public class ExtractionReviewItem {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "extraction_review_id", nullable = false)
    private ExtractionReview extractionReview;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false, length = 40)
    private ReviewSectionType sectionType;

    @Column(name = "source_text", columnDefinition = "text")
    private String sourceText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_suggested_value", nullable = false, columnDefinition = "jsonb")
    private JsonNode aiSuggestedValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "edited_value", columnDefinition = "jsonb")
    private JsonNode editedValue;

    @Column(nullable = false)
    private boolean selected = false;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    public JsonNode effectiveValue() {
        return editedValue != null ? editedValue : aiSuggestedValue;
    }
}
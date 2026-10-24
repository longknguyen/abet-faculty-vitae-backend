package edu.virginia.cs.abetvitae.accreditation.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "abet_template",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_abet_template_cycle_revision",
                columnNames = {"review_cycle", "revision_number"}
        )
)
public class AbetTemplate {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "review_cycle", nullable = false, length = 50)
    private String reviewCycle;

    @Column(name = "department_name", nullable = false, length = 200)
    private String departmentName;

    @Column(name = "review_start_date")
    private LocalDate reviewStartDate;

    @Column(name = "review_end_date")
    private LocalDate reviewEndDate;

    @Column(name = "submission_deadline")
    private LocalDate submissionDeadline;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AbetTemplateStatus status = AbetTemplateStatus.DRAFT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_sections", nullable = false, columnDefinition = "jsonb")
    private JsonNode requiredSections = JsonNodeFactory.instance.arrayNode();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "formatting_rules", nullable = false, columnDefinition = "jsonb")
    private JsonNode formattingRules = JsonNodeFactory.instance.objectNode();

    @Column(name = "rendering_template_name", nullable = false, length = 200)
    private String renderingTemplateName;

    @Column(name = "published_at")
    private Instant publishedAt;

    public static AbetTemplate createDraft(
            String reviewCycle,
            LocalDate reviewStartDate,
            LocalDate reviewEndDate,
            LocalDate submissionDeadline,
            String departmentName,
            int revisionNumber,
            JsonNode requiredSections,
            JsonNode formattingRules,
            String renderingTemplateName) {
        AbetTemplate template = new AbetTemplate();
        template.reviewCycle = reviewCycle;
        template.reviewStartDate = reviewStartDate;
        template.reviewEndDate = reviewEndDate;
        template.submissionDeadline = submissionDeadline;
        template.departmentName = departmentName;
        template.revisionNumber = revisionNumber;
        template.requiredSections = requiredSections.deepCopy();
        template.formattingRules = formattingRules.deepCopy();
        template.renderingTemplateName = renderingTemplateName;
        return template;
    }

    public void updateDraft(
            String reviewCycle,
            LocalDate reviewStartDate,
            LocalDate reviewEndDate,
            LocalDate submissionDeadline,
            String departmentName,
            JsonNode requiredSections,
            JsonNode formattingRules,
            String renderingTemplateName) {
        requireStatus(AbetTemplateStatus.DRAFT);
        this.reviewCycle = reviewCycle;
        this.reviewStartDate = reviewStartDate;
        this.reviewEndDate = reviewEndDate;
        this.submissionDeadline = submissionDeadline;
        this.departmentName = departmentName;
        this.requiredSections = requiredSections.deepCopy();
        this.formattingRules = formattingRules.deepCopy();
        this.renderingTemplateName = renderingTemplateName;
    }

    public void publish(Instant publicationTime) {
        requireStatus(AbetTemplateStatus.DRAFT);
        status = AbetTemplateStatus.PUBLISHED;
        publishedAt = Objects.requireNonNull(publicationTime);
    }

    public void retire() {
        requireStatus(AbetTemplateStatus.PUBLISHED);
        status = AbetTemplateStatus.RETIRED;
    }

    public JsonNode getRequiredSections() {
        return requiredSections.deepCopy();
    }

    public JsonNode getFormattingRules() {
        return formattingRules.deepCopy();
    }

    private void requireStatus(AbetTemplateStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Expected template status " + expected + " but was " + status);
        }
    }
}

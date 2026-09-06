package edu.virginia.cs.abetvitae.accredidation.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
}

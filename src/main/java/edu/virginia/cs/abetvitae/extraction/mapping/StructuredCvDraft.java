package edu.virginia.cs.abetvitae.extraction.mapping;

import java.util.List;
import java.util.Objects;

/** Immutable mapper output that must pass validation before it can become a draft review. */
public record StructuredCvDraft(
        List<MappedReviewSection> sections,
        MappingProvenance provenance) {

    public StructuredCvDraft {
        sections = List.copyOf(Objects.requireNonNull(sections));
        provenance = Objects.requireNonNull(provenance);
    }
}

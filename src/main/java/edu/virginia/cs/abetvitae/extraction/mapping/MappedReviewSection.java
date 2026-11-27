package edu.virginia.cs.abetvitae.extraction.mapping;

import edu.virginia.cs.abetvitae.review.model.ReviewSectionType;
import java.util.List;
import java.util.Objects;

/** One unconfirmed ABET review section derived from sanitised parser text. */
public record MappedReviewSection(
        ReviewSectionType sectionType,
        String sourceText,
        List<String> entries,
        int displayOrder) {

    public MappedReviewSection {
        sectionType = Objects.requireNonNull(sectionType);
        entries = List.copyOf(Objects.requireNonNull(entries));
    }
}

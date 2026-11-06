package edu.virginia.cs.abetvitae.extraction.mapping;

import edu.virginia.cs.abetvitae.review.model.ReviewSectionType;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Strictly validates mapper output before any draft review is persisted. */
@Component
public final class StructuredCvDraftValidator {

    private static final int MAXIMUM_ENTRIES_PER_SECTION = 200;
    private static final int MAXIMUM_ENTRY_LENGTH = 10_000;
    private static final int MAXIMUM_SOURCE_LENGTH = 500_000;
    private static final int MAXIMUM_TOTAL_ENTRY_CHARACTERS = 1_000_000;

    public void validate(StructuredCvDraft draft) {
        if (draft == null) {
            throw invalid();
        }
        Set<ReviewSectionType> sectionTypes = EnumSet.noneOf(
                ReviewSectionType.class
        );
        Set<Integer> displayOrders = new HashSet<>();
        int totalCharacters = 0;
        for (MappedReviewSection section : draft.sections()) {
            if (!sectionTypes.add(section.sectionType())
                    || !displayOrders.add(section.displayOrder())
                    || section.displayOrder() < 0
                    || section.entries().size() > MAXIMUM_ENTRIES_PER_SECTION
                    || section.sourceText() != null
                    && section.sourceText().length() > MAXIMUM_SOURCE_LENGTH) {
                throw invalid();
            }
            for (String entry : section.entries()) {
                if (entry == null
                        || entry.isBlank()
                        || entry.length() > MAXIMUM_ENTRY_LENGTH) {
                    throw invalid();
                }
                totalCharacters += entry.length();
                if (totalCharacters > MAXIMUM_TOTAL_ENTRY_CHARACTERS) {
                    throw invalid();
                }
            }
        }
        if (!sectionTypes.equals(EnumSet.allOf(ReviewSectionType.class))
                || displayOrders.size() != ReviewSectionType.values().length) {
            throw invalid();
        }
        for (int order = 0; order < ReviewSectionType.values().length; order++) {
            if (!displayOrders.contains(order)) {
                throw invalid();
            }
        }
    }

    private static CvMappingException invalid() {
        return new CvMappingException(
                "The structured CV draft did not satisfy the review contract"
        );
    }
}

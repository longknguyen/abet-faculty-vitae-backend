package edu.virginia.cs.abetvitae.extraction.mapping;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.virginia.cs.abetvitae.review.model.ReviewSectionType;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class StructuredCvDraftValidatorTest {

    private final StructuredCvDraftValidator validator =
            new StructuredCvDraftValidator();

    @Test
    void acceptsExactlyOneBoundedSuggestionForEverySupportedSection() {
        assertThatCode(() -> validator.validate(completeDraft()))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsIncompleteMapperOutputBeforePersistence() {
        StructuredCvDraft incomplete = new StructuredCvDraft(
                completeDraft().sections().stream()
                        .filter(section -> section.sectionType()
                                != ReviewSectionType.CREDENTIAL)
                        .toList(),
                completeDraft().provenance()
        );

        assertThatThrownBy(() -> validator.validate(incomplete))
                .isInstanceOf(CvMappingException.class)
                .hasMessage(
                        "The structured CV draft did not satisfy the review contract"
                );
    }

    private static StructuredCvDraft completeDraft() {
        return new StructuredCvDraft(
                Arrays.stream(ReviewSectionType.values())
                        .map(type -> new MappedReviewSection(
                                type,
                                "source",
                                List.of("entry"),
                                type.ordinal()
                        ))
                        .toList(),
                new MappingProvenance("fixture", "1", null, null)
        );
    }
}

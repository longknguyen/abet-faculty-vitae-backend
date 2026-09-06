package edu.virginia.cs.abetvitae.review.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewSectionTypeTest {

    @Test
    void valuesMatchPersistenceContract() {
        assertThat(ReviewSectionType.values()).containsExactly(
                ReviewSectionType.PROFILE,
                ReviewSectionType.EDUCATION,
                ReviewSectionType.EXPERIENCE,
                ReviewSectionType.CREDENTIAL,
                ReviewSectionType.PROFESSIONAL_DEVELOPMENT,
                ReviewSectionType.SERVICE,
                ReviewSectionType.PUBLICATION_PRESENTATION
        );
    }
}

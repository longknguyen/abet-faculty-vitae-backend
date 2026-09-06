package edu.virginia.cs.abetvitae.review.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractionReviewStatusTest {

    @Test
    void valuesMatchPersistenceContract() {
        assertThat(ExtractionReviewStatus.values()).containsExactly(
                ExtractionReviewStatus.DRAFT,
                ExtractionReviewStatus.CONFIRMED
        );
    }
}

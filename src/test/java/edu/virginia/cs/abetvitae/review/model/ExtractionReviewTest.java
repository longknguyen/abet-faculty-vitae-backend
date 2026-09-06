package edu.virginia.cs.abetvitae.review.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractionReviewTest {

    @Test
    void newReviewStartsAsDraft() {
        ExtractionReview review = new ExtractionReview();

        assertThat(review.getStatus()).isEqualTo(ExtractionReviewStatus.DRAFT);
    }
}

package edu.virginia.cs.abetvitae.review.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractionReviewItemTest {

    @Test
    void effectiveValueUsesAiSuggestionWhenNoEditExists() {
        ExtractionReviewItem item = new ExtractionReviewItem();
        JsonNode suggestion = JsonNodeFactory.instance.objectNode().put("degree", "PhD");
        item.setAiSuggestedValue(suggestion);

        assertThat(item.effectiveValue()).isSameAs(suggestion);
    }

    @Test
    void effectiveValuePrefersEditedValue() {
        ExtractionReviewItem item = new ExtractionReviewItem();
        JsonNode suggestion = JsonNodeFactory.instance.objectNode().put("degree", "PhD");
        JsonNode edit = JsonNodeFactory.instance.objectNode().put("degree", "DPhil");
        item.setAiSuggestedValue(suggestion);
        item.setEditedValue(edit);

        assertThat(item.effectiveValue()).isSameAs(edit);
    }

    @Test
    void newReviewItemIsNotSelected() {
        ExtractionReviewItem item = new ExtractionReviewItem();

        assertThat(item.isSelected()).isFalse();
    }
}

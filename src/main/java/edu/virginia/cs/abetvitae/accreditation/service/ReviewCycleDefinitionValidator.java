package edu.virginia.cs.abetvitae.accreditation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import edu.virginia.cs.abetvitae.accreditation.dto.ReviewCycleRequest;
import edu.virginia.cs.abetvitae.review.model.ReviewSectionType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
class ReviewCycleDefinitionValidator {

    private static final int MAXIMUM_JSON_DEPTH = 4;
    private static final int MAXIMUM_JSON_NODES = 100;
    private static final int MAXIMUM_RULE_NAME_LENGTH = 100;
    private static final int MAXIMUM_TEXT_VALUE_LENGTH = 1_000;

    private final ReviewCycleJsonMapper jsonMapper;

    ReviewCycleDefinitionValidator(ReviewCycleJsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    ValidatedReviewCycleDefinition validate(ReviewCycleRequest request) {
        ArrayNode requiredSections = validateRequiredSections(request);
        JsonNode formattingRules = jsonMapper.toPersistenceValue(request.formattingRules());
        validateFormattingRules(formattingRules);
        return new ValidatedReviewCycleDefinition(
                request.label().strip(),
                request.departmentName().strip(),
                requiredSections,
                formattingRules,
                request.renderingTemplateName().strip());
    }

    private static ArrayNode validateRequiredSections(ReviewCycleRequest request) {
        ArrayNode validated = JsonNodeFactory.instance.arrayNode();
        Set<ReviewSectionType> seen = new HashSet<>();
        for (String value : request.requiredSections()) {
            ReviewSectionType section;
            try {
                section = ReviewSectionType.valueOf(value.strip().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw invalidRequest("Unsupported required section: " + value);
            }
            if (!seen.add(section)) {
                throw invalidRequest("Required sections must not contain duplicates");
            }
            validated.add(section.name());
        }
        return validated;
    }

    private static void validateFormattingRules(JsonNode rules) {
        if (!rules.isObject()) {
            throw invalidRequest("Formatting rules must be a JSON object");
        }
        int nodes = validateJsonNode(rules, 0);
        if (nodes > MAXIMUM_JSON_NODES) {
            throw invalidRequest("Formatting rules contain too many values");
        }
    }

    private static int validateJsonNode(JsonNode node, int depth) {
        if (depth > MAXIMUM_JSON_DEPTH) {
            throw invalidRequest("Formatting rules are nested too deeply");
        }
        if (node.isNull() || node.isMissingNode()) {
            throw invalidRequest("Formatting rules must not contain null values");
        }
        if (node.isTextual() && (node.textValue().isBlank()
                || node.textValue().length() > MAXIMUM_TEXT_VALUE_LENGTH)) {
            throw invalidRequest("Formatting rule text values must be non-blank and at most "
                    + MAXIMUM_TEXT_VALUE_LENGTH + " characters");
        }

        int nodes = 1;
        if (node.isObject()) {
            for (Map.Entry<String, JsonNode> field : node.properties()) {
                if (field.getKey().isBlank()
                        || field.getKey().length() > MAXIMUM_RULE_NAME_LENGTH) {
                    throw invalidRequest("Formatting rule names must be non-blank and at most "
                            + MAXIMUM_RULE_NAME_LENGTH + " characters");
                }
                nodes += validateJsonNode(field.getValue(), depth + 1);
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                nodes += validateJsonNode(child, depth + 1);
            }
        } else if (!node.isValueNode()) {
            throw invalidRequest("Formatting rules contain an unsupported JSON value");
        }
        return nodes;
    }

    private static ResponseStatusException invalidRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }

    record ValidatedReviewCycleDefinition(
            String label,
            String departmentName,
            JsonNode requiredSections,
            JsonNode formattingRules,
            String renderingTemplateName) {
    }
}

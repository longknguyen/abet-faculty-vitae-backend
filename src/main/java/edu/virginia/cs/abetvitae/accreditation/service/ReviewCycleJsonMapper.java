package edu.virginia.cs.abetvitae.accreditation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class ReviewCycleJsonMapper {

    private static final TypeReference<Map<String, Object>> JSON_OBJECT_TYPE =
            new TypeReference<>() { };

    private final ObjectMapper objectMapper = new ObjectMapper();

    JsonNode toPersistenceValue(Map<String, Object> value) {
        return objectMapper.valueToTree(value);
    }

    Map<String, Object> toApiValue(JsonNode value) {
        return objectMapper.convertValue(value, JSON_OBJECT_TYPE);
    }
}

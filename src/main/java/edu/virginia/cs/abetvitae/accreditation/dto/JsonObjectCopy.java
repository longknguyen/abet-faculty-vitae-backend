package edu.virginia.cs.abetvitae.accreditation.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JsonObjectCopy {

    private JsonObjectCopy() {
    }

    static Map<String, Object> copy(Map<String, Object> source) {
        if (source == null) {
            return null;
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, copyValue(value)));
        return Collections.unmodifiableMap(copy);
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, child) -> {
                if (!(key instanceof String stringKey)) {
                    throw new IllegalArgumentException("JSON object keys must be strings");
                }
                copy.put(stringKey, copyValue(child));
            });
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            list.forEach(child -> copy.add(copyValue(child)));
            return Collections.unmodifiableList(copy);
        }
        return value;
    }
}

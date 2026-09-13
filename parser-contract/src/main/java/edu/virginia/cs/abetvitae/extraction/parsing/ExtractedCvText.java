package edu.virginia.cs.abetvitae.extraction.parsing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Bounded plain text and a small, allow-listed subset of document metadata. */
public record ExtractedCvText(
        String detectedMediaType, String text, Map<String, List<String>> metadata) {

    public ExtractedCvText {
        detectedMediaType = Objects.requireNonNull(detectedMediaType);
        text = Objects.requireNonNull(text);
        Objects.requireNonNull(metadata);

        Map<String, List<String>> immutableMetadata = new LinkedHashMap<>();
        metadata.forEach((name, values) ->
                immutableMetadata.put(name, List.copyOf(values)));
        metadata = Map.copyOf(immutableMetadata);
    }
}

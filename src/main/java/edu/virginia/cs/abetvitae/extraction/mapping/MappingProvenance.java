package edu.virginia.cs.abetvitae.extraction.mapping;

/** Non-secret identifiers for the mapper and optional private model implementation. */
public record MappingProvenance(
        String mapperName,
        String mapperVersion,
        String modelProvider,
        String modelName) {

    public MappingProvenance {
        mapperName = requireValue(mapperName, "Mapper name");
        mapperVersion = requireValue(mapperVersion, "Mapper version");
        modelProvider = optionalValue(modelProvider);
        modelName = optionalValue(modelName);
    }

    private static String requireValue(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static String optionalValue(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

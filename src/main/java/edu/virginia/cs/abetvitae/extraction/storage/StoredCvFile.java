package edu.virginia.cs.abetvitae.extraction.storage;

public record StoredCvFile(
        String storageKey,
        long fileSizeBytes,
        String sha256
) {
}

package edu.virginia.cs.abetvitae.extraction.storage;

import java.io.InputStream;
import java.io.IOException;

public interface PrivateCvStorage {

    StoredCvFile store(
            InputStream inputStream,
            long maximumBytes,
            String contentType
    ) throws IOException;

    void delete(String storageKey) throws IOException;
}
package edu.virginia.cs.abetvitae.extraction.storage;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public interface PrivateCvStorage {

    StoredCvFile store(
            InputStream inputStream,
            long maximumBytes,
            String contentType
    ) throws IOException;

    long copyTo(
            String storageKey,
            OutputStream outputStream,
            long maximumBytes
    ) throws IOException;

    void delete(String storageKey) throws IOException;
}

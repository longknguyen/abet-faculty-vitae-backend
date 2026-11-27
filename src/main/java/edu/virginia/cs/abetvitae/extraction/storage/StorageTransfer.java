package edu.virginia.cs.abetvitae.extraction.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class StorageTransfer {

    private static final int BUFFER_SIZE = 8_192;

    private StorageTransfer() {
    }

    static long copy(
            InputStream input,
            OutputStream output,
            long maximumBytes
    ) throws IOException {
        if (maximumBytes < 1) {
            throw new IllegalArgumentException("Maximum bytes must be positive");
        }

        byte[] buffer = new byte[BUFFER_SIZE];
        long totalBytes = 0;
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            totalBytes += bytesRead;
            if (totalBytes > maximumBytes) {
                throw new IOException("Stored file exceeds the maximum allowed size");
            }
            output.write(buffer, 0, bytesRead);
        }
        return totalBytes;
    }
}

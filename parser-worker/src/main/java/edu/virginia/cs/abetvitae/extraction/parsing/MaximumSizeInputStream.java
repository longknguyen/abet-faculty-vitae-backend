package edu.virginia.cs.abetvitae.extraction.parsing;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/** Enforces a byte limit even if a staged file changes after its initial size check. */
final class MaximumSizeInputStream extends FilterInputStream {

    private final long maximumBytes;
    private long bytesRead;

    MaximumSizeInputStream(InputStream input, long maximumBytes) {
        super(Objects.requireNonNull(input));
        this.maximumBytes = maximumBytes;
    }

    @Override
    public int read() throws IOException {
        int value = super.read();
        if (value != -1) {
            count(1L);
        }
        return value;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        long remainingWithProbe = maximumBytes - bytesRead + 1L;
        int boundedLength = (int) Math.min(length, Math.max(remainingWithProbe, 1L));
        int read = super.read(buffer, offset, boundedLength);
        if (read > 0) {
            count(read);
        }
        return read;
    }

    @Override
    public long skip(long amount) throws IOException {
        long remainingWithProbe = maximumBytes - bytesRead + 1L;
        long skipped = super.skip(Math.min(amount, Math.max(remainingWithProbe, 1L)));
        if (skipped > 0) {
            count(skipped);
        }
        return skipped;
    }

    private void count(long amount) throws MaximumSizeExceededException {
        bytesRead += amount;
        if (bytesRead > maximumBytes) {
            throw new MaximumSizeExceededException();
        }
    }
}

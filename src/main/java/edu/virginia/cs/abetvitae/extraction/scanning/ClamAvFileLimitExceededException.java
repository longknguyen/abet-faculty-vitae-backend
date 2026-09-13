package edu.virginia.cs.abetvitae.extraction.scanning;

import java.io.IOException;
import java.io.Serial;

/** Signals that a file grew beyond the configured limit while it was being streamed. */
final class ClamAvFileLimitExceededException extends IOException {

    @Serial
    private static final long serialVersionUID = 1L;
}

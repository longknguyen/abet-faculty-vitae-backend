package edu.virginia.cs.abetvitae.extraction.parsing;

import java.io.IOException;
import java.io.Serial;

/** Signals that a stream exceeded its configured byte limit. */
final class MaximumSizeExceededException extends IOException {

    @Serial
    private static final long serialVersionUID = 1L;
}

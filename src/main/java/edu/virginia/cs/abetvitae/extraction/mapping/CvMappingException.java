package edu.virginia.cs.abetvitae.extraction.mapping;

import java.io.Serial;

public final class CvMappingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CvMappingException(String message) {
        super(message);
    }
}

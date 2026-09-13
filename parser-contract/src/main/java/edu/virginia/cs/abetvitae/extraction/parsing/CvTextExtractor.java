package edu.virginia.cs.abetvitae.extraction.parsing;

import java.nio.file.Path;

/** Extracts bounded text from a staged CV after format validation. */
@FunctionalInterface
public interface CvTextExtractor {

    ExtractedCvText extract(Path stagedFile, String originalFilename);
}

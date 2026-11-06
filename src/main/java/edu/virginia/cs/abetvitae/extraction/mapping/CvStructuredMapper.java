package edu.virginia.cs.abetvitae.extraction.mapping;

import edu.virginia.cs.abetvitae.extraction.parsing.ExtractedCvText;

/** Deterministic boundary for mapping parser output; implementations receive no tools or credentials. */
public interface CvStructuredMapper {

    StructuredCvDraft map(ExtractedCvText extractedText);
}

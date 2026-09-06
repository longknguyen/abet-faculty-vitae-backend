package edu.virginia.cs.abetvitae.extraction.model;

public enum ProcessingStage {
    QUEUED,
    VALIDATING,
    EXTRACTING_TEXT,
    STRUCTURING_WITH_AI,
    CREATING_REVIEW,
    COMPLETE
}
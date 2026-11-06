package edu.virginia.cs.abetvitae.extraction.service;

import edu.virginia.cs.abetvitae.extraction.model.CvUpload;
import edu.virginia.cs.abetvitae.extraction.model.ProcessingJob;

/** Upload metadata and the durable job created in the same transaction. */
public record CvUploadReceipt(CvUpload upload, ProcessingJob processingJob) {
}

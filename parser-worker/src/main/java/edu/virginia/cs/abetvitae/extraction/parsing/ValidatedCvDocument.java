package edu.virginia.cs.abetvitae.extraction.parsing;

import org.apache.tika.mime.MediaType;

/** Sanitised filename and independently detected, allow-listed document type. */
record ValidatedCvDocument(String safeFilename, MediaType mediaType) {}

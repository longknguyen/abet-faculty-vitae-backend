package edu.virginia.cs.abetvitae.extraction.messaging;

final class CvProcessingQueues {

    static final String EXCHANGE = "abet.vitae.cv.processing";
    static final String ROUTING_KEY = "cv.process";
    static final String QUEUE = "abet.vitae.cv.processing";
    static final String DEAD_LETTER_EXCHANGE = "abet.vitae.cv.processing.dlx";
    static final String DEAD_LETTER_ROUTING_KEY = "cv.process.dead";
    static final String DEAD_LETTER_QUEUE = "abet.vitae.cv.processing.dead";

    private CvProcessingQueues() {
    }
}

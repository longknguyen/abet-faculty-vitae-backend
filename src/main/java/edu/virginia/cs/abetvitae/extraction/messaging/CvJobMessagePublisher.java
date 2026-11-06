package edu.virginia.cs.abetvitae.extraction.messaging;

import java.util.UUID;

interface CvJobMessagePublisher {

    void publish(UUID processingJobId);
}

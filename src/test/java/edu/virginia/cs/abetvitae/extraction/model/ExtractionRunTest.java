package edu.virginia.cs.abetvitae.extraction.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractionRunTest {

    @Test
    void extractionRunRetainsResultAndPayloadLifecycle() {
        ProcessingJob processingJob = new ProcessingJob();
        JsonNode output = JsonNodeFactory.instance.objectNode().put("name", "Ada Lovelace");
        Instant completedAt = Instant.parse("2026-09-06T12:00:00Z");
        Instant expiresAt = Instant.parse("2026-09-08T12:00:00Z");
        ExtractionRun run = new ExtractionRun();
        run.setProcessingJob(processingJob);
        run.setRunNumber(2);
        run.setStatus(ExtractionRunStatus.SUCCEEDED);
        run.setExtractedText("Extracted curriculum vitae text");
        run.setStructuredOutput(output);
        run.setConfidenceScore(new BigDecimal("0.9750"));
        run.setCompletedAt(completedAt);
        run.setPayloadExpiresAt(expiresAt);

        assertThat(run.getProcessingJob()).isSameAs(processingJob);
        assertThat(run.getRunNumber()).isEqualTo(2);
        assertThat(run.getStatus()).isEqualTo(ExtractionRunStatus.SUCCEEDED);
        assertThat(run.getExtractedText()).isEqualTo("Extracted curriculum vitae text");
        assertThat(run.getStructuredOutput()).isSameAs(output);
        assertThat(run.getConfidenceScore()).isEqualByComparingTo("0.9750");
        assertThat(run.getCompletedAt()).isEqualTo(completedAt);
        assertThat(run.getPayloadExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void purgedExtractionCanClearTemporaryPayloads() {
        ExtractionRun run = new ExtractionRun();
        Instant purgedAt = Instant.parse("2026-09-09T12:00:00Z");
        run.setExtractedText(null);
        run.setStructuredOutput(null);
        run.setPayloadPurgedAt(purgedAt);

        assertThat(run.getExtractedText()).isNull();
        assertThat(run.getStructuredOutput()).isNull();
        assertThat(run.getPayloadPurgedAt()).isEqualTo(purgedAt);
    }
}

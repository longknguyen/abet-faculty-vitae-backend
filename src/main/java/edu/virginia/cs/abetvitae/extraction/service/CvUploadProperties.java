package edu.virginia.cs.abetvitae.extraction.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Resource and retention limits enforced before an upload enters private storage. */
@ConfigurationProperties("app.cv-upload")
public record CvUploadProperties(
        long maximumFileSizeBytes,
        Duration retentionPeriod) {

    private static final long MAXIMUM_CONFIGURED_FILE_SIZE = 25L * 1_024L * 1_024L;
    private static final Duration MAXIMUM_RETENTION_PERIOD = Duration.ofDays(365);

    public CvUploadProperties {
        if (maximumFileSizeBytes < 1
                || maximumFileSizeBytes > MAXIMUM_CONFIGURED_FILE_SIZE) {
            throw new IllegalArgumentException("The CV upload size limit is invalid");
        }
        if (retentionPeriod == null
                || retentionPeriod.isNegative()
                || retentionPeriod.isZero()
                || retentionPeriod.compareTo(MAXIMUM_RETENTION_PERIOD) > 0) {
            throw new IllegalArgumentException("The CV upload retention period is invalid");
        }
    }
}

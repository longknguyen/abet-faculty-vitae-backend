package edu.virginia.cs.abetvitae.extraction.messaging;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.cv-processing")
public record CvProcessingProperties(
        boolean enabled,
        Duration outboxPollInterval,
        Duration publisherConfirmTimeout,
        int maximumAttempts,
        Duration initialRetryInterval,
        double retryMultiplier,
        Duration maximumRetryInterval) {

    public CvProcessingProperties {
        requirePositive(outboxPollInterval, "Outbox poll interval");
        requirePositive(publisherConfirmTimeout, "Publisher confirm timeout");
        requirePositive(initialRetryInterval, "Initial retry interval");
        requirePositive(maximumRetryInterval, "Maximum retry interval");
        if (maximumAttempts < 1 || maximumAttempts > 10) {
            throw new IllegalArgumentException(
                    "CV processing maximum attempts must be between 1 and 10"
            );
        }
        if (!Double.isFinite(retryMultiplier) || retryMultiplier < 1) {
            throw new IllegalArgumentException(
                    "CV processing retry multiplier must be at least 1"
            );
        }
        if (maximumRetryInterval.compareTo(initialRetryInterval) < 0) {
            throw new IllegalArgumentException(
                    "Maximum retry interval cannot be shorter than the initial interval"
            );
        }
    }

    private static void requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}

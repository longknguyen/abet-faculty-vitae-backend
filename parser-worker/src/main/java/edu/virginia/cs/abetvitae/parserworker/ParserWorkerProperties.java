package edu.virginia.cs.abetvitae.parserworker;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Security-sensitive limits for the parser worker. */
@ConfigurationProperties("parser-worker")
public record ParserWorkerProperties(
        String token,
        long maximumFileSizeBytes,
        int maximumExtractedCharacters,
        long maximumPdfMainMemoryBytes,
        Duration maximumExecutionTime,
        Path temporaryDirectory) {

    private static final int MINIMUM_TOKEN_LENGTH = 32;
    private static final long MAXIMUM_FILE_SIZE_BYTES = 25L * 1_024L * 1_024L;
    private static final int MAXIMUM_EXTRACTED_CHARACTERS = 2_000_000;
    private static final long MAXIMUM_PDF_MAIN_MEMORY_BYTES = 64L * 1_024L * 1_024L;
    private static final Duration MAXIMUM_EXECUTION_TIME = Duration.ofSeconds(45);

    public ParserWorkerProperties {
        if (token == null
                || token.length() < MINIMUM_TOKEN_LENGTH
                || token.chars().anyMatch(character -> character <= 0x20 || character >= 0x7f)) {
            throw new IllegalArgumentException(
                    "PARSER_WORKER_TOKEN must contain at least 32 visible ASCII characters");
        }
        if (maximumFileSizeBytes < 1
                || maximumFileSizeBytes > MAXIMUM_FILE_SIZE_BYTES
                || maximumExtractedCharacters < 1
                || maximumExtractedCharacters > MAXIMUM_EXTRACTED_CHARACTERS
                || maximumPdfMainMemoryBytes < 1
                || maximumPdfMainMemoryBytes > MAXIMUM_PDF_MAIN_MEMORY_BYTES) {
            throw new IllegalArgumentException("Parser-worker size limits are invalid");
        }
        if (maximumExecutionTime == null
                || maximumExecutionTime.isZero()
                || maximumExecutionTime.isNegative()
                || maximumExecutionTime.compareTo(MAXIMUM_EXECUTION_TIME) > 0) {
            throw new IllegalArgumentException("The parser execution time is invalid");
        }
        if (temporaryDirectory == null
                || !temporaryDirectory.isAbsolute()
                || temporaryDirectory.normalize().getParent() == null) {
            throw new IllegalArgumentException("The parser temporary directory must be absolute");
        }
    }
}

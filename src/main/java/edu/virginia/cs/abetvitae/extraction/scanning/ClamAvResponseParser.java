package edu.virginia.cs.abetvitae.extraction.scanning;

/** Converts a bounded ClamAV protocol response into a domain result. */
final class ClamAvResponseParser {

    private static final String RESPONSE_PREFIX = "stream: ";
    private static final String INFECTED_SUFFIX = " FOUND";

    MalwareScanResult parse(String response) {
        if ("stream: OK".equals(response)) {
            return MalwareScanResult.clean();
        }
        if (response.startsWith(RESPONSE_PREFIX) && response.endsWith(INFECTED_SUFFIX)) {
            String signature = response.substring(
                    RESPONSE_PREFIX.length(), response.length() - INFECTED_SUFFIX.length());
            return MalwareScanResult.infected(signature);
        }
        throw new MalwareScanningException(
                MalwareScanFailure.SCAN_FAILED,
                "Malware scanning did not produce a conclusive result");
    }
}

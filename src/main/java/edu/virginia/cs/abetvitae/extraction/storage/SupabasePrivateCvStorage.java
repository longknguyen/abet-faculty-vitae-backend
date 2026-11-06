package edu.virginia.cs.abetvitae.extraction.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import java.time.Duration;

public class SupabasePrivateCvStorage implements PrivateCvStorage {

    private final String supabaseUrl;
    private final String secretKey;
    private final String bucketName;

    private final HttpClient httpClient;
    private final Duration requestTimeout;

    public SupabasePrivateCvStorage(
            String supabaseUrl,
            String secretKey,
            String bucketName,
            HttpClient httpClient,
            Duration requestTimeout
    ) {
        this.supabaseUrl = requireBaseUrl(supabaseUrl);
        this.secretKey = requireValue(secretKey, "Supabase secret key");
        this.bucketName = requireBucketName(bucketName);
        this.httpClient = java.util.Objects.requireNonNull(httpClient);
        this.requestTimeout = requirePositiveTimeout(requestTimeout);
    }

    @Override
    public StoredCvFile store(
            InputStream inputStream,
            long maximumBytes,
            String contentType
    ) throws IOException {

        String storageKey = UUID.randomUUID().toString();

        // Read the file while enforcing the maximum allowed size.
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MessageDigest digest;

        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }

        byte[] buffer = new byte[8192];
        long totalBytes = 0;
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {

            totalBytes += bytesRead;

            if (totalBytes > maximumBytes) {
                throw new IOException(
                        "Uploaded file exceeds the maximum allowed size"
                );
            }

            digest.update(buffer, 0, bytesRead);
            outputStream.write(buffer, 0, bytesRead);
        }

        if (totalBytes == 0) {
            throw new IOException("Uploaded file is empty");
        }

        byte[] fileBytes = outputStream.toByteArray();

        // Create the Supabase upload URL.
        URI uploadUri = URI.create(
                supabaseUrl + "/storage/v1/object/"
                        + bucketName + "/" + storageKey
        );

        // Build the upload request.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uploadUri)
                .header("apikey", secretKey)
                .header("Authorization", "Bearer " + secretKey)
                .header("Content-Type", contentType)
                .header("x-upsert", "false")
                .timeout(requestTimeout)
                .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
                .build();

        try {
            HttpResponse<Void> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException(
                        "Supabase upload failed with status "
                                + response.statusCode()
                );
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "Supabase upload was interrupted",
                    exception
            );
        }

        return new StoredCvFile(
                storageKey,
                totalBytes,
                HexFormat.of().formatHex(digest.digest())
        );
    }

    @Override
    public long copyTo(
            String storageKey,
            OutputStream outputStream,
            long maximumBytes
    ) throws IOException {
        validateStorageKey(storageKey);
        URI downloadUri = URI.create(
                supabaseUrl + "/storage/v1/object/authenticated/"
                        + bucketName + "/" + storageKey
        );
        HttpRequest request = authenticatedRequest(downloadUri)
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            try (InputStream responseBody = response.body()) {
                if (response.statusCode() < 200
                        || response.statusCode() >= 300) {
                    throw new IOException(
                            "Supabase download failed with status "
                                    + response.statusCode()
                    );
                }
                return StorageTransfer.copy(
                        responseBody,
                        outputStream,
                        maximumBytes
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Supabase download was interrupted", exception);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        validateStorageKey(storageKey);

        URI deleteUri = URI.create(
                supabaseUrl + "/storage/v1/object/"
                        + bucketName + "/" + storageKey
        );

        HttpRequest request = authenticatedRequest(deleteUri)
                .DELETE()
                .build();

        try {
            HttpResponse<Void> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException(
                        "Supabase file deletion failed with status "
                                + response.statusCode()
                );
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Supabase deletion was interrupted", exception);
        }
    }

    private HttpRequest.Builder authenticatedRequest(URI uri) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .timeout(requestTimeout)
                .header("apikey", secretKey)
                .header("Authorization", "Bearer " + secretKey);
    }

    private static void validateStorageKey(String storageKey)
            throws IOException {
        if (storageKey == null || !storageKey.matches("[a-f0-9-]{36}")) {
            throw new IOException("Invalid storage key");
        }
    }

    private static String requireBaseUrl(String value) {
        String baseUrl = requireValue(value, "Supabase URL")
                .replaceAll("/+$", "");
        URI uri = URI.create(baseUrl);
        if (!("https".equalsIgnoreCase(uri.getScheme())
                || "http".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) {
            throw new IllegalArgumentException("Supabase URL is invalid");
        }
        return baseUrl;
    }

    private static String requireBucketName(String value) {
        String bucket = requireValue(value, "Supabase bucket");
        if (!bucket.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Supabase bucket is invalid");
        }
        return bucket;
    }

    private static String requireValue(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be configured");
        }
        return value;
    }

    private static Duration requirePositiveTimeout(Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(
                    "Supabase request timeout must be positive"
            );
        }
        return value;
    }
}

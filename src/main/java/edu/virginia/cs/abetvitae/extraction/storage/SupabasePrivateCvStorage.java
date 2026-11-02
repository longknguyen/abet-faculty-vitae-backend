package edu.virginia.cs.abetvitae.extraction.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.io.ByteArrayOutputStream;

public class SupabasePrivateCvStorage implements PrivateCvStorage {

    private final String supabaseUrl;
    private final String secretKey;
    private final String bucketName;

    private final HttpClient httpClient;

    public SupabasePrivateCvStorage(
            String supabaseUrl,
            String secretKey,
            String bucketName,
            HttpClient httpClient
    ) {
        this.supabaseUrl = supabaseUrl.replaceAll("/+$", "");
        this.secretKey = secretKey;
        this.bucketName = bucketName;
        this.httpClient = httpClient;
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
    public void delete(String storageKey) throws IOException {
        if (storageKey == null || !storageKey.matches("[a-f0-9-]{36}")) {
            throw new IOException("Invalid storage key");
        }

        URI deleteUri = URI.create(
                supabaseUrl + "/storage/v1/object/"
                        + bucketName + "/" + storageKey
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(deleteUri)
                .header("apikey", secretKey)
                .header("Authorization", "Bearer " + secretKey)
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
}

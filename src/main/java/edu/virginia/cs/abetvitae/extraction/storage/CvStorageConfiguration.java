package edu.virginia.cs.abetvitae.extraction.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.http.HttpClient;
import java.time.Duration;

import java.io.IOException;

@Configuration
public class CvStorageConfiguration {

    @Bean
    @ConditionalOnProperty(
            name = "app.cv-storage.provider",
            havingValue = "local",
            matchIfMissing = true
    )
    public PrivateCvStorage privateCvStorage(
            @Value("${app.cv-storage.directory}") String storageDirectory
    ) throws IOException {
        return new FileSystemPrivateCvStorage(storageDirectory);
    }

    @Bean
    @ConditionalOnProperty(
            name = "app.cv-storage.provider",
            havingValue = "supabase"
    )
    public PrivateCvStorage supabasePrivateCvStorage(
            @Value("${app.cv-storage.supabase.url}") String supabaseUrl,
            @Value("${app.cv-storage.supabase.secret-key}") String secretKey,
            @Value("${app.cv-storage.supabase.bucket}") String bucketName,
            @Value("${app.cv-storage.connect-timeout}") Duration connectTimeout,
            @Value("${app.cv-storage.request-timeout}") Duration requestTimeout
    ) {
        return new SupabasePrivateCvStorage(
                supabaseUrl,
                secretKey,
                bucketName,
                HttpClient.newBuilder()
                        .connectTimeout(connectTimeout)
                        .build(),
                requestTimeout
        );
    }
}

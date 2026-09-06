package edu.virginia.cs.abetvitae;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("netbadge")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "app.netbadge.base-url=https://faculty-vitae.example.edu/",
            "app.netbadge.idp-metadata-uri=classpath:saml/test-idp-metadata.xml",
            "testcontainers.ollama.enabled=false",
            "testcontainers.rabbitmq.enabled=false"
        }
)
class NetBadgeMetadataEndpointTest {

    @LocalServerPort
    private int port;

    @Test
    void publishesServiceProviderMetadataWithoutAuthentication() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/saml2/metadata/netbadge"))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("entityID=\"https://faculty-vitae.example.edu"
                        + "/saml2/service-provider-metadata/netbadge\"")
                .contains("Location=\"https://faculty-vitae.example.edu"
                        + "/login/saml2/sso/netbadge\"");
    }
}

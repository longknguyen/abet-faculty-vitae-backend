package edu.virginia.cs.abetvitae;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
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
            "app.netbadge.frontend-base-url=https://faculty-vitae-frontend.example.edu/",
            "app.netbadge.assertion-consumer-service-base-url="
                    + "https://faculty-vitae-frontend.example.edu/",
            "testcontainers.ollama.enabled=false",
            "testcontainers.rabbitmq.enabled=false"
        }
)
class NetBadgeMetadataEndpointTest {

    @LocalServerPort
    private int port;

    @Autowired
    private AuthenticationSuccessHandler netBadgeAuthenticationSuccessHandler;

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
                .contains("Location=\"https://faculty-vitae-frontend.example.edu"
                        + "/login/saml2/sso/netbadge\"");
    }

    @Test
    void permitsSessionCookieOnNetBadgePostCallback() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port
                        + "/saml2/authenticate/netbadge"))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isBetween(200, 399);
        assertThat(response.headers().allValues("set-cookie"))
                .anySatisfy(cookie -> assertThat(cookie)
                        .contains("Secure")
                        .contains("HttpOnly")
                        .contains("SameSite=None"));
    }

    @Test
    void redirectsSuccessfulAuthenticationToConfiguredFrontend() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "test-user",
                null);

        netBadgeAuthenticationSuccessHandler.onAuthenticationSuccess(
                request,
                response,
                authentication);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://faculty-vitae-frontend.example.edu");
    }

    @Test
    void permitsCredentialedHomeRequestsFromConfiguredFrontend() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/home"))
                .header("Origin", "https://faculty-vitae-frontend.example.edu")
                .header("Access-Control-Request-Method", "GET")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("access-control-allow-origin"))
                .contains("https://faculty-vitae-frontend.example.edu");
        assertThat(response.headers().firstValue("access-control-allow-credentials"))
                .contains("true");
    }

    @Test
    void returnsUnauthorizedInsteadOfSamlHtmlForUnauthenticatedApiRequest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/home"))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("location")).isEmpty();
    }
}

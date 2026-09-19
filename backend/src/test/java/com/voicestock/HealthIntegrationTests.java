package com.voicestock;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("bootstrap")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthIntegrationTests {
    @LocalServerPort
    int port;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(5));
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthReturnsLiveServiceStatusWithoutCredentials() throws Exception {
        var response = send(request("/api/health").GET());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).contains("application/json");
        assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        assertThat(response.body()).contains("\"status\":\"UP\"", "\"service\":\"voicestock-api\"", "\"timestamp\":");
    }

    @Test
    void allowsConfiguredFrontendOrigin() throws Exception {
        var response = send(request("/api/health").header("Origin", "http://localhost:5173").GET());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:5173");
    }

    @Test
    void allowsBrowserPreflight() throws Exception {
        var response = send(request("/api/health")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Access-Control-Allow-Methods").orElse("")).contains("GET");
    }

    @Test
    void rejectsUnconfiguredOrigins() throws Exception {
        var response = send(request("/api/health").header("Origin", "https://untrusted.example").GET());
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }

    @Test
    void deniesNonHealthEndpointsUntilAuthenticationIsImplemented() throws Exception {
        assertThat(send(request("/api/products").GET()).statusCode()).isEqualTo(401);
        assertThat(send(request("/actuator/env").GET()).statusCode()).isEqualTo(401);
    }

    @Test
    void deploymentHealthWorksWithoutPretendingDatabaseIsConnected() throws Exception {
        var response = send(request("/actuator/health").GET());
        assertThat(response.statusCode()).isEqualTo(200);
        JSONAssert.assertEquals("{\"status\":\"UP\"}", response.body(), false);
        assertThat(response.body()).doesNotContain("\"components\"", "\"db\"");
    }
}

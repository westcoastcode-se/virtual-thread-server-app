package se.westcoastcode;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.westcoastcode.features.JSONFeature.fromJson;
import static se.westcoastcode.features.JSONFeature.toJson;

public class AuthenticationTest {

    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres");
    private static Main main;

    @BeforeAll
    static void beforeAll() {
        postgres.start();

        var config = new Config();
        config.setDatabase(new Config.Database());
        config.getDatabase().setUrl(postgres.getJdbcUrl());
        config.getDatabase().setUsername(postgres.getUsername());
        config.getDatabase().setPassword(postgres.getPassword());
        main = new Main(config);
    }

    @AfterAll
    static void afterAll() {
        main.close();
        postgres.stop();
    }

    @Test
    @SneakyThrows
    public void verifyAuthenticationSuccess() {
        var body = toJson(new Main.AuthRequest("admin", "super-secret"));
        var request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + main.getAppConfig().getServer().getPort() + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            assertEquals(200, response.statusCode());
            var accessToken = fromJson(Main.AccessToken.class, response.body());
            assertNotNull(accessToken);
        }
    }

    @Test
    @SneakyThrows
    public void verifyAuthenticationFailed() {
        var body = toJson(new Main.AuthRequest("admin", "hell no!"));
        var request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + main.getAppConfig().getServer().getPort() + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            assertEquals(401, response.statusCode());
        }
    }
}

package se.westcoastcode;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.westcoastcode.features.JSONFeature.fromJson;
import static se.westcoastcode.features.JSONFeature.toJson;

public class AuthenticationTest {

    private static API api;

    @BeforeAll
    static void beforeAll() {
        api = new API();
        api.start();
    }

    @AfterAll
    static void afterAll() {
        api.close();
    }

    @Test
    @SneakyThrows
    public void verifyAuthenticationSuccess() {
        var body = toJson(new Main.AuthRequest("admin", "super-secret"));
        var request = HttpRequest.newBuilder()
                .uri(api.toURI("/api/v1/auth/login"))
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
                .uri(api.toURI("/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            assertEquals(401, response.statusCode());
        }
    }
}

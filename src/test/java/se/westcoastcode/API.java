package se.westcoastcode;

import lombok.SneakyThrows;
import org.testcontainers.containers.PostgreSQLContainer;
import se.westcoastcode.persistence.Employee;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.westcoastcode.features.JSONFeature.fromJson;
import static se.westcoastcode.features.JSONFeature.toJson;

/**
 * Class for helping to communicate with the main REST-server
 */
public class API {
    private final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres");
    private Main main;
    private String accessToken;

    /**
     * Login to the server and return an access token
     *
     * @return The access token
     */
    @SneakyThrows
    protected Main.AccessToken login() {
        return login("admin", "super-secret");
    }

    /**
     * Login to the server and return an access token
     *
     * @param username The username
     * @param password The password
     * @return The access token
     */
    @SneakyThrows
    protected Main.AccessToken login(final String username, final String password) {
        var body = toJson(new Main.AuthRequest(username, password));
        var request = HttpRequest.newBuilder()
                .uri(toURI("/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            assertEquals(200, response.statusCode());
            var accessToken = fromJson(Main.AccessToken.class, response.body());
            assertNotNull(accessToken);
            return accessToken;
        }
    }

    /**
     * @return The base url to the application
     */
    protected String getBaseURL() {
        return "http://localhost:" + main.getAppConfig().getServer().getPort();
    }

    /**
     * @param path The path on the server
     * @return A full URI to the server
     */
    @SneakyThrows
    protected URI toURI(final String path) {
        return new URI(getBaseURL() + path);
    }

    /**
     * Start the database and application
     */
    protected void start() {
        // Start postgres in docker
        postgres.start();

        // Start main application in memory and connect it to PostgresSQL running in docker
        var config = new Config();
        config.setDatabase(new Config.Database());
        config.getDatabase().setUrl(postgres.getJdbcUrl());
        config.getDatabase().setUsername(postgres.getUsername());
        config.getDatabase().setPassword(postgres.getPassword());
        main = new Main(config);
        main.start();
    }

    /**
     * @return A valid access token
     */
    protected String getAccessToken() {
        if (accessToken == null) {
            accessToken = login().accessToken();
        }
        return accessToken;
    }

    /**
     * Stop the application and the database
     */
    protected void close() {
        // Stop main application
        main.close();

        // Stop PostgresSQL
        postgres.stop();
    }

    /**
     * @return All employees
     */
    @SneakyThrows
    public Employee[] getEmployees() {
        var request = HttpRequest.newBuilder()
                .uri(toURI("/api/v1/employees"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + getAccessToken())
                .GET()
                .build();
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            assertEquals(200, response.statusCode());
            var employees = fromJson(Employee[].class, response.body());
            assertNotNull(employees);
            return employees;
        }
    }

    /**
     * Add a new employee
     *
     * @param employee The new employee
     * @return The added employee from the server
     */
    @SneakyThrows
    public Employee addEmployee(Employee employee) {
        var body = toJson(employee);
        var request = HttpRequest.newBuilder()
                .uri(toURI("/api/v1/employees"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + getAccessToken())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            assertEquals(201, response.statusCode());
            var employees = fromJson(Employee.class, response.body());
            assertNotNull(employees);
            return employees;
        }
    }

    /**
     * Delete the supplied employee
     *
     * @param employee The employee
     * @return The deleted employee from the server
     */
    @SneakyThrows
    public Employee deleteEmployee(Employee employee) {
        var request = HttpRequest.newBuilder()
                .uri(toURI("/api/v1/employees/" + employee.getId()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + getAccessToken())
                .DELETE()
                .build();
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            assertEquals(200, response.statusCode());
            var employees = fromJson(Employee.class, response.body());
            assertNotNull(employees);
            return employees;
        }
    }

}

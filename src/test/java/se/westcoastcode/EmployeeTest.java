package se.westcoastcode;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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

public class EmployeeTest {

    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres");
    private static Main main;
    private static Main.AccessToken accessToken;

    @SneakyThrows
    public static Main.AccessToken login() {
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
            return accessToken;
        }
    }

    @BeforeAll
    static void beforeAll() {
        // Start postgres in docker
        postgres.start();

        // Start main application in memory and connect it to PostgresSQL running in docker
        var config = new Config();
        config.setDatabase(new Config.Database());
        config.getDatabase().setUrl(postgres.getJdbcUrl());
        config.getDatabase().setUsername(postgres.getUsername());
        config.getDatabase().setPassword(postgres.getPassword());
        main = new Main(config);

        // Get access token
        accessToken = login();
    }

    @AfterAll
    static void afterAll() {
        // Stop main application
        main.close();

        // Stop PostgresSQL
        postgres.stop();
    }

    @SneakyThrows
    public static Employee[] getEmployees() {
        var request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + main.getAppConfig().getServer().getPort() + "/api/v1/employees"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken.accessToken())
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

    @SneakyThrows
    public static Employee addEmployee(Employee employee) {
        var body = toJson(employee);
        var request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + main.getAppConfig().getServer().getPort() + "/api/v1/employees"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken.accessToken())
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

    @Test
    @SneakyThrows
    public void verifyAddingAndGettingEmployees() {
        var employees = getEmployees();
        assertEquals(0, employees.length);

        var expectedEmployee = new Employee();
        expectedEmployee.setName("John Doe");
        var addedEmployee = addEmployee(expectedEmployee);
        assertNotNull(addedEmployee.getId());
        assertEquals(expectedEmployee.getName(), addedEmployee.getName());

        employees = getEmployees();
        assertEquals(1, employees.length);
        assertEquals(addedEmployee, employees[0]);
    }
}

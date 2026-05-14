package se.westcoastcode;

import com.dslplatform.json.CompiledJson;
import com.zaxxer.hikari.HikariDataSource;
import io.fusionauth.http.HTTPMethod;
import io.fusionauth.http.log.Logger;
import io.fusionauth.http.server.HTTPListenerConfiguration;
import io.fusionauth.http.server.HTTPRequest;
import io.fusionauth.http.server.HTTPResponse;
import io.fusionauth.http.server.HTTPServer;
import jakarta.validation.ConstraintViolationException;
import lombok.Getter;
import lombok.SneakyThrows;
import se.westcoastcode.persistence.Employee;
import se.westcoastcode.persistence.EmployeeRepository;

import javax.sql.DataSource;
import java.util.UUID;

import static se.westcoastcode.Config.loadConfig;
import static se.westcoastcode.Database.initDatabaseTables;
import static se.westcoastcode.features.HTTPFeatures.*;
import static se.westcoastcode.features.JSONFeature.fromJson;
import static se.westcoastcode.features.JWTFeature.authenticate;
import static se.westcoastcode.features.JWTFeature.createAccessToken;
import static se.westcoastcode.features.LoggingFeatures.LOGGING_FACTORY;
import static se.westcoastcode.features.LoggingFeatures.getLogger;

public class Main implements AutoCloseable {
    private static final Logger log = getLogger(Main.class);

    private final HTTPServer server;
    private final HTTPServer managementServer;
    private final DataSource dataSource;
    private final EmployeeRepository employeeRepository;

    @Getter
    private final Config appConfig;

    public Main(final Config appConfig) {
        this.appConfig = appConfig;

        dataSource = new HikariDataSource(appConfig.createHikariConfig());
        initDatabaseTables(dataSource);

        employeeRepository = new EmployeeRepository(dataSource);

        // Main server
        server = new HTTPServer()
                .withLoggerFactory(LOGGING_FACTORY)
                .withHandler(this::traceLogging)
                .withListener(new HTTPListenerConfiguration(appConfig.getServer().getPort()));
        server.start();

        // Management Server for metrics and performance monitoring
        var monitoring = new Monitoring();
        managementServer = new HTTPServer()
                .withLoggerFactory(LOGGING_FACTORY)
                .withHandler((req, res) -> {
                    ok(monitoring.current(), res);
                })
                .withListener(new HTTPListenerConfiguration(appConfig.getServer().getManagementPort()));
        managementServer.start();
    }

    /**
     * Entrypoint
     */
    static void main() {
        // Load configuration file based on the system property
        var config = loadConfig(System.getProperty("profile", "dev"));

        // Instantiate the main application
        new Main(config);
    }

    /**
     * Wrap all requests inside an exception block
     *
     * @param req The request
     * @param res The response
     */
    @SneakyThrows
    public void exceptionHandler(HTTPRequest req, HTTPResponse res) {
        try {
            handleRequest(req, res);
        } catch (final HttpError e) {
            status(e.createErrorMessage(), res, e.getHttpCode());
        } catch (final ConstraintViolationException e) {
            log.info("Bad request", e);
            var error = badRequest(req, e.getConstraintViolations());
            status(error.createErrorMessage(), res, error.getHttpCode());
        } catch (final Exception e) {
            log.error("Unexpected error", e);
            var error = internalServerError(req);
            status(error.createErrorMessage(), res, error.getHttpCode());
        }
    }

    /**
     * Wrap all requests inside a trace logging block
     *
     * @param req The request
     * @param res The response
     */
    public void traceLogging(HTTPRequest req, HTTPResponse res) {
        var time = System.currentTimeMillis();
        exceptionHandler(req, res);
        var elapsed = System.currentTimeMillis() - time;
        log.info("Incoming Request:'{} {}', Time:{} ms", req.getMethod(), req.getPath(), elapsed);
    }

    /**
     * The actual request handler
     *
     * @param req The request
     * @param res The response
     */
    @SneakyThrows
    public void handleRequest(HTTPRequest req, HTTPResponse res) {
        if (req.getPath().equals("/api/v1/ping")) {
            if (req.getMethod() == HTTPMethod.GET) {
                ok("pong", res);
                return;
            }
        } else if (req.getPath().equals("/api/v1/employees")) {
            if (req.getMethod() == HTTPMethod.GET) {
                var _ = authenticate(req, "test:read");
                ok(employeeRepository.findAll(), res);
                return;
            }
            if (req.getMethod() == HTTPMethod.POST) {
                var _ = authenticate(req, "test:write");
                var employee = fromJson(Employee.class, req);
                employeeRepository.insert(employee);
                created(employee, res);
                return;
            }
        } else if (req.getPath().startsWith("/api/v1/employees/")) {
            if (req.getMethod() == HTTPMethod.GET) {
                var _ = authenticate(req, "test:read");
                var id = valueFromPath(req, "/api/v1/employees/%", UUID.class);
                var employee = employeeRepository.findOne(id);
                if (employee.isEmpty()) {
                    throw notFound(req);
                }
                ok(employee.get(), res);
                return;
            } else if (req.getMethod() == HTTPMethod.DELETE) {
                var _ = authenticate(req, "test:delete");
                var id = valueFromPath(req, "/api/v1/employees/%", UUID.class);
                var employee = employeeRepository.findOne(id);
                if (employee.isEmpty()) {
                    throw notFound(req);
                }
                employeeRepository.delete(employee.get());
                ok(employee.get(), res);
                return;
            }
        } else if (req.getPath().equals("/api/v1/auth/login")) {
            if (req.getMethod() == HTTPMethod.POST) {
                var authRequest = fromJson(AuthRequest.class, req);
                // DISCLAIMER: Do not copy this. This is not production friendly!
                if (authRequest.username.equals("admin") && authRequest.password.equals("super-secret")) {
                    ok(new AccessToken(createAccessToken()), res);
                    return;
                } else {
                    throw unauthorized(req);
                }
            }
        }
        throw notFound(req);
    }

    @SneakyThrows
    @Override
    public void close() {
        if (managementServer != null) {
            managementServer.close();
        }
        if (server != null) {
            server.close();
        }
    }

    @CompiledJson
    public record AuthRequest(String username, String password) {
    }

    @CompiledJson
    public record AccessToken(String accessToken) {
    }
}

package se.westcoastcode;

import com.dslplatform.json.CompiledJson;
import com.zaxxer.hikari.HikariConfig;
import lombok.Data;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import static se.westcoastcode.features.JSONFeature.fromJson;

@Data
@CompiledJson
public class Config {

    @CompiledJson
    @Data
    public static class Database {
        private String url;
        private String username;
        private String password;
    }

    @CompiledJson
    @Data
    public static class Server {
        private short port = 4242;
        private short managementPort = 4343;
    }

    private Database database = new Database();
    private Server server = new Server();

    /**
     * @return Configuration used to connect to the database
     */
    public HikariConfig createHikariConfig() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(database.url);
        config.setUsername(database.username);
        config.setPassword(database.password);

        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(5000);
        config.setAutoCommit(false);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("DB-Thread");
        return config;
    }

    /**
     * @return The loaded configuration.
     */
    @SneakyThrows
    public static Config loadConfig(final String env) {
        // Load different configuration files based on different environments. Dev should load the configuration
        // from a file in the repository.
        //
        // In case of test, qa and prod. Those configurations should be injected into the docker container or whatever
        // virtualization method you are using
        return switch (env) {
            case "dev" -> loadConfigFromFile(Path.of("src/config/config-dev.json"));
            case "test" -> loadConfigFromFile(Path.of("/app/config-test.json"));
            case "qa" -> loadConfigFromFile(Path.of("/app/config-qa.json"));
            case "prod" -> loadConfigFromFile(Path.of("/app/config-prod.json"));
            default -> throw new RuntimeException("Unknown env: " + env);
        };
    }

    /**
     * Load configuration from a file on disk
     *
     * @param filePath The path to the file
     * @return The configuration
     */
    @SneakyThrows
    private static Config loadConfigFromFile(Path filePath) {
        try (var stream = Files.newInputStream(filePath)) {
            return fromJson(Config.class, stream);
        }
    }

    /**
     * Load configuration from the supplied resource name
     *
     * @param resourceName The resource name
     * @return The configuration
     */
    @SneakyThrows
    public static Config loadConfigFromResource(String resourceName) {
        try (var stream = Main.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (stream == null) {
                throw new RuntimeException("Missing configuration file");
            }
            return fromJson(Config.class, stream);
        }
    }
}

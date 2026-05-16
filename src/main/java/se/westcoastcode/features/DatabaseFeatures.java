package se.westcoastcode.features;

import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;

public class DatabaseFeatures {
    /**
     * Run SQL found in applications classpath
     *
     * @param dataSource   The datasource
     * @param resourcePath The resource
     */
    @SneakyThrows
    public static void sql(DataSource dataSource, String resourcePath) {
        var resource = DatabaseFeatures.class.getClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new RuntimeException(String.format("Resource %s not found", resourcePath));
        }

        var text = Files.readString(Path.of(resource.getFile()));
        try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
            try {
                stmt.execute(text);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
}

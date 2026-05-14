package se.westcoastcode;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.Statement;

/**
 * Class for configuring the database
 */
@AllArgsConstructor
public final class Database {
    @SneakyThrows
    public static void initDatabaseTables(final DataSource dataSource) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            createEmployees(stmt);
            conn.commit();
        }
    }

    @SneakyThrows
    private static void createEmployees(final Statement stmt) {
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS employee (
                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                     name VARCHAR(100)
                )""");
    }
}

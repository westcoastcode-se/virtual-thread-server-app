package se.westcoastcode.features;

import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

public class DatabaseFeatures {
    @FunctionalInterface
    public interface ExceptionalFunction<T, R> {
        R apply(T t) throws Exception;
    }

    @FunctionalInterface
    public interface ExceptionalConsumer<T> {
        void accept(T t) throws Exception;
    }

    /**
     * Run the supplied function in a transaction
     *
     * @param ds The datasource
     * @param r  The runnable
     */
    @SneakyThrows
    public static void transactional(final DataSource ds, final ExceptionalConsumer<Connection> r) {
        try (var conn = ds.getConnection()) {
            try {
                r.accept(conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Run the supplied function in a transaction
     *
     * @param ds The datasource
     * @param r  The runnable
     */
    @SneakyThrows
    public static void transactional(final DataSource ds, boolean readOnly, final ExceptionalConsumer<Connection> r) {
        try (var conn = ds.getConnection()) {
            conn.setReadOnly(readOnly);
            try {
                r.accept(conn);
                if (!readOnly) {
                    conn.commit();
                }
            } catch (Exception e) {
                if (!readOnly) {
                    conn.rollback();
                }
                throw e;
            }
        }
    }

    /**
     * Run the supplied function in a transaction
     *
     * @param ds The datasource
     * @param r  The runnable
     */
    @SneakyThrows
    public static <T> T transactional(final DataSource ds, final ExceptionalFunction<Connection, T> r) {
        final T result;
        try (var conn = ds.getConnection()) {
            try {
                result = r.apply(conn);
                conn.commit();
            } catch (final Exception e) {
                conn.rollback();
                throw e;
            }
        }
        return result;
    }

    /**
     * Run the supplied function in a transaction
     *
     * @param ds The datasource
     * @param r  The runnable
     */
    @SneakyThrows
    public static <T> T transactional(final DataSource ds, boolean readOnly, final ExceptionalFunction<Connection, T> r) {
        final T result;
        try (var conn = ds.getConnection()) {
            conn.setReadOnly(readOnly);
            try {
                result = r.apply(conn);
                if (!readOnly) {
                    conn.commit();
                }
            } catch (final Exception e) {
                if (!readOnly) {
                    conn.rollback();
                }
                throw e;
            }
        }
        return result;
    }

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
        transactional(dataSource, conn -> {
            try (var stmt = conn.createStatement()) {
                stmt.execute(text);
            }
        });
    }


}

package se.westcoastcode.features;

import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.function.Consumer;
import java.util.function.Function;

public class DatabaseFeatures {
    /**
     * Run the supplied function in a transaction
     *
     * @param ds The datasource
     * @param r  The runnable
     */
    @SneakyThrows
    public static void transactional(final DataSource ds, final Consumer<Connection> r) {
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
    public static void transactional(final DataSource ds, boolean readOnly, final Consumer<Connection> r) {
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
    public static <T> T transactional(final DataSource ds, final Function<Connection, T> r) {
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
    public static <T> T transactional(final DataSource ds, boolean readOnly, final Function<Connection, T> r) {
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
}

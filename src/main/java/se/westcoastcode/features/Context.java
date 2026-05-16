package se.westcoastcode.features;

import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.Connection;

import static se.westcoastcode.features.UUIDFeatures.newUUID;

/**
 * Context that contains the necessary features for a function to work within a virtual thread.
 * <p>
 * Basically ThreadLocals are a big no-no when using virtual threads. My other, official, option is ScopedValue, but those
 * are pretty much global variables.
 *
 */
public final class Context {
    private final DataSource dataSource;
    private final String traceId;
    private final Connection connection;

    public Context(DataSource dataSource, String traceId, Connection connection) {
        this.dataSource = dataSource;
        this.traceId = traceId;
        this.connection = connection;
    }

    public Context(DataSource dataSource) {
        this(dataSource, newUUID().toString(), null);
    }

    @FunctionalInterface
    public interface ExceptionalFunction<T, R> {
        R apply(T t, Connection conn) throws Exception;
    }

    @FunctionalInterface
    public interface ExceptionalConsumer<T> {
        void accept(T t, Connection conn) throws Exception;
    }

    /**
     * Run the supplied function in a transaction
     * <p>
     * Any opened connection will automatically be closed if it's inside the expression
     *
     * @param r The function to be executed in a transaction. Argument will contain a context
     *          with an opened connection
     */
    @SneakyThrows
    public void transactional(final ExceptionalConsumer<Context> r) {
        if (connection != null) {
            // Uplift connection to disable readonly
            var readOnly = connection.isReadOnly();
            if (readOnly) {
                connection.setReadOnly(true);
            }
            r.accept(this, connection);
            return;
        }

        try (var conn = dataSource.getConnection()) {
            try {
                r.accept(new Context(dataSource, traceId, conn), conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Run the supplied function in a transaction
     * <p>
     * Any opened connection will automatically be closed if it's inside the expression
     *
     * @param r The function to be executed in a transaction. Argument will contain a context
     *          with an opened connection
     */
    @SneakyThrows
    public <T> T transactional(final ExceptionalFunction<Context, T> r) {
        if (connection != null) {
            // Uplift connection to disable readonly
            var readOnly = connection.isReadOnly();
            if (readOnly) {
                connection.setReadOnly(true);
            }
            return r.apply(this, connection);
        }

        final T result;
        try (var conn = dataSource.getConnection()) {
            try {
                result = r.apply(new Context(dataSource, traceId, conn), conn);
                conn.commit();
            } catch (final Exception e) {
                conn.rollback();
                throw e;
            }
        }
        return result;
    }


    /**
     * Run the supplied function in a transaction. The transaction might be read-only. It depends on if it's
     * inside another transaction that's read or writable
     * <p>
     * Any opened connection will automatically be closed if it's inside the expression
     *
     * @param r The function to be executed in a transaction. Argument will contain a context
     *          with an opened connection
     */
    @SneakyThrows
    public void readonly(final ExceptionalConsumer<Context> r) {
        if (connection != null) {
            r.accept(this, connection);
            return;
        }

        try (var conn = dataSource.getConnection()) {
            try {
                conn.setReadOnly(true);
                r.accept(new Context(dataSource, traceId, conn), conn);
                // It might not be readonly anymore if multiple "transactions" are present
                if (!conn.isReadOnly()) {
                    conn.commit();
                }
            } catch (Exception e) {
                if (!conn.isReadOnly()) {
                    conn.rollback();
                }
                throw e;
            }
        }
    }

    /**
     * Run the supplied function in a transaction. The transaction might be read-only. It depends on if it's
     * inside another transaction that's read or writable
     * <p>
     * Any opened connection will automatically be closed if it's inside the expression
     *
     * @param r The runnable
     */
    @SneakyThrows
    public <T> T readonly(final ExceptionalFunction<Context, T> r) {
        if (connection != null) {
            return r.apply(this, connection);
        }

        final T result;
        try (var conn = dataSource.getConnection()) {
            try {
                conn.setReadOnly(true);
                result = r.apply(new Context(dataSource, traceId, conn), conn);
                // It might not be readonly anymore if multiple "transactions" are present
                if (!conn.isReadOnly()) {
                    conn.commit();
                }
            } catch (final Exception e) {
                if (!conn.isReadOnly()) {
                    conn.rollback();
                }
                throw e;
            }
        }
        return result;
    }
}
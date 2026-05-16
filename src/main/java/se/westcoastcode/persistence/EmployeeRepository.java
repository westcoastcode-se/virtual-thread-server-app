package se.westcoastcode.persistence;

import io.fusionauth.http.log.Logger;
import lombok.SneakyThrows;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import se.westcoastcode.features.Context;

import javax.sql.DataSource;
import java.io.EOFException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static se.westcoastcode.features.LoggingFeatures.getLogger;
import static se.westcoastcode.features.ValidationFeatures.validate;

public class EmployeeRepository {
    private static final Logger log = getLogger(EmployeeRepository.class);

    /**
     * Get all employees
     *
     * @param context The context
     * @return A list of all employees
     */
    @SneakyThrows
    public List<Employee> findAll(final Context context) {
        var query = "SELECT * FROM employee";

        return context.readonly((_, conn) -> {
            try (final Statement ps = conn.createStatement()) {
                var result = new ArrayList<Employee>();
                try (final ResultSet rs = ps.executeQuery(query)) {
                    while (rs.next()) {
                        result.add(new Employee(
                                (UUID) rs.getObject("id"),
                                rs.getString("name")
                        ));
                    }
                }
                return result;
            }
        });
    }

    /**
     * Find a single employee
     *
     * @param context The context
     * @param id      The employee id
     * @return The employee if found; Optional.empty() otherwise
     */
    @SneakyThrows
    public Optional<Employee> findOne(final Context context, final UUID id) {
        var query = "SELECT * FROM employee WHERE id = ?";

        return context.readonly((_, conn) -> {
            try (final PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setObject(1, id);
                try (final ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new Employee(
                                (UUID) rs.getObject("id"),
                                rs.getString("name")
                        ));
                    } else {
                        return Optional.empty();
                    }
                }
            }
        });
    }

    /**
     * @return All employee ids in the database
     */
    @SneakyThrows
    public List<UUID> findAllIds(final Context context) {
        var query = "SELECT id FROM employee";

        return context.readonly((_, conn) -> {
            try (final Statement ps = conn.createStatement()) {
                var result = new ArrayList<UUID>();
                try (final ResultSet rs = ps.executeQuery(query)) {
                    while (rs.next()) {
                        result.add((UUID) rs.getObject("id"));
                    }
                }
                return result;
            }
        });
    }

    /**
     * Add a new employee. This will automatically notify all subscribers
     *
     * @param context  The context
     * @param employee The employee
     */
    @SneakyThrows
    public void insert(final Context context, final Employee employee) {
        validate(employee);

        var query = "INSERT INTO employee (name) VALUES (?)";

        context.transactional((_, conn) -> {
            try (final PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, employee.getName());
                if (ps.executeUpdate() != 1) {
                    throw new RuntimeException("Failed to insert employee");
                }
                try (final ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        employee.setId((UUID) rs.getObject("id"));
                    }
                }
            }
        });
    }

    /**
     * Delete the supplied employee
     *
     * @param context  The context
     * @param employee The employee
     */
    public void delete(final Context context, final Employee employee) {
        if (!deleteById(context, employee.getId())) {
            throw new RuntimeException("Failed to delete " + employee);
        }
    }

    /**
     * Delete an employee with the supplied id
     *
     * @param context The context
     * @param id      The employee id
     * @return true if removing the employee was successful
     */
    @SneakyThrows
    public boolean deleteById(final Context context, final UUID id) {
        var query = "DELETE FROM employee WHERE id = ?";

        return context.transactional((_, conn) -> {
            try (final PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setObject(1, id);
                if (ps.executeUpdate() != 1) {
                    return false;
                }
            }
            return true;
        });
    }

    /**
     * Create an Employee subscribe against the database
     *
     * @param added   Called when an employee is added
     * @param updated Called when an employee is updated
     * @param deleted Called when an employee is deleted
     * @return a closable subscribe
     */
    public AutoCloseable subscribe(DataSource dataSource, Consumer<UUID> added, Consumer<UUID> updated, Consumer<UUID> deleted) {
        var thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    doSubscribe(dataSource, added, updated, deleted);
                } catch (Exception e) {
                    log.error("Error while subscribing", e);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
            }
            log.info("Subscriber thread exiting");
        });
        thread.setName("EmployeeRepository-Subscriber");
        thread.start();
        return () -> {
            log.info("Closing subscribe");
            thread.interrupt();
            log.info("Subscriber closed");
        };
    }

    /**
     * Subscribe for changes in the employee table
     *
     * @param added   Called when an employee is added
     * @param updated Called when an employee is updated
     * @param deleted Called when an employee is deleted
     */
    @SneakyThrows
    private void doSubscribe(DataSource dataSource, Consumer<UUID> added, Consumer<UUID> updated, Consumer<UUID> deleted) {
        var query = "LISTEN employee_changed";
        try (var conn = dataSource.getConnection()) {
            var ps = conn.createStatement();
            ps.execute(query);
            ps.close();
            conn.commit();

            while (!Thread.currentThread().isInterrupted()) {
                var pgConnection = conn.unwrap(PGConnection.class);
                var notifications = pgConnection.getNotifications(10000);
                if (notifications != null && notifications.length > 0) {
                    log.trace("{} notifications received", notifications.length);
                    for (PGNotification notification : notifications) {
                        var parts = notification.getParameter().split(":");
                        if (parts.length != 3) {
                            continue;
                        }
                        var action = parts[1];
                        var id = UUID.fromString(parts[2]);
                        switch (action) {
                            case "I" -> added.accept(id);
                            case "U" -> updated.accept(id);
                            case "D" -> deleted.accept(id);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (e.getCause() instanceof EOFException && Thread.currentThread().isInterrupted()) {
                // This exception happens when thread is interrupted sleeping connections
                // are woken up because of it
                return;
            }
            throw e;
        }
    }
}

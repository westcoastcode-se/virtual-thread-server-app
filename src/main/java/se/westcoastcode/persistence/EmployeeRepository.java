package se.westcoastcode.persistence;

import io.fusionauth.http.log.Logger;
import lombok.SneakyThrows;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

import javax.sql.DataSource;
import java.io.EOFException;
import java.sql.Connection;
import java.sql.SQLException;
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
    private final DataSource dataSource;

    public EmployeeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @SneakyThrows
    public List<Employee> findAll() {
        var query = "SELECT id, name FROM employee";

        try (var conn = dataSource.getConnection(); var ps = conn.createStatement()) {
            var result = new ArrayList<Employee>();
            try (var rs = ps.executeQuery(query)) {
                while (rs.next()) {
                    result.add(new Employee(
                            (UUID) rs.getObject("id"),
                            rs.getString("name")
                    ));
                }
            }
            return result;
        }
    }

    /**
     * Get an employee based on its unique ID
     *
     * @param id The employee id
     * @return The employee if found; Optional.empty() otherwise
     */
    @SneakyThrows
    public Optional<Employee> findOne(final UUID id) {
        try (var conn = dataSource.getConnection()) {
            return findOne(conn, id);
        }
    }


    @SneakyThrows
    private Optional<Employee> findOne(final Connection conn, final UUID id) {
        var query = "SELECT id, name FROM employee WHERE id = ?";

        try (var ps = conn.prepareStatement(query)) {
            ps.setObject(1, id);
            try (var rs = ps.executeQuery()) {
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
    }

    @SneakyThrows
    public List<UUID> findAllIds() {
        var query = "SELECT id FROM employee";

        try (var conn = dataSource.getConnection(); var ps = conn.createStatement()) {
            var result = new ArrayList<UUID>();
            try (var rs = ps.executeQuery(query)) {
                while (rs.next()) {
                    result.add((UUID) rs.getObject("id"));
                }
            }
            return result;
        }
    }

    @SneakyThrows
    public void insert(final Employee employee) {
        validate(employee);

        var query = "INSERT INTO employee (name) VALUES (?)";

        try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, employee.getName());
            if (ps.executeUpdate() != 1) {
                throw new RuntimeException("Failed to insert employee");
            }
            conn.commit();
            try (var rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    employee.setId((UUID) rs.getObject("id"));
                }
            }
        }
    }

    /**
     * Delete the supplied employee
     *
     * @param employee The employee
     */
    public void delete(final Employee employee) {
        if (!deleteById(employee.getId())) {
            throw new RuntimeException("Failed to delete " + employee);
        }
    }

    /**
     * Delete an employee with the supplied id
     *
     * @param id The employee id
     * @return true if removing the employee was successful
     */
    @SneakyThrows
    public boolean deleteById(final UUID id) {
        var query = "DELETE FROM employee WHERE id = ?";

        try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(query)) {
            ps.setObject(1, id);
            if (ps.executeUpdate() != 1) {
                return false;
            }
            conn.commit();
        }
        return true;
    }

    /**
     * Create an Employee subscriber against the database
     *
     * @param added   Called when an employee is added
     * @param updated Called when an employee is updated
     * @param deleted Called when an employee is deleted
     * @return a closable subscriber
     */
    public AutoCloseable subscriber(Consumer<Employee> added, Consumer<Employee> updated, Consumer<UUID> deleted) {
        var thread = new Thread(() -> subscribe(added, updated, deleted));
        thread.start();
        return () -> {
            log.info("Closing subscriber");
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
    public void subscribe(Consumer<Employee> added, Consumer<Employee> updated, Consumer<UUID> deleted) {
        var query = "LISTEN employee_changed";

        try (var conn = dataSource.getConnection()) {
            var ps = conn.createStatement();
            ps.execute(query);
            ps.close();
            conn.commit();

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    var pgConnection = conn.unwrap(PGConnection.class);
                    var notifications = pgConnection.getNotifications(30000);
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
                                case "I" -> {
                                    var employee = findOne(id);
                                    employee.ifPresent(added);
                                }
                                case "U" -> {
                                    var employee = findOne(id);
                                    employee.ifPresent(updated);
                                }
                                case "D" -> deleted.accept(id);
                            }
                        }
                    }
                } catch (SQLException e) {
                    if (e.getCause() instanceof EOFException && Thread.currentThread().isInterrupted()) {
                        // This exception happens when thread is interrupted sleeping connections
                        // are woken up because of it
                    } else {
                        log.error("Unhandled SQL exception", e);
                    }
                }
            }
        }
        log.info("Subscriber thread exiting");
    }
}

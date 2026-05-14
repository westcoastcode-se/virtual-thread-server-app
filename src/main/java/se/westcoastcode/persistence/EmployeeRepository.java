package se.westcoastcode.persistence;

import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static se.westcoastcode.features.ValidationFeatures.validate;

public class EmployeeRepository {
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

    @SneakyThrows
    public Optional<Employee> findOne(final UUID id) {
        var query = "SELECT id, name FROM employee WHERE id = ?";

        try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(query)) {
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
}

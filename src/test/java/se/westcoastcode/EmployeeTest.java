package se.westcoastcode;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.westcoastcode.persistence.Employee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EmployeeTest {

    private static API api;

    @BeforeAll
    static void beforeAll() {
        api = new API();
        api.start();
    }

    @AfterAll
    static void afterAll() {
        api.close();
    }

    @Test
    public void verifyAddingAndDeletingEmployee() {
        // Add a new employee
        var expectedEmployee = new Employee();
        expectedEmployee.setName("John Doe");
        var addedEmployee = api.addEmployee(expectedEmployee);
        assertNotNull(addedEmployee.getId());
        assertEquals(expectedEmployee.getName(), addedEmployee.getName());

        // Verify that the employee was added
        var employees = api.getEmployees();
        assertEquals(1, employees.length);
        assertEquals(addedEmployee, employees[0]);

        // Delete the employee
        var deletedEmployee = api.deleteEmployee(addedEmployee);
        assertEquals(addedEmployee, deletedEmployee);

        // Verify that the employee is deleted
        employees = api.getEmployees();
        assertEquals(0, employees.length);
    }
}

package com.pamu.repository;

import com.pamu.model.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ExtendWith(SpringExtension.class)
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    @DisplayName("Should save and find employee by id")
    void testSaveAndFindById() {
        Employee emp = new Employee();
        emp.setId(1L);
        emp.setName("Ravi");
        emp.setCity("Hyderabad");
        emp.setState("Telangana");
        emp.setCategory("employee");
        emp.setSalary(45000.0);
        emp.setDoj(LocalDate.of(2023, 6, 4));
        Employee saved = employeeRepository.save(emp);
        Optional<Employee> found = employeeRepository.findById(Math.toIntExact(saved.getId()));
        assertTrue(found.isPresent());
        assertEquals("Ravi", found.get().getName());
    }

    @Test
    @DisplayName("Should find all employees")
    void testFindAll() {
        Employee emp1 = new Employee();
        emp1.setId(1L);
        emp1.setName("Ravi");
        emp1.setCity("Hyderabad");
        emp1.setState("Telangana");
        emp1.setCategory("employee");
        emp1.setSalary(45000.0);
        emp1.setDoj(LocalDate.of(2023, 6, 4));
        Employee emp2 = new Employee();
        emp2.setId(2L);
        emp2.setName("Shivam");
        emp2.setCity("Bangalore");
        emp2.setState("Karnataka");
        emp2.setCategory("manager");
        emp2.setSalary(75000.0);
        emp2.setDoj(LocalDate.of(2022, 7, 5));
        employeeRepository.save(emp1);
        employeeRepository.save(emp2);
        List<Employee> all = employeeRepository.findAll();
        assertTrue(all.size() >= 2);
    }
}


package com.pamu.repository;

import com.pamu.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    @Query(value = "SELECT * FROM employees ORDER BY salary DESC LIMIT 1 OFFSET :n", nativeQuery = true)
    Employee findNthHighestSalary(int n);
}

package com.pamu.controller;

import com.pamu.dto.EmployeeDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.pamu.service.EmployeeService;

import java.io.File;
import java.io.IOException;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    /**
     * Processes an uploaded file to extract employee data, generate additional records,
     * and store all data in the database. Returns a downloadable Excel file of the processed data.
     *
     * @param file the uploaded file containing employee data
     * @return ResponseEntity with the processed Excel file as a downloadable resource
     * @throws IOException if file processing or writing fails
     */
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> processEmployeeFile(@RequestParam("file") MultipartFile file) throws IOException {
        // Process file: extract employees, generate 50 more, and insert into database
        File processedFile = employeeService.processAndDownloadEmployees(file);
        Resource resource = new FileSystemResource(processedFile);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employees.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }

    /**
     * Retrieves a paginated list of all employees.
     * This method is developed to support employee management and reporting.
     *
     * @param page the page number to retrieve (zero-based)
     * @param size the number of employees per page
     * @param sortBy the field by which to sort the employees
     * @return a Page of EmployeeDTOs containing the employee data
     */
    @GetMapping("/employees")
    public Page<EmployeeDTO> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return employeeService.getAllEmployees(pageable);
    }

    /**
     * Returns a list of employees eligible for gratuity.
     * This method is developed to support compliance and financial planning.
     *
     * @return ResponseEntity with a list of EmployeeDTOs
     */
    @GetMapping("/gratuity-eligibility")
    public ResponseEntity<List<EmployeeDTO>> getGratuityEligibleEmployees() {
        return ResponseEntity.ok(employeeService.getGratuityEligibleEmployees());
    }

    /**
     * Returns a list of employees whose salary is higher than their manager's salary.
     * This method is developed to support analytics and reporting on salary structure.
     *
     * @return ResponseEntity with a list of EmployeeDTOs
     */
    @GetMapping("/higher-salary-than-manager")
    public ResponseEntity<List<EmployeeDTO>> getEmployeesWithHigherSalaryThanManager() {
        return ResponseEntity.ok(employeeService.getEmployeesWithHigherSalaryThanManager());
    }

    /**
     * Returns the employee with the Nth highest salary.
     * This method is developed to support leaderboard, analytics, and compensation benchmarking.
     *
     * @param n The rank (1-based) for the highest salary
     * @return EmployeeDTO with the Nth highest salary, or null if not found
     */
    @GetMapping("/nth-highest-salary")
    public EmployeeDTO getNthHighestSalary(@RequestParam int n) {
        return employeeService.getNthHighestSalaryEmployee(n);
    }

    /**
     * Generates and returns the employee hierarchy for a given manager as a downloadable JSON file.
     * This method is developed to support org chart visualization and reporting.
     *
     * @param managerId The ID of the manager whose hierarchy is requested
     * @return ResponseEntity with the hierarchy JSON file as a downloadable resource
     * @throws IOException if file writing fails
     */
    @GetMapping("/hierarchy/download/{managerId}")
    public ResponseEntity<Resource> getEmployeeHierarchyByManager(@PathVariable Long managerId) throws IOException {
        File jsonFile = employeeService.getEmployeeHierarchyByManager(managerId);
        Resource resource = new FileSystemResource(jsonFile);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + jsonFile.getName())
                .body(resource);
    }
}

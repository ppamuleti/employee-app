package com.pamu.controller;

import com.pamu.dto.EmployeeDTO;
import com.pamu.model.Employee;
import com.pamu.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmployeeControllerTest {

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private EmployeeController employeeController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllEmployees() {
        EmployeeDTO emp1 = new EmployeeDTO();
        emp1.setId(1L);
        emp1.setName("Ravi");
        EmployeeDTO emp2 = new EmployeeDTO();
        emp2.setId(2L);
        emp2.setName("Shivam");
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));
        Page<EmployeeDTO> page = new PageImpl<>(List.of(emp1, emp2));
        when(employeeService.getAllEmployees(pageable)).thenReturn(page);

        Page<EmployeeDTO> result = employeeController.getAllEmployees(0, 10, "id");
        assertEquals(2, result.getContent().size());
        assertEquals("Ravi", result.getContent().get(0).getName());
        assertEquals("Shivam", result.getContent().get(1).getName());
    }

    @Test
    void testGetGratuityEligibleEmployees() {
        EmployeeDTO emp1 = new EmployeeDTO();
        emp1.setId(1L);
        emp1.setName("Ravi");
        when(employeeService.getGratuityEligibleEmployees()).thenReturn(List.of(emp1));
        ResponseEntity<List<EmployeeDTO>> response = employeeController.getGratuityEligibleEmployees();
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals("Ravi", response.getBody().get(0).getName());
    }

    @Test
    void testProcessEmployeeFile_returnsExcelResource() throws Exception {
        // Prepare a mock MultipartFile with a real Excel file
        java.nio.file.Path excelPath = java.nio.file.Paths.get("src/test/resources/Employee.xlsx");
        byte[] excelBytes = java.nio.file.Files.readAllBytes(excelPath);
        MockMultipartFile mockFile = new MockMultipartFile("file", "Employee.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

        // Prepare a temp file to simulate the processed file
        File tempFile = File.createTempFile("processed-employees", ".xlsx");
        when(employeeService.processAndDownloadEmployees(any(MultipartFile.class))).thenReturn(tempFile);

        ResponseEntity<Resource> response = employeeController.processEmployeeFile(mockFile);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("employees.xlsx"));
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", response.getHeaders().getContentType().toString());
        // Clean up
        tempFile.delete();
    }

    @Test
    void testGetEmployeesWithHigherSalaryThanManager_returnsList() {
        EmployeeDTO emp1 = new EmployeeDTO();
        emp1.setId(1L);
        emp1.setName("Ravi");
        when(employeeService.getEmployeesWithHigherSalaryThanManager()).thenReturn(List.of(emp1));
        ResponseEntity<List<EmployeeDTO>> response = employeeController.getEmployeesWithHigherSalaryThanManager();
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Ravi", response.getBody().get(0).getName());
    }

    @Test
    void testGetNthHighestSalary_returnsEmployeeDTO() {
        EmployeeDTO emp = new EmployeeDTO();
        emp.setId(1L);
        emp.setName("Ravi");
        when(employeeService.getNthHighestSalaryEmployee(2)).thenReturn(emp);
        EmployeeDTO result = employeeController.getNthHighestSalary(2);
        assertNotNull(result);
        assertEquals("Ravi", result.getName());
    }

    @Test
    void testGetEmployeeHierarchyByManager_returnsJsonResource() throws Exception {
        File tempJson = File.createTempFile("employee_hierarchy_", ".json");
        when(employeeService.getEmployeeHierarchyByManager(1L)).thenReturn(tempJson);
        ResponseEntity<Resource> response = employeeController.getEmployeeHierarchyByManager(1L);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains(tempJson.getName()));
        tempJson.delete();
    }

    // Add more tests for other controller methods as needed
}

package com.pamu.service;

import com.pamu.dto.EmployeeDTO;
import com.pamu.model.Employee;
import com.pamu.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetGratuityEligibleEmployees_onlyReturnsThoseWithDOJGreaterThan60Months() {
        // Employee eligible (6 years ago)
        Employee eligible = new Employee();
        eligible.setId(1L);
        eligible.setName("Sneha");
        eligible.setSalary(70000.0);
        eligible.setCategory("employee");
        eligible.setDoj(LocalDate.now().minusMonths(72));

        Employee manager = new Employee();
        manager.setId(10L);
        eligible.setManager(manager);

        // Employee not eligible (less than 5 years)
        Employee ineligible = new Employee();
        ineligible.setId(2L);
        ineligible.setName("Rahul");
        ineligible.setSalary(50000.0);
        ineligible.setCategory("employee");
        ineligible.setDoj(LocalDate.now().minusMonths(36));
        ineligible.setManager(null);

        when(employeeRepository.findAll()).thenReturn(List.of(eligible, ineligible));

        List<EmployeeDTO> result = employeeService.getGratuityEligibleEmployees();

        assertEquals(1, result.size());
        EmployeeDTO dto = result.get(0);
        assertEquals("Sneha", dto.getName());
        assertEquals(10L, dto.getManagerId());
        assertTrue(dto.getDoj().isBefore(LocalDate.now().minusYears(5)));
    }

    @Test
    void testGetNthHighestSalaryEmployee_returnsValidDTO() {
        // Arrange
        Employee manager = new Employee();
        manager.setId(99L);

        Employee emp = new Employee();
        emp.setId(1L);
        emp.setName("Meera");
        emp.setSalary(120000.0);
        emp.setCategory("Manager");
        emp.setDoj(LocalDate.of(2018, 5, 10));
        emp.setManager(manager);

        when(employeeRepository.findNthHighestSalary(0)).thenReturn(emp);

        // Act
        EmployeeDTO dto = employeeService.getNthHighestSalaryEmployee(1); // n=1 → 0-based offset

        // Assert
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Meera", dto.getName());
        assertEquals(120000, dto.getSalary());
        assertEquals("Manager", dto.getCategory());
        assertEquals(99L, dto.getManagerId());
    }

    @Test
    void testGetNthHighestSalaryEmployee_returnsNullWhenNotFound() {
        when(employeeRepository.findNthHighestSalary(3)).thenReturn(null);
        EmployeeDTO result = employeeService.getNthHighestSalaryEmployee(4);
        assertNull(result);
    }

    @Test
    void testGetNthHighestSalaryEmployee_throwsExceptionForInvalidN() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> employeeService.getNthHighestSalaryEmployee(0));
        assertEquals("Rank must be >= 1", ex.getMessage());
    }

    @Test
    void testProcessAndDownloadEmployees_returnsFileAndCallsRepository() throws Exception {
        // Use the provided Excel file from test resources
        java.nio.file.Path excelPath = java.nio.file.Paths.get("src/test/resources/Employee.xlsx");
        byte[] excelBytes = java.nio.file.Files.readAllBytes(excelPath);
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(excelBytes));
        Employee emp = new Employee();
        emp.setId(1L);
        emp.setName("Test");
        when(employeeRepository.findAll()).thenReturn(Collections.singletonList(emp));

        File result = employeeService.processAndDownloadEmployees(mockFile);

        assertNotNull(result);
        assertTrue(result.exists());
        verify(employeeRepository, atLeastOnce()).findAll();
    }

    /*@Test
    void testProcessAndDownloadEmployees_handlesIOException() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getInputStream()).thenThrow(new IOException("Test IO Exception"));
        assertThrows(IOException.class, () -> employeeService.processAndDownloadEmployees(mockFile));
    }*/

    @Test
    void testImportEmployeeData_savesEmployeesAndManagers() throws Exception {
        java.nio.file.Path excelPath = java.nio.file.Paths.get("src/test/resources/Employee.xlsx");
        try (InputStream is = java.nio.file.Files.newInputStream(excelPath)) {
            // Mock repository to just return the input list
            when(employeeRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(employeeRepository).flush();

            employeeService.importEmployeeData(is);

            // At least two saveAll calls: one for initial save, one for manager update
            verify(employeeRepository, atLeast(2)).saveAll(anyList());
            verify(employeeRepository, atLeastOnce()).flush();
        }
    }

    @Test
    void testParseExcel_parsesEmployeesAndHierarchyCorrectly() throws Exception {
        java.nio.file.Path excelPath = java.nio.file.Paths.get("src/test/resources/Employee.xlsx");
        try (InputStream is = java.nio.file.Files.newInputStream(excelPath)) {
            // Use reflection to access the private parseExcel method
            java.lang.reflect.Method method = EmployeeService.class.getDeclaredMethod("parseExcel", InputStream.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Long, EmployeeService.EmployeeWrapper> result = (Map<Long, EmployeeService.EmployeeWrapper>) method.invoke(employeeService, is);

            // Basic assertions: map is not empty and contains expected keys
            assertNotNull(result);
            assertFalse(result.isEmpty());
            // Check at least one employee from the Excel file exists
            assertTrue(result.keySet().stream().anyMatch(id -> id == 123L)); // e.g., Ravi
            // Check that a Director exists (either from file or synthetic)
            boolean hasDirector = result.values().stream().anyMatch(w ->
                w.employee != null && "Director".equalsIgnoreCase(w.employee.getCategory())
            );
            assertTrue(hasDirector);
        }
    }

    @Test
    void testWriteEmployeesToExcel_createsValidExcelFile() throws Exception {
        // Prepare a list of employees
        Employee director = new Employee();
        director.setId(1L);
        director.setName("Rama");
        director.setCity("chennai");
        director.setState("Tamilnadu");
        director.setCategory("Director");
        director.setSalary(150000.0);
        director.setDoj(LocalDate.of(2022, 10, 25));
        director.setManager(null);

        Employee manager = new Employee();
        manager.setId(2L);
        manager.setName("Shivam");
        manager.setCity("bangalore");
        manager.setState("karnataka");
        manager.setCategory("manager");
        manager.setSalary(75000.0);
        manager.setDoj(LocalDate.of(2022, 7, 5));
        manager.setManager(director);

        Employee emp = new Employee();
        emp.setId(3L);
        emp.setName("Ravi");
        emp.setCity("hyderabad");
        emp.setState("Telangana");
        emp.setCategory("employee");
        emp.setSalary(45000.0);
        emp.setDoj(LocalDate.of(2023, 6, 4));
        emp.setManager(manager);

        List<Employee> employees = List.of(director, manager, emp);

        // Use reflection to access the private writeEmployeesToExcel method
        java.lang.reflect.Method method = EmployeeService.class.getDeclaredMethod("writeEmployeesToExcel", List.class);
        method.setAccessible(true);
        File excelFile = (File) method.invoke(employeeService, employees);

        assertNotNull(excelFile);
        assertTrue(excelFile.exists());
        // Validate the Excel file content
        try (InputStream is = new java.io.FileInputStream(excelFile)) {
            org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(is);
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            // Check header row
            org.apache.poi.ss.usermodel.Row header = sheet.getRow(0);
            assertEquals("ID", header.getCell(0).getStringCellValue());
            assertEquals("Name", header.getCell(1).getStringCellValue());
            assertEquals("Manager ID", header.getCell(5).getStringCellValue());
            // Check data rows
            org.apache.poi.ss.usermodel.Row row1 = sheet.getRow(1);
            assertEquals(1L, (long) row1.getCell(0).getNumericCellValue());
            assertEquals("Rama", row1.getCell(1).getStringCellValue());
            assertEquals(0, (long) row1.getCell(5).getNumericCellValue()); // Director has no manager
            org.apache.poi.ss.usermodel.Row row2 = sheet.getRow(2);
            assertEquals(2L, (long) row2.getCell(0).getNumericCellValue());
            assertEquals("Shivam", row2.getCell(1).getStringCellValue());
            assertEquals(1L, (long) row2.getCell(5).getNumericCellValue()); // Manager's manager is Director
            org.apache.poi.ss.usermodel.Row row3 = sheet.getRow(3);
            assertEquals(3L, (long) row3.getCell(0).getNumericCellValue());
            assertEquals("Ravi", row3.getCell(1).getStringCellValue());
            assertEquals(2L, (long) row3.getCell(5).getNumericCellValue()); // Employee's manager is Manager
            workbook.close();
        }
        // Clean up
        excelFile.delete();
    }

    @Test
    void testGetEmployeesWithHigherSalaryThanManager_returnsCorrectEmployees() {
        Employee director = new Employee();
        director.setId(1L);
        director.setName("Rama");
        director.setCategory("Director");
        director.setSalary(150000.0);
        director.setDoj(LocalDate.of(2022, 10, 25));

        Employee manager = new Employee();
        manager.setId(2L);
        manager.setName("Shivam");
        manager.setCategory("manager");
        manager.setSalary(75000.0);
        manager.setDoj(LocalDate.of(2022, 7, 5));
        manager.setManager(director);

        Employee emp1 = new Employee();
        emp1.setId(3L);
        emp1.setName("Ravi");
        emp1.setCategory("employee");
        emp1.setSalary(80000.0); // Higher than manager
        emp1.setDoj(LocalDate.of(2023, 6, 4));
        emp1.setManager(manager);

        Employee emp2 = new Employee();
        emp2.setId(4L);
        emp2.setName("Krishna");
        emp2.setCategory("employee");
        emp2.setSalary(70000.0); // Lower than manager
        emp2.setDoj(LocalDate.of(2021, 3, 7));
        emp2.setManager(manager);

        when(employeeRepository.findAll()).thenReturn(List.of(director, manager, emp1, emp2));

        List<EmployeeDTO> result = employeeService.getEmployeesWithHigherSalaryThanManager();

        assertEquals(1, result.size());
        EmployeeDTO dto = result.get(0);
        assertEquals(emp1.getId(), dto.getId());
        assertEquals(emp1.getName(), dto.getName());
        assertEquals(emp1.getSalary(), dto.getSalary());
        assertEquals(manager.getId(), dto.getManagerId());
    }

    @Test
    void testGetEmployeeHierarchyByManager_createsJsonFile() throws Exception {
        // Setup a simple hierarchy: Director -> Manager -> Employee
        Employee director = new Employee();
        director.setId(1L);
        director.setName("Rama");
        director.setCategory("Director");
        director.setSalary(150000.0);

        Employee manager = new Employee();
        manager.setId(2L);
        manager.setName("Shivam");
        manager.setCategory("manager");
        manager.setSalary(75000.0);
        manager.setManager(director);

        Employee emp = new Employee();
        emp.setId(3L);
        emp.setName("Ravi");
        emp.setCategory("employee");
        emp.setSalary(45000.0);
        emp.setManager(manager);

        when(employeeRepository.findAll()).thenReturn(List.of(director, manager, emp));

        File jsonFile = employeeService.getEmployeeHierarchyByManager(1L);

        assertNotNull(jsonFile);
        assertTrue(jsonFile.exists());
        // Optionally, check the file content contains the manager's name
        String content = java.nio.file.Files.readString(jsonFile.toPath());
        assertTrue(content.contains("Rama"));
        assertTrue(content.contains("Shivam"));
        assertTrue(content.contains("Ravi"));
        // Clean up
        jsonFile.delete();
    }

}

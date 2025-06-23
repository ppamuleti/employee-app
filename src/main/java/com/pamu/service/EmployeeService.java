package com.pamu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pamu.dto.EmployeeDTO;
import com.pamu.exception.EmployeeNotFoundException;
import com.pamu.exception.FileProcessingException;
import com.pamu.exception.InvalidEmployeeDataException;
import com.pamu.model.Employee;
import com.pamu.model.EmployeeNode;
import com.pamu.repository.EmployeeRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    @Autowired EmployeeRepository employeeRepository;

    /**
     * Processes the uploaded employee Excel file, imports the data, and returns a downloadable Excel file.
     * This method is developed to provide a single entry point for file upload, processing, and export.
     * @param file Multipart Excel file containing employee data
     * @return File containing processed employee data
     */
    public File processAndDownloadEmployees(MultipartFile file) {
        try {
            importEmployeeData(file.getInputStream());
            return writeEmployeesToExcel(getAllEmployeesFromCache());
        } catch (IOException e) {
            throw new FileProcessingException("Failed to process employee file", e);
        }
    }

    /**
     * Imports employee data from an InputStream (Excel file), parses, validates, and persists it.
     * This method is developed to support bulk employee import and manager relationship setup.
     * @param inputStream InputStream of the Excel file
     */
    @Transactional
    public void importEmployeeData(InputStream inputStream) {
        try {
            Map<Long, EmployeeWrapper> wrapperMap = parseExcel(inputStream);
            List<Employee> employees = wrapperMap.values().stream()
                    .map(w -> w.employee)
                    .toList();
            for (Employee emp : employees) {
                emp.setManager(null);
            }
            employeeRepository.saveAll(employees);
            employeeRepository.flush();
            for (EmployeeWrapper wrapper : wrapperMap.values()) {
                if (wrapper.managerId != null) {
                    EmployeeWrapper managerWrapper = wrapperMap.get(wrapper.managerId);
                    if (managerWrapper == null) {
                        throw new EmployeeNotFoundException("Manager with ID " + wrapper.managerId + " not found for employee " + wrapper.employee.getId());
                    }
                    wrapper.employee.setManager(managerWrapper.employee);
                }
            }
            employeeRepository.saveAll(employees);
        } catch (RuntimeException e) {
            throw new InvalidEmployeeDataException("Invalid employee data in Excel", e);
        }
    }

    static class EmployeeWrapper {
        Employee employee;
        Long managerId;

        EmployeeWrapper(Employee employee, Long managerId) {
            this.employee = employee;
            this.managerId = managerId;
        }
    }

    /**
     * Parses the Excel file and builds a map of EmployeeWrapper objects for further processing.
     * This method is developed to modularize Excel parsing and synthetic hierarchy generation.
     * @param inputStream InputStream of the Excel file
     * @return Map of employee ID to EmployeeWrapper
     */
    private Map<Long, EmployeeWrapper> parseExcel(InputStream inputStream) {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<Long, EmployeeWrapper> map = new HashMap<>();
            Random random = new Random();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Employee emp = new Employee();
                emp.setId((long) row.getCell(0).getNumericCellValue());
                emp.setName(row.getCell(1).getStringCellValue());
                emp.setCity(row.getCell(2).getStringCellValue());
                emp.setState(row.getCell(3).getStringCellValue());
                emp.setCategory(row.getCell(4).getStringCellValue());
                emp.setSalary(row.getCell(6).getNumericCellValue());
                Cell dojCell = row.getCell(7);
                if (DateUtil.isCellDateFormatted(dojCell)) {
                    emp.setDoj(dojCell.getLocalDateTimeCellValue().toLocalDate());
                }
                Long managerId = null;
                Cell managerCell = row.getCell(5);
                if (managerCell != null && managerCell.getCellType() == CellType.NUMERIC) {
                    managerId = (long) managerCell.getNumericCellValue();
                }
                map.put(emp.getId(), new EmployeeWrapper(emp, managerId));
            }

            // Step 2: Determine or create the Director
            Optional<EmployeeWrapper> existingDirector = map.values().stream()
                    .filter(w -> "Director".equalsIgnoreCase(w.employee.getCategory()))
                    .filter(w -> w.managerId == null || w.managerId == 0)
                    .findFirst();

            long syntheticStartId = 10000;
            Long directorId;

            if (existingDirector.isPresent()) {
                directorId = existingDirector.get().employee.getId();
            } else {
                directorId = syntheticStartId;
                Employee director = new Employee();
                director.setId(directorId);
                director.setName("Director" + directorId);
                director.setCity("HQ");
                director.setState("Leadership");
                director.setCategory("Director");
                director.setSalary(Math.round((80000 + random.nextDouble() * 40000) * 100.0) / 100.0);
                director.setDoj(LocalDate.now().minusYears(10));
                map.put(directorId, new EmployeeWrapper(director, null));
            }

            // Step 3: Create synthetic hierarchy
            int totalSynthetic = 50;
            int managerCount = Math.max(1, totalSynthetic / 4);
            int employeeCount = totalSynthetic - managerCount;
            int directToDirectorEmployeeCount = Math.max(1, employeeCount / 6);

            List<Long> syntheticManagerIds = new ArrayList<>();
            List<Long> employeesReportingToDirector = new ArrayList<>();

            // 3.1: Generate Managers under the Director
            for (int i = 1; i <= managerCount; i++) {
                long id = syntheticStartId + i;
                Employee mgr = new Employee();
                mgr.setId(id);
                mgr.setName("Manager" + id);
                mgr.setCity("City" + (i % 10));
                mgr.setState("State" + (i % 5));
                mgr.setCategory("manager");
                mgr.setSalary(Math.round((50000 + random.nextDouble() * 30000) * 100.0) / 100.0);
                mgr.setDoj(LocalDate.now().minusYears(2 + random.nextInt(4)));

                map.put(id, new EmployeeWrapper(mgr, directorId));
                syntheticManagerIds.add(id);
            }

            // 3.2: Generate Employees directly under Director (must not have reportees)
            for (int i = 0; i < directToDirectorEmployeeCount; i++) {
                long id = syntheticStartId + managerCount + i;
                Employee emp = new Employee();
                emp.setId(id);
                emp.setName("Emp" + id);
                emp.setCity("City" + random.nextInt(10));
                emp.setState("State" + random.nextInt(5));
                emp.setCategory("employee");
                emp.setSalary(Math.round((30000 + random.nextDouble() * 50000) * 100.0) / 100.0);
                emp.setDoj(LocalDate.now().minusDays(random.nextInt(365 * 5)));

                map.put(id, new EmployeeWrapper(emp, directorId));
                employeesReportingToDirector.add(id); // Track them to exclude later
            }

            // 3.3: Generate remaining Employees under Managers ONLY
            int remainingEmployees = employeeCount - directToDirectorEmployeeCount;

            // Exclude employees under Director from being chosen as managers
            List<Long> validManagers = syntheticManagerIds.stream().filter(
                    id -> !employeesReportingToDirector.contains(id)
            ).toList();

            for (int i = 0; i < remainingEmployees; i++) {
                long id = syntheticStartId + managerCount + directToDirectorEmployeeCount + i;
                Employee emp = new Employee();
                emp.setId(id);
                emp.setName("Emp" + id);
                emp.setCity("City" + random.nextInt(10));
                emp.setState("State" + random.nextInt(5));
                emp.setCategory("employee");
                emp.setSalary(Math.round((30000 + random.nextDouble() * 50000) * 100.0) / 100.0);
                emp.setDoj(LocalDate.now().minusDays(random.nextInt(365 * 5)));

                Long assignedManager = validManagers.get(random.nextInt(validManagers.size()));
                map.put(id, new EmployeeWrapper(emp, assignedManager));
            }

            return map;
        } catch (IOException e) {
            throw new FileProcessingException("Failed to parse Excel file", e);
        } catch (RuntimeException e) {
            throw new InvalidEmployeeDataException("Invalid data in Excel file", e);
        }
    }

    /**
     * Writes a list of employees to an Excel file and returns the file.
     * This method is developed to support exporting employee data for download or reporting.
     * @param employees List of employees to export
     * @return File containing the exported employee data
     */
    private File writeEmployeesToExcel(List<Employee> employees) {
        try {
            // Create a temporary file to avoid overwrite/corruption issues
            File outputFile = File.createTempFile("employee-export", ".xlsx");

            try (Workbook workbook = new XSSFWorkbook(); FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                Sheet sheet = workbook.createSheet("Employees");

                // Create header row
                String[] headers = {"ID", "Name", "City", "State", "Category", "Manager ID", "Salary", "DOJ"};
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }

                // Fill data rows
                int rowNum = 1;
                for (Employee emp : employees) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(emp.getId());
                    row.createCell(1).setCellValue(safe(emp.getName()));
                    row.createCell(2).setCellValue(safe(emp.getCity()));
                    row.createCell(3).setCellValue(safe(emp.getState()));
                    row.createCell(4).setCellValue(safe(emp.getCategory()));
                    row.createCell(5).setCellValue(emp.getManager() != null ? emp.getManager().getId() : 0);
                    row.createCell(6).setCellValue(emp.getSalary() != null ? emp.getSalary() : 0.0);
                    row.createCell(7).setCellValue(emp.getDoj() != null ? emp.getDoj().toString() : "");
                }

                // Autosize columns for better readability
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                // Write content to file
                workbook.write(fileOut);
            }
            return outputFile;
        } catch (IOException e) {
            throw new FileProcessingException("Failed to write employees to Excel file", e);
        }
    }

    // Utility method to safely extract strings
    private String safe(String value) {
        return value != null ? value : "";
    }

    /**
     * Returns a list of employees eligible for gratuity (more than 5 years of service).
     * This method is developed to support HR and payroll use cases for gratuity calculation.
     * @return List of EmployeeDTOs eligible for gratuity
     */
    public List<EmployeeDTO> getGratuityEligibleEmployees() {
        return getAllEmployeesFromCache().stream()
                .filter(emp -> emp.getDoj() != null &&
                        ChronoUnit.MONTHS.between(emp.getDoj(), LocalDate.now()) > 60)
                .map(emp -> new EmployeeDTO(
                        emp.getId(),
                        emp.getName(),
                        emp.getSalary(),
                        emp.getCategory(),
                        emp.getDoj(),
                        emp.getManager() != null ? emp.getManager().getId() : null
                ))
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of employees whose salary is higher than their manager's salary.
     * This method is developed to support analytics and reporting on salary structure.
     * @return List of EmployeeDTOs with higher salary than their manager
     */
    public List<EmployeeDTO> getEmployeesWithHigherSalaryThanManager() {
        return getAllEmployeesFromCache().stream()
                .filter(emp -> emp.getManager() != null && emp.getSalary() > emp.getManager().getSalary())
                .map(emp -> new EmployeeDTO(
                        emp.getId(),
                        emp.getName(),
                        emp.getSalary(),
                        emp.getCategory(),
                        emp.getDoj(),
                        emp.getManager().getId()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Returns the employee with the Nth highest salary.
     * This method is developed to support leaderboard, analytics, and compensation benchmarking.
     * @param n The rank (1-based) for the highest salary
     * @return EmployeeDTO with the Nth highest salary, or null if not found
     */
    public EmployeeDTO getNthHighestSalaryEmployee(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("Rank must be >= 1");
        }
        Employee emp = employeeRepository.findNthHighestSalary(n - 1); // Shifted for 0-based SQL
        if (emp == null) return null;

        return new EmployeeDTO(
                emp.getId(),
                emp.getName(),
                emp.getSalary(),
                emp.getCategory(),
                emp.getDoj(),
                emp.getManager() != null ? emp.getManager().getId() : null
        );
    }

    /**
     * Returns all employees from the cache for performance optimization.
     * This method is developed to reduce database load for frequently accessed employee lists.
     * @return List of all Employee entities
     */
    @Cacheable("allEmployees")
    public List<Employee> getAllEmployeesFromCache() {
        return employeeRepository.findAll();
    }

    /**
     * Returns a paginated list of employees as EmployeeDTOs.
     * This method is developed to support efficient pagination and sorting in UI/API.
     * @param pageable Pageable object containing page, size, and sort info
     * @return Page of EmployeeDTOs
     */
    @Cacheable(value = "pagedEmployees", key = "'page_'+#pageable.pageNumber + '_size_'+#pageable.pageSize + '_sort_'+#pageable.sort.toString()")
    public Page<EmployeeDTO> getAllEmployees(Pageable pageable) {
        return employeeRepository.findAll(pageable)
                .map(emp -> new EmployeeDTO(
                        emp.getId(),
                        emp.getName(),
                        emp.getSalary(),
                        emp.getCategory(),
                        emp.getDoj(),
                        emp.getManager() != null ? emp.getManager().getId() : null
                ));
    }

    /**
     * Generates and returns the employee hierarchy for a given manager as a JSON file.
     * This method is developed to support org chart visualization and reporting.
     * @param managerId The ID of the manager whose hierarchy is requested
     * @return File containing the hierarchy in JSON format
     */
    public File getEmployeeHierarchyByManager(Long managerId) {
        List<Employee> employees = getAllEmployeesFromCache();
        Map<Long, EmployeeNode> employeeMap = new HashMap<>();
        for (Employee emp : employees) {
            Long mgrId = emp.getManager() != null ? emp.getManager().getId() : null;
            employeeMap.put(emp.getId(), new EmployeeNode(emp.getId(), mgrId, emp.getName(), emp.getCategory()));
        }
        for (Employee emp : employees) {
            Long mgrId = emp.getManager() != null ? emp.getManager().getId() : null;
            if (mgrId != null) {
                employeeMap.get(mgrId).addReportee(employeeMap.get(emp.getId()));
            }
        }
        EmployeeNode root = employeeMap.get(managerId);
        if (root == null) {
            throw new EmployeeNotFoundException("Manager with ID " + managerId + " not found.");
        }
        try {
            File jsonFile = new File("employee_hierarchy_" + managerId + ".json");
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, root);
            return jsonFile;
        } catch (IOException e) {
            throw new FileProcessingException("Failed to write employee hierarchy JSON file", e);
        }
    }
}

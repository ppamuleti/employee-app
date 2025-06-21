# employee-app

## Strategies Used

This application was developed using the following strategies and best practices:

1. **Spring Boot, Microservice, JPA, Caffeine, H2**: Core backend built with Spring Boot, using JPA for ORM and Caffeine for high-performance caching. The architecture is modular and microservice-ready.
2. **Caching**: Frequently accessed data is cached using Caffeine to improve performance and reduce database load.
3. **JWT**: JSON Web Tokens are used for secure authentication and authorization.
4. **Swagger**: API documentation and testing are enabled via Swagger/OpenAPI.
5. **Unit and Integration Tests**: Comprehensive test coverage using JUnit, Mockito, and Spring Boot Test for both unit and integration scenarios.
6. **Dockerizing**: The app is containerized using Docker for easy deployment and environment consistency.
7. **GitHub Actions CI/CD**: Automated build, test, and Docker image creation pipeline using GitHub Actions.
8. **Applied DRY, KISS, YAGNI principles**: Codebase follows Don't Repeat Yourself, Keep It Simple Stupid, and You Aren't Gonna Need It principles for maintainability and clarity.

---

## Architecture Diagram

```
+-------------------+        +-------------------+        +-------------------+
|                   |        |                   |        |                   |
|   User/Client     +------->+  Spring Boot App  +------->+   Database (JPA)  |
| (Swagger, UI, API)|        | (REST Controller) |        |                   |
|                   |        |                   |        |                   |
+-------------------+        +-------------------+        +-------------------+
         |                           |   ^
         |                           |   |
         |         +-----------------+   |
         |         |                     |
         v         v                     |
+-----------------------+                |
|  JWT Authentication   |<---------------+
+-----------------------+
         |
         v
+-----------------------+
|      Caching (Caffeine)|
+-----------------------+
         |
         v
+-----------------------+
|   File Processing     |
| (Excel, JSON Export)  |
+-----------------------+
```

### Explanation: How a User Can Process the Employee File

1. **Upload**: The user uploads an Excel file containing employee data via the REST API (e.g., Swagger UI or a frontend client).
2. **Authentication**: The request is authenticated using JWT. Only authorized users can process files.
3. **Controller**: The REST controller receives the file and delegates processing to the service layer.
4. **Service Layer**:
    - Parses the Excel file, validates data, and persists employees using JPA.
    - Applies business logic (e.g., self-referencing manager relationships).
    - Uses Caffeine cache for frequently accessed employee data.
5. **Export/Download**: The processed data can be exported as Excel or JSON (e.g., for hierarchy visualization).
6. **API Documentation**: All endpoints and their usage are documented and testable via Swagger UI.
7. **CI/CD & Docker**: The app is built, tested, and containerized automatically via GitHub Actions, ensuring reliable deployments.

---

## How Initial Users Are Loaded and How to Add New Users

- **Initial User Loading:**
  - The application uses the `data.sql` file in `src/main/resources` to pre-load users into the database when the application starts.
  - For example, the following entry creates an admin user with the role `ROLE_ADMIN`:
    ```sql
    INSERT INTO users (id, username, password, role) VALUES
    (1, 'admin', '$2a$10$8oVQUIHbAXmKeS2452g/Z.kfuW3gyYqA/ODFiend4f8sqhe4avpBe', 'ROLE_ADMIN');
    ```
  - Passwords are stored in BCrypt-hashed format for security.

- **How to Create a New User:**
  1. Generate a BCrypt hash for the desired password (use an online tool or Java BCrypt encoder).
  2. Add a new line to `data.sql` with a unique id, username, hashed password, and role (e.g., `ROLE_USER` or `ROLE_ADMIN`).
  3. Example:
    ```sql
    INSERT INTO users (id, username, password, role) VALUES
    (3, 'newuser', '$2a$10$yourHashedPasswordHere', 'ROLE_USER');
    ```
  4. Restart the application to load the new user.

> **Note:** For production or persistent databases, use a registration endpoint or admin tool to add users instead of editing `data.sql` directly. The `data.sql` approach is best for development and testing.

---

## How to Use Key Features

### 1. Get Gratuity Eligible Employees
- **Endpoint:** `GET /employees/gratuity-eligible`
- **Description:** Returns a list of employees who have completed more than 5 years (60 months) of service and are eligible for gratuity.
- **Response Example:**
```json
[
  {
    "id": 1,
    "name": "Sneha",
    "salary": 70000.0,
    "category": "employee",
    "doj": "2019-01-01",
    "managerId": 10
  }
]
```

### 2. Get Employees With Higher Salary Than Their Manager
- **Endpoint:** `GET /employees/higher-than-manager`
- **Description:** Returns a list of employees whose salary is greater than their direct manager's salary.
- **Response Example:**
```json
[
  {
    "id": 3,
    "name": "Ravi",
    "salary": 80000.0,
    "category": "employee",
    "doj": "2023-06-04",
    "managerId": 2
  }
]
```

### 3. Get Nth Highest Salary Employee
- **Endpoint:** `GET /employees/nth-highest-salary/{n}`
- **Description:** Returns the employee with the Nth highest salary. For example, `n=1` returns the highest paid employee.
- **Response Example:**
```json
{
  "id": 2,
  "name": "Shivam",
  "salary": 120000.0,
  "category": "manager",
  "doj": "2022-07-05",
  "managerId": 1
}
```

---

## API Documentation

This project uses **Swagger/OpenAPI** for interactive API documentation.

- **Swagger UI:**
  - Visit: `http://localhost:8080/swagger-ui/index.html`
  - You can view, try out, and test all endpoints directly from the browser.
  - **Pagination:** The `/employees` API supports pagination using `page`, `size`, and `sortBy` query parameters for efficient data retrieval and navigation.

- **OpenAPI Spec:**
  - Visit: `http://localhost:8080/v3/api-docs`
  - Download the OpenAPI JSON for integration with other tools.

**Authentication:**
- Most endpoints require a valid JWT token. Use the `/auth/login` endpoint to obtain a token, then use the "Authorize" button in Swagger UI to authenticate your requests.

---

For more details, see the source code and Swagger documentation.

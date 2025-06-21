package com.pamu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Main entry point for the Employee Application.
 * This class is developed to bootstrap the Spring Boot application and enable caching for performance optimization.
 * The @EnableCaching annotation activates Spring's annotation-driven cache management capability.
 * The main method launches the application using Spring Boot's auto-configuration and component scanning.
 */
@SpringBootApplication
@EnableCaching
public class EmployeeAppApplication {

    /**
     * Starts the Employee Application.
     * This method is developed to launch the Spring Boot application context and initialize all beans and configurations.
     * @param args command-line arguments passed to the application
     */
	public static void main(String[] args) {
		SpringApplication.run(EmployeeAppApplication.class, args);
	}

}

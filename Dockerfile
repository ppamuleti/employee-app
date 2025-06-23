# Use an official OpenJDK 21 runtime as a parent image
FROM eclipse-temurin:21-jdk-alpine

# Set the working directory
WORKDIR /app

# Copy the built jar to the container
COPY target/*.jar app.jar

# Copy Maven wrapper and set permissions
COPY mvnw ./
RUN chmod +x mvnw

# Expose the default Spring Boot port
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Simple Dockerfile - JAR is already built by Jenkins and copied to VPS
FROM eclipse-temurin:21-jre-slim

WORKDIR /app

# Copy the pre-built JAR
COPY icsQuizUserService-0.1.jar app.jar

# Expose the port
EXPOSE 3090

# Run the Spring Boot application
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar", "--server.port=3090"]
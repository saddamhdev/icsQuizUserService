# ================================
# Stage 1 — Build JAR using Maven
# ================================
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ================================
# Stage 2 — Run JAR with Java 21
# ================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the exact JAR name
COPY --from=builder /app/target/icsQuizUserService-0.1.jar app.jar

# Expose port 8080 for WebFlux
EXPOSE 3090

# JVM performance tweaks (use G1GC)
ENTRYPOINT ["java", "-XX:+UseG1GC", "-jar", "/app/app.jar"]

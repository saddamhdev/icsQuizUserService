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

# Copy JAR
COPY --from=builder /app/target/*.jar app.jar

# You want container to run on 3090
EXPOSE 3090

# Spring Boot will now run on 3090 inside the container
ENTRYPOINT ["java", "-XX:+UseG1GC", "-jar", "/app/app.jar", "--server.port=3090"]

FROM eclipse-temurin:21-jre-slim

WORKDIR /app

COPY icsQuizUserService-0.1.jar app.jar

EXPOSE 3090

ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar", "--server.port=3090"]
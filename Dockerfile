FROM eclipse-temurin:21-jre
WORKDIR /app

# copy the JAR uploaded by Jenkins
COPY icsQuizUserService-0.1.jar app.jar

EXPOSE 3090

ENTRYPOINT ["java", "-XX:+UseG1GC", "-jar", "/app/app.jar", "--server.port=3090"]

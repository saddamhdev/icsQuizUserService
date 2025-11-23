FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy JAR uploaded by Jenkins
COPY icsQuizUserService-0.1.jar app.jar

# JVM Optimizations (BEST for Spring Boot in Kubernetes)
ENV JAVA_OPTS="\
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication \
    -Xms256m \
    -Xmx512m \
"

EXPOSE 3090

# Final entrypoint using JAVA_OPTS
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --server.port=3090"]

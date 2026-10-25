# Stage 1 - Build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Step 1 - Install root pom
COPY pom.xml .
RUN mvn install -N -DskipTests

# Step 2 - Build eod-reconciliation-job (does not use surveillance-events-lib)
COPY eod-reconciliation-job/pom.xml eod-reconciliation-job/
COPY eod-reconciliation-job/src eod-reconciliation-job/src
RUN mvn package -f eod-reconciliation-job/pom.xml -DskipTests

# Stage 2 - Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/eod-reconciliation-job/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
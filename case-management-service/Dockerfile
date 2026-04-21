# Stage 1 - Build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Step 1 - Install root pom
COPY pom.xml .
RUN mvn install -N -DskipTests

# Step 2 - Install surveillance-events-lib into local Maven repo
COPY surveillance-events-lib/pom.xml surveillance-events-lib/
COPY surveillance-events-lib/src surveillance-events-lib/src
RUN mvn install -f surveillance-events-lib/pom.xml -DskipTests

# Step 3 - Build case-management-service
COPY case-management-service/pom.xml case-management-service/
COPY case-management-service/src case-management-service/src
RUN mvn package -f case-management-service/pom.xml -DskipTests

# Stage 2 - Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/case-management-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
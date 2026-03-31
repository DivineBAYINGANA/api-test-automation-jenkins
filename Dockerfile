# ============================================================
# API Test Automation - REST Assured Test Suite
# Dockerfile for containerized test execution
# ============================================================

# Stage 1: Build and cache Maven dependencies
FROM maven:3.9.6-eclipse-temurin-21 AS builder

LABEL maintainer="API Testing Team"
LABEL description="Containerized API test suite using REST Assured + JUnit 5 + Allure"

# Set working directory
WORKDIR /app

# Copy dependency manifest first
COPY pom.xml .

# Download dependencies (cache hit enabled)
RUN mvn dependency:go-offline -B

# Copy source and test code
COPY src ./src

# Default test run (headless / CLI)
RUN mvn test -B --no-transfer-progress || true

# ============================================================
# Stage 2: Generate Allure Report
# ============================================================
FROM builder AS reporter

# Generate Allure HTML report from results
RUN mvn allure:report -B --no-transfer-progress || echo "Report generation finished"

# ============================================================
# Stage 3: Serve Report via Nginx
# ============================================================
FROM nginx:alpine AS report-site

# Copy the generated report to Nginx html directory
COPY --from=reporter /app/target/site/allure-maven-plugin /usr/share/nginx/html

# Expose Nginx port
EXPOSE 80

# Default command: keep nginx running
CMD ["nginx", "-g", "daemon off;"]

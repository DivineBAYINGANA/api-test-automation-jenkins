# API Test Automation with Jenkins CI/CD

This project demonstrates how to automate REST Assured API tests using Jenkins CI/CD. It includes a Jenkins pipeline, Dockerfile, and sample test suite.

## Features
- REST Assured API tests (JUnit 5)
- Jenkins pipeline for CI/CD
- Dockerfile for containerized test execution
- Allure and JUnit test reports
- Email/Slack notifications on build status

## Jenkins Setup
1. **Install Jenkins** (locally or via Docker):
   - Docker: `docker run -p 8080:8080 -p 50000:50000 jenkins/jenkins:lts`
2. **Install Plugins:**
   - Git, Pipeline, HTML Publisher, JUnit, Email Extension, Slack (optional)
3. **Configure Jenkins Pipeline Job:**
   - Use the provided `Jenkinsfile` (Declarative Pipeline)
   - Set up credentials and tools (Maven, JDK)
   - Configure email/Slack notifications
4. **Webhook:**
   - Add a webhook in your GitHub repo to trigger Jenkins on push

## Running Locally with Docker
```
docker build -t api-test-suite .
docker run --rm api-test-suite
```

## Project Structure
- `src/` - Test source code
- `Dockerfile` - Container build
- `Jenkinsfile` - Jenkins pipeline
- `pom.xml` - Maven dependencies

## Reports
- JUnit XML: `target/surefire-reports/`
- Allure HTML: `target/allure-results/`

## Notifications
- Configure email/Slack in Jenkinsfile as needed

---
Replace placeholders in Jenkinsfile with your repo and notification details.

# API Test Automation with Jenkins CI/CD

This project demonstrates how to automate REST Assured API tests using Jenkins CI/CD with comprehensive reporting and notifications.

## Features
✅ REST Assured API tests (JUnit 5)  
✅ Jenkins declarative pipeline   
✅ Allure and JUnit test reports  
✅ Email and Slack notifications  
✅ Docker containerized execution  
✅ GitHub webhook triggers  
✅ Build artifacts archiving  

## Quick Start

### Prerequisites
- Jenkins installed and running
- Maven 3.8.1+
- JDK 21+
- Git
- (Optional) Docker

### 1. Jenkins Installation

**Option A: Local Jenkins**
```bash
java -jar jenkins.war
```
Access at `http://localhost:8080`

**Option B: Docker Jenkins**
```bash
docker run -d -p 8080:8080 -p 50000:50000 --name jenkins jenkins/jenkins:lts
```

### 2. Required Jenkins Plugins

Install via **Manage Jenkins → Manage Plugins:**

- Pipeline (workflow-aggregator)
- Git
- HTML Publisher
- JUnit
- Email Extension
- Slack Notification
- Log Parser (optional)

### 3. Configure Jenkins Tools

Go to **Manage Jenkins → Tools Configuration:**

**Maven:**
- Name: `Maven 3.8.1`
- MAVEN_HOME: (auto-install or set path)

**JDK:**
- Name: `JDK21`
- JAVA_HOME: Set to your JDK 21 installation

### 4. Create a Pipeline Job

1. **New Item** → Enter job name → Select **Pipeline**
2. Under **Pipeline**, select **Pipeline script from SCM**
3. **SCM:** Git
   - **Repository URL:** Your GitHub repo URL
   - **Branch:** `*/main` or `*/master`
4. **Script Path:** `Jenkinsfile` (default)
5. Save

### 5. Configure Notifications

See the setup guides:
- [Email Setup](JENKINS_EMAIL_SETUP.md)
- [Slack Setup](JENKINS_SLACK_SETUP.md)

### 6. GitHub Webhook (Optional but Recommended)

1. Go to your GitHub repo → **Settings → Webhooks → Add webhook**
2. **Payload URL:** `http://your-jenkins-url/github-webhook/`
3. **Content type:** `application/json`
4. **Trigger:** Push events
5. **Active:** ✓
6. Click **Add webhook**

## Running Tests Locally

### Maven Command
```bash
mvn clean test
```

### Docker Container
```bash
docker build -t api-test-suite .
docker run --rm api-test-suite
```

## Test Reports

### JUnit Reports
- Location: `target/surefire-reports/`
- Published in Jenkins: **JUnit** section

### Allure Reports
- Location: `target/allure-results/` (raw data)
- Generated HTML: `target/allure-report/index.html`
- Published in Jenkins: **Allure Report** tab

## Project Structure
```
.
├── Jenkinsfile                           # Jenkins pipeline definition
├── Dockerfile                            # Containerized test execution
├── pom.xml                               # Maven dependencies & plugins
├── README.md                             # This file
├── JENKINS_EMAIL_SETUP.md                # Email configuration guide
├── JENKINS_SLACK_SETUP.md                # Slack configuration guide
└── src/
    ├── main/java/                        # (if applicable)
    └── test/java/                        # API test classes
        └── resources/                    # Test data, configs
```

## Pipeline Stages

1. **Checkout** - Pulls code from Git
2. **Build** - `mvn clean install -DskipTests`
3. **Test** - `mvn test` - Runs all tests
4. **Report** - Generates and publishes Allure reports
5. **Notify** - Sends email and Slack notifications
6. **Archive** - Saves artifacts

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Pipeline not triggering | Check webhook in GitHub, verify URL is correct |
| "Couldn't find any executable" | Ensure Maven/JDK configured in Tools |
| No test results | Check `target/surefire-reports/*.xml` exists |
| Missing Allure report | Allure plugin configured in pom.xml, see `mvn allure:report` |
| Slack notifications not sending | Check plugin installed, credential configured in Jenkins |
| Email not received | Check SMTP config, look in Jenkins logs |

## Configuration Files

- **Jenkinsfile** - Declarative pipeline with stages and notifications
- **pom.xml** - Maven build, REST Assured, JUnit 5, Allure dependencies
- **Dockerfile** - Multi-stage build with test execution and report generation

## Logging & Debugging

- **Jenkins Build Log:** View real-time output in Jenkins UI
- **Maven Output:** See `mvn test` and `mvn allure:report` output
- **Slack Messages:** Check Jenkins logs if notifications fail
- **Email Logs:** **Manage Jenkins → System Log** for email issues

---

**For detailed setup instructions, see:**
- [Email Setup Guide](JENKINS_EMAIL_SETUP.md)
- [Slack Setup Guide](JENKINS_SLACK_SETUP.md)

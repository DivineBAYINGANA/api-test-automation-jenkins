#!/usr/bin/env groovy

pipeline {
    agent any

    triggers {
        githubPush()
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                bat 'mvn clean install -DskipTests -B'
            }
        }

        stage('Test') {
            steps {
                bat 'mvn test -B'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Report') {
            steps {
                script {
                    bat 'mvn allure:report -B'

                    if (fileExists('target/site/allure-maven-plugin/index.html')) {
                        echo "Allure report found and publishing..."
                        publishHTML([
                            allowMissing: false,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'target/site/allure-maven-plugin',
                            reportFiles: 'index.html',
                            reportName: 'Allure Report'
                        ])
                    } else {
                        echo "Warning: Allure report not found at expected location."
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                echo "========== SENDING SUCCESS NOTIFICATIONS =========="

                echo "[1/2] Attempting to send Slack notification..."
                try {
                    withCredentials([string(credentialsId: 'incoming-webhook', variable: 'SLACK_WEBHOOK')]) {
                        def message = ":white_check_mark: *SUCCESS*: Job '${env.JOB_NAME}' [#${env.BUILD_NUMBER}]\nStatus: PASSED | Duration: ${currentBuild.durationString}\nBuild: ${env.BUILD_URL}\nReport: ${env.BUILD_URL}allure/"
                        writeFile file: 'slack-payload.json', text: groovy.json.JsonOutput.toJson([text: message])
                        bat """powershell -Command "Invoke-RestMethod -Uri \$env:SLACK_WEBHOOK -Method Post -ContentType 'application/json' -Body (Get-Content slack-payload.json -Raw)" """
                    }
                    echo "✅ Slack notification sent successfully"
                } catch (Exception e) {
                    echo "❌ Slack notification failed: ${e.message}"
                }

                echo "[2/2] Attempting to send email notification..."
                try {
                    mail(
                        to: 'divinegihozo@gmail.com',
                        subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: """Build PASSED

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Duration: ${currentBuild.durationString}

View Build: ${env.BUILD_URL}
View Report: ${env.BUILD_URL}allure/
"""
                    )
                    echo "✅ Email notification sent successfully"
                } catch (Exception e) {
                    echo "❌ Email notification failed: ${e.message}"
                }

                echo "========== SUCCESS NOTIFICATIONS COMPLETE =========="
            }
        }
        failure {
            script {
                echo "========== SENDING FAILURE NOTIFICATIONS =========="

                echo "[1/2] Attempting to send Slack notification..."
                try {
                    withCredentials([string(credentialsId: 'incoming-webhook', variable: 'SLACK_WEBHOOK')]) {
                        def message = ":x: *FAILED*: Job '${env.JOB_NAME}' [#${env.BUILD_NUMBER}]\nStatus: FAILED | Duration: ${currentBuild.durationString}\nBuild: ${env.BUILD_URL}\nReport: ${env.BUILD_URL}allure/"
                        writeFile file: 'slack-payload.json', text: groovy.json.JsonOutput.toJson([text: message])
                        bat """powershell -Command "Invoke-RestMethod -Uri \$env:SLACK_WEBHOOK -Method Post -ContentType 'application/json' -Body (Get-Content slack-payload.json -Raw)" """
                    }
                    echo "✅ Slack notification sent successfully"
                } catch (Exception e) {
                    echo "❌ Slack notification failed: ${e.message}"
                }

                echo "[2/2] Attempting to send email notification..."
                try {
                    mail(
                        to: 'divinegihozo@gmail.com',
                        subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: """Build FAILED

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Duration: ${currentBuild.durationString}

View Build: ${env.BUILD_URL}
View Report: ${env.BUILD_URL}allure/

Please check the console output for error details.
"""
                    )
                    echo "✅ Email notification sent successfully"
                } catch (Exception e) {
                    echo "❌ Email notification failed: ${e.message}"
                }

                echo "========== FAILURE NOTIFICATIONS COMPLETE =========="
            }
        }
        unstable {
            script {
                echo "========== SENDING UNSTABLE NOTIFICATIONS =========="

                echo "[1/2] Attempting to send Slack notification..."
                try {
                    withCredentials([string(credentialsId: 'incoming-webhook', variable: 'SLACK_WEBHOOK')]) {
                        def message = ":warning: *UNSTABLE*: Job '${env.JOB_NAME}' [#${env.BUILD_NUMBER}]\nStatus: UNSTABLE (test failures) | Duration: ${currentBuild.durationString}\nBuild: ${env.BUILD_URL}\nReport: ${env.BUILD_URL}allure/"
                        writeFile file: 'slack-payload.json', text: groovy.json.JsonOutput.toJson([text: message])
                        bat """powershell -Command "Invoke-RestMethod -Uri \$env:SLACK_WEBHOOK -Method Post -ContentType 'application/json' -Body (Get-Content slack-payload.json -Raw)" """
                    }
                    echo "✅ Slack notification sent successfully"
                } catch (Exception e) {
                    echo "❌ Slack notification failed: ${e.message}"
                }

                echo "[2/2] Attempting to send email notification..."
                try {
                    mail(
                        to: 'divinegihozo@gmail.com',
                        subject: "UNSTABLE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: """Build UNSTABLE - Test Failures Detected

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Duration: ${currentBuild.durationString}

View Build: ${env.BUILD_URL}
View Report: ${env.BUILD_URL}allure/

Please check the Allure report for failing tests.
"""
                    )
                    echo "✅ Email notification sent successfully"
                } catch (Exception e) {
                    echo "❌ Email notification failed: ${e.message}"
                }

                echo "========== UNSTABLE NOTIFICATIONS COMPLETE =========="
            }
        }
        always {
            archiveArtifacts artifacts: 'target/site/allure-maven-plugin/**,target/surefire-reports/**', allowEmptyArchive: true
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
        }
    }
}

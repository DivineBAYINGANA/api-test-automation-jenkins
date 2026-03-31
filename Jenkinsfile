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
                echo "[1/3] Sending Slack notification..."
                try {
                    slackSend(
                        color: 'good',
                        message: """✅ SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' passed
*Build Details:*
• Status: PASSED
• Duration: ${currentBuild.durationString}
• <${env.BUILD_URL}|View Full Build>
• <${env.BUILD_URL}allure|View Allure Report>"""
                    )
                    echo "✅ Slack notification sent"
                } catch (Exception e) {
                    echo "⚠️  Slack error: ${e.message}"
                }
                
                echo "[2/3] Sending Email notification..."
                try {
                    mail(
                        subject: "✅ SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: """Build PASSED

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Duration: ${currentBuild.durationString}

View Build: ${env.BUILD_URL}
View Report: ${env.BUILD_URL}allure/
"""
                    )
                    echo "✅ Email notification sent"
                } catch (Exception e) {
                    echo "⚠️  Email error: ${e.message}"
                }
                
                echo "[3/3] Archiving artifacts..."
                archiveArtifacts artifacts: 'target/site/allure-maven-plugin/**,target/surefire-reports/**', allowEmptyArchive: true
                junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                echo "✅ Artifacts archived"
                echo "========== SUCCESS NOTIFICATIONS COMPLETE =========="
            }
        }
        failure {
            script {
                echo "========== SENDING FAILURE NOTIFICATIONS =========="
                echo "[1/3] Sending Slack notification..."
                try {
                    slackSend(
                        color: 'danger',
                        message: """❌ FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' failed
*Build Details:*
• Status: FAILED
• Duration: ${currentBuild.durationString}
• <${env.BUILD_URL}|View Full Build>
• <${env.BUILD_URL}allure|View Allure Report>"""
                    )
                    echo "✅ Slack notification sent"
                } catch (Exception e) {
                    echo "⚠️  Slack error: ${e.message}"
                }
                
                echo "[2/3] Sending Email notification..."
                try {
                    mail(
                        subject: "❌ FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: """Build FAILED

Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Duration: ${currentBuild.durationString}

View Build: ${env.BUILD_URL}
View Report: ${env.BUILD_URL}allure/

Please check the console output for error details.
"""
                    )
                    echo "✅ Email notification sent"
                } catch (Exception e) {
                    echo "⚠️  Email error: ${e.message}"
                }
                
                echo "[3/3] Archiving artifacts..."
                archiveArtifacts artifacts: 'target/site/allure-maven-plugin/**,target/surefire-reports/**', allowEmptyArchive: true
                junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                echo "✅ Artifacts archived"
                echo "========== FAILURE NOTIFICATIONS COMPLETE =========="
            }
        }
        unstable {
            script {
                echo "Build is unstable - sending notifications..."
                try {
                    slackSend(
                        color: 'warning',
                        message: """⚠️  UNSTABLE: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' is unstable
<${env.BUILD_URL}|View Build>"""
                    )
                } catch (Exception e) {
                    echo "Slack error: ${e.message}"
                }
            }
        }
        always {
            echo "========== PIPELINE CLEANUP =========="
            cleanWs(deleteDirs: true, patterns: [[pattern: 'parse-results.ps1, slack-payload.json', type: 'INCLUDE']])
            echo "========== PIPELINE COMPLETE =========="
        }
    }
}

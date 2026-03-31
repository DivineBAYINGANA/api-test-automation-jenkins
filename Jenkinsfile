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
                    // Generate Allure report
                    bat 'mvn allure:report -B'
                    
                    // Publish Allure report (generated in target/site/allure-maven-plugin)
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
                
                // Send Slack notification
                echo "[1/3] Attempting to send Slack notification..."
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
                    echo "✅ Slack notification sent successfully"
                } catch (Exception e) {
                    echo "❌ Slack notification failed: ${e.message}"
                }
                
                // Upload report to Slack
                echo "[2/3] Checking for Allure report file..."
                if (fileExists('target/site/allure-maven-plugin/index.html')) {
                    echo "✅ Report file found, uploading to Slack..."
                    try {
                        slackUploadFile(filePath: 'target/site/allure-maven-plugin/index.html', initialComment: 'Allure Test Report')
                        echo "✅ Report uploaded to Slack successfully"
                    } catch (Exception e) {
                        echo "⚠️ Report upload failed: ${e.message}"
                    }
                } else {
                    echo "⚠️ Report file not found at target/site/allure-maven-plugin/index.html"
                }
                
                // Send email notification
                echo "[3/3] Attempting to send email notification..."
                try {
                    mail(
                        subject: "✅ SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: """Build PASSED
                
Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Duration: ${currentBuild.durationString}

View Build: ${env.BUILD_URL}
View Report: ${env.BUILD_URL}allure/
""",
                        recipientProviders: [developers(), requestor()]
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
                
                // Send Slack notification
                echo "[1/3] Attempting to send Slack notification..."
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
                    echo "✅ Slack notification sent successfully"
                } catch (Exception e) {
                    echo "❌ Slack notification failed: ${e.message}"
                }
                
                // Upload report to Slack
                echo "[2/3] Checking for Allure report file..."
                if (fileExists('target/site/allure-maven-plugin/index.html')) {
                    echo "✅ Report file found, uploading to Slack..."
                    try {
                        slackUploadFile(filePath: 'target/site/allure-maven-plugin/index.html', initialComment: 'Allure Test Report - Review for failures')
                        echo "✅ Report uploaded to Slack successfully"
                    } catch (Exception e) {
                        echo "⚠️ Report upload failed: ${e.message}"
                    }
                } else {
                    echo "⚠️ Report file not found at target/site/allure-maven-plugin/index.html"
                }
                
                // Send email notification
                echo "[3/3] Attempting to send email notification..."
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
""",
                        recipientProviders: [developers(), requestor()]
                    )
                    echo "✅ Email notification sent successfully"
                } catch (Exception e) {
                    echo "❌ Email notification failed: ${e.message}"
                }
                
                echo "========== FAILURE NOTIFICATIONS COMPLETE =========="
            }
        }
        always {
            archiveArtifacts artifacts: 'target/site/allure-maven-plugin/**,target/surefire-reports/**', allowEmptyArchive: true
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
        }
    }
}

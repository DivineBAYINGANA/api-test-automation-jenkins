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
            slackSend(
                color: 'good',
                message: """✅ SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' passed
*Build Details:*
• Status: PASSED
• Duration: ${currentBuild.durationString}
• <${env.BUILD_URL}|View Full Build>
• <${env.BUILD_URL}allure|View Allure Report>"""
            )
            script {
                if (fileExists('target/site/allure-maven-plugin/index.html')) {
                    slackUploadFile(filePath: 'target/site/allure-maven-plugin/index.html', initialComment: 'Allure Test Report')
                }
            }
        }
        failure {
            slackSend(
                color: 'danger',
                message: """❌ FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' failed
*Build Details:*
• Status: FAILED
• Duration: ${currentBuild.durationString}
• <${env.BUILD_URL}|View Full Build>
• <${env.BUILD_URL}allure|View Allure Report>"""
            )
            script {
                if (fileExists('target/site/allure-maven-plugin/index.html')) {
                    slackUploadFile(filePath: 'target/site/allure-maven-plugin/index.html', initialComment: 'Allure Test Report - Review for failures')
                }
            }
        }
        always {
            archiveArtifacts artifacts: 'target/site/allure-maven-plugin/**,target/surefire-reports/**', allowEmptyArchive: true
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
        }
    }
}

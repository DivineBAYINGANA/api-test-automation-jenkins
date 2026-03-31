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
                    // Try to generate Allure report if plugin is configured
                    bat '''
                        mvn allure:report -B 2>nul || echo "Allure report generation skipped"
                    '''
                    
                    // Check if allure-report exists, if not check for allure-results
                    if (fileExists('target/allure-report/index.html')) {
                        echo "Allure report found, publishing..."
                        publishHTML([
                            allowMissing: false,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'target/allure-report',
                            reportFiles: 'index.html',
                            reportName: 'Allure Report'
                        ])
                    } else if (fileExists('target/allure-results')) {
                        echo "Allure results found but HTML report not generated. Publishing results..."
                        publishHTML([
                            allowMissing: true,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'target/allure-results',
                            reportFiles: 'index.html',
                            reportName: 'Allure Results'
                        ])
                    } else {
                        echo "No Allure reports found. JUnit reports will be available."
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
                if (fileExists('target/allure-report/index.html')) {
                    slackUploadFile(filePath: 'target/allure-report/index.html', initialComment: 'Allure Test Report')
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
                if (fileExists('target/allure-report/index.html')) {
                    slackUploadFile(filePath: 'target/allure-report/index.html', initialComment: 'Allure Test Report - Review for failures')
                }
            }
        }
        always {
            archiveArtifacts artifacts: 'target/allure-report/**,target/surefire-reports/**', allowEmptyArchive: true
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
        }
    }
}

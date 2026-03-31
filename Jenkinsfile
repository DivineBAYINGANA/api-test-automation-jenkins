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
                bat 'mvn allure:report -B || exit /b 0'
                publishHTML([reportDir: 'target/allure-report', reportFiles: 'index.html', reportName: 'Allure Report'])
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'target/allure-report/**,target/surefire-reports/**', allowEmptyArchive: true
        }
    }
}

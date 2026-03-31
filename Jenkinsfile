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
                catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                    bat 'mvn test -B'
                }
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml',
                          allowEmptyResults: true
                }
            }
        }

        stage('Report') {
            steps {
                script {
                    catchError(buildResult: currentBuild.result ?: 'SUCCESS', stageResult: 'UNSTABLE') {
                        bat 'mvn allure:report -B'
                    }

                    if (fileExists('target/site/allure-maven-plugin/index.html')) {
                        echo "Allure report found and publishing..."
                        publishHTML([
                            allowMissing: true,
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
                        color: '#36a64f',
                        channel: '#jenkins-notifications',
                        botUser: false,
                        baseUrl: 'https://hooks.slack.com/services/',
                        message: """✅ *BUILD PASSED*
*Job:* ${env.JOB_NAME}
*Build #:* ${env.BUILD_NUMBER}
*Status:* SUCCESS
*Duration:* ${currentBuild.durationString}
*Branch:* ${env.GIT_BRANCH ?: 'N/A'}
*Commit:* ${env.GIT_COMMIT?.take(7) ?: 'N/A'}

*Reports & Details:*
• <${env.BUILD_URL}|View Full Build>
• <${env.BUILD_URL}testReport|View Test Results>
• <${env.BUILD_URL}Allure_20Report|View Allure Report>
• <${env.BUILD_URL}execution/node/2/ws|View Workspace>

Build completed successfully with all tests passing! 🎉"""
                    )
                    echo "✅ Slack notification sent successfully"
                } catch (Exception e) {
                    echo "⚠️  Slack error: ${e.message} - Check webhook configuration"
                }
                
                echo "[2/3] Sending Email notification..."
                try {
                    def testSummary = currentBuild.result == 'SUCCESS' ? 'All tests passed ✅' : 'Tests executed'
                    withCredentials([string(credentialsId: 'recipient-email', variable: 'RECIPIENT')]) {
                    mail(
                        to: RECIPIENT,
                        subject: "✅ BUILD PASSED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        mimeType: 'text/html',
                        body: """
<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; color: #333; }
        .header { background-color: #28a745; color: white; padding: 20px; border-radius: 5px; margin-bottom: 20px; }
        .section { margin: 20px 0; padding: 15px; background-color: #f5f5f5; border-left: 4px solid #28a745; }
        .section h3 { margin-top: 0; color: #28a745; }
        .detail { margin: 8px 0; }
        .label { font-weight: bold; color: #333; }
        .value { color: #666; }
        .footer { margin-top: 30px; font-size: 12px; color: #999; border-top: 1px solid #ddd; padding-top: 10px; }
        a { color: #0066cc; text-decoration: none; }
        a:hover { text-decoration: underline; }
        .status-success { color: #28a745; font-weight: bold; }
    </style>
</head>
<body>
    <div class="header">
        <h2>✅ Build Passed Successfully</h2>
    </div>
    
    <div class="section">
        <h3>Build Information</h3>
        <div class="detail"><span class="label">Job Name:</span> <span class="value">${env.JOB_NAME}</span></div>
        <div class="detail"><span class="label">Build Number:</span> <span class="value">#${env.BUILD_NUMBER}</span></div>
        <div class="detail"><span class="label">Status:</span> <span class="value status-success">✅ SUCCESS</span></div>
        <div class="detail"><span class="label">Duration:</span> <span class="value">${currentBuild.durationString}</span></div>
        <div class="detail"><span class="label">Timestamp:</span> <span class="value">${new Date().format('yyyy-MM-dd HH:mm:ss')}</span></div>
    </div>
    
    <div class="section">
        <h3>Test Summary</h3>
        <div class="detail"><span class="label">Test Results:</span> <span class="value">${testSummary}</span></div>
        <div class="detail"><span class="label">Report Generated:</span> <span class="value">✅ Yes</span></div>
        <div class="detail"><span class="label">Artifacts Archived:</span> <span class="value">✅ Yes</span></div>
    </div>
    
    <div class="section">
        <h3>Quick Links</h3>
        <ul>
            <li><a href="${env.BUILD_URL}">Full Build Log</a></li>
            <li><a href="${env.BUILD_URL}testReport">Test Results Report</a></li>
            <li><a href="${env.BUILD_URL}Allure_20Report">Allure Test Report</a></li>
            <li><a href="${env.BUILD_URL}execution/node/2/ws">Build Workspace</a></li>
        </ul>
    </div>
    
    <div class="section">
        <h3>Next Steps</h3>
        <ul>
            <li>Review the Allure Report for detailed test metrics</li>
            <li>Check test results for any potential improvements</li>
            <li>Deploy to next stage if applicable</li>
        </ul>
    </div>
    
    <div class="footer">
        <p>This automated notification was generated by Jenkins Pipeline for <strong>${env.JOB_NAME}</strong></p>
        <p>Generated at: ${new Date().format('yyyy-MM-dd HH:mm:ss')} | Build Server: ${env.JENKINS_URL}</p>
    </div>
</body>
</html>
"""
                    )
                    } // end withCredentials
                    echo "✅ Email notification sent successfully"
                } catch (Exception e) {
                    echo "⚠️  Email error: ${e.message} - Check SMTP configuration"
                }
                
                echo "[3/3] Archiving artifacts..."
                archiveArtifacts artifacts: 'target/site/allure-maven-plugin/**,target/surefire-reports/**', allowEmptyArchive: true
                junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                echo "✅ Artifacts archived successfully"
                echo "========== SUCCESS NOTIFICATIONS COMPLETE =========="
            }
        }
        failure {
            script {
                echo "========== SENDING FAILURE NOTIFICATIONS =========="
                echo "[1/3] Sending Slack notification..."
                try {
                    slackSend(
                        color: '#d32f2f',
                        channel: '#jenkins-notifications',
                        botUser: false,
                        baseUrl: 'https://hooks.slack.com/services/',
                        message: """❌ *BUILD FAILED*
*Job:* ${env.JOB_NAME}
*Build #:* ${env.BUILD_NUMBER}
*Status:* FAILED
*Duration:* ${currentBuild.durationString}
*Branch:* ${env.GIT_BRANCH ?: 'N/A'}
*Commit:* ${env.GIT_COMMIT?.take(7) ?: 'N/A'}

*⚠️ Action Required:*
• <${env.BUILD_URL}|View Full Build Log>
• <${env.BUILD_URL}testReport|View Test Failures>
• <${env.BUILD_URL}Allure_20Report|View Allure Report>
• <${env.BUILD_URL}console|View Console Output>

Please investigate and fix the build issues. Check the logs for more details. ⛔"""
                    )
                    echo "✅ Slack notification sent successfully"
                } catch (Exception e) {
                    echo "⚠️  Slack error: ${e.message} - Check webhook configuration"
                }
                
                echo "[2/3] Sending Email notification..."
                try {
                    withCredentials([string(credentialsId: 'recipient-email', variable: 'RECIPIENT')]) {
                    mail(
                        to: RECIPIENT,
                        subject: "❌ BUILD FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        mimeType: 'text/html',
                        body: """
<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; color: #333; }
        .header { background-color: #d32f2f; color: white; padding: 20px; border-radius: 5px; margin-bottom: 20px; }
        .section { margin: 20px 0; padding: 15px; background-color: #f5f5f5; border-left: 4px solid #d32f2f; }
        .section h3 { margin-top: 0; color: #d32f2f; }
        .detail { margin: 8px 0; }
        .label { font-weight: bold; color: #333; }
        .value { color: #666; }
        .footer { margin-top: 30px; font-size: 12px; color: #999; border-top: 1px solid #ddd; padding-top: 10px; }
        a { color: #0066cc; text-decoration: none; }
        a:hover { text-decoration: underline; }
        .status-failure { color: #d32f2f; font-weight: bold; }
        .warning-box { background-color: #fff3cd; border: 1px solid #ffc107; padding: 15px; border-radius: 4px; margin: 15px 0; }
    </style>
</head>
<body>
    <div class="header">
        <h2>❌ Build Failed - Action Required</h2>
    </div>
    
    <div class="warning-box">
        <strong>⚠️ ALERT:</strong> Your build has failed. Please review the errors below and take appropriate action.
    </div>
    
    <div class="section">
        <h3>Build Information</h3>
        <div class="detail"><span class="label">Job Name:</span> <span class="value">${env.JOB_NAME}</span></div>
        <div class="detail"><span class="label">Build Number:</span> <span class="value">#${env.BUILD_NUMBER}</span></div>
        <div class="detail"><span class="label">Status:</span> <span class="value status-failure">❌ FAILED</span></div>
        <div class="detail"><span class="label">Duration:</span> <span class="value">${currentBuild.durationString}</span></div>
        <div class="detail"><span class="label">Timestamp:</span> <span class="value">${new Date().format('yyyy-MM-dd HH:mm:ss')}</span></div>
        <div class="detail"><span class="label">Branch:</span> <span class="value">${env.GIT_BRANCH ?: 'N/A'}</span></div>
    </div>
    
    <div class="section">
        <h3>Failure Details</h3>
        <div class="detail"><span class="label">Failure Type:</span> <span class="value">Check logs for details</span></div>
        <div class="detail"><span class="label">Stage Failed:</span> <span class="value">See console output</span></div>
    </div>
    
    <div class="section">
        <h3>🔗 Documentation & Resources</h3>
        <ul>
            <li><a href="${env.BUILD_URL}console">Full Console Output</a></li>
            <li><a href="${env.BUILD_URL}testReport">Test Failure Report</a></li>
            <li><a href="${env.BUILD_URL}Allure_20Report">Allure Test Report</a></li>
            <li><a href="${env.BUILD_URL}">Full Build Details</a></li>
        </ul>
    </div>
    
    <div class="section">
        <h3>Recommended Actions</h3>
        <ol>
            <li>Review the <strong>Console Output</strong> to identify the root cause</li>
            <li>Check the <strong>Test Report</strong> for failing test details</li>
            <li>View the <strong>Full Build Log</strong> for complete error messages</li>
            <li>Fix the issues in your code and commit</li>
            <li>Push changes to trigger a new build</li>
        </ol>
    </div>
    
    <div class="section">
        <h3>Need Help?</h3>
        <p>If you need assistance debugging this build failure:</p>
        <ul>
            <li>Check recent changes in Git history</li>
            <li>Review the Allure Report for visual test analytics</li>
            <li>Contact the QA team or DevOps team for support</li>
        </ul>
    </div>
    
    <div class="footer">
        <p>This automated notification was generated by Jenkins Pipeline for <strong>${env.JOB_NAME}</strong></p>
        <p>Generated at: ${new Date().format('yyyy-MM-dd HH:mm:ss')} | Build Server: ${env.JENKINS_URL}</p>
    </div>
</body>
</html>
"""
                    )
                    } // end withCredentials
                    echo "✅ Email notification sent successfully"
                } catch (Exception e) {
                    echo "⚠️  Email error: ${e.message} - Check SMTP configuration"
                }
                
                echo "[3/3] Archiving artifacts..."
                archiveArtifacts artifacts: 'target/site/allure-maven-plugin/**,target/surefire-reports/**', allowEmptyArchive: true
                junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                echo "✅ Artifacts archived for failure analysis"
                echo "========== FAILURE NOTIFICATIONS COMPLETE =========="
            }
        }
        unstable {
            script {
                echo "========== SENDING UNSTABLE BUILD NOTIFICATIONS =========="
                echo "[1/2] Sending Slack notification..."
                try {
                    slackSend(
                        color: '#ff9800',
                        channel: '#jenkins-notifications',
                        botUser: false,
                        baseUrl: 'https://hooks.slack.com/services/',
                        message: """⚠️  *BUILD UNSTABLE*
*Job:* ${env.JOB_NAME}
*Build #:* ${env.BUILD_NUMBER}
*Status:* UNSTABLE (Tests Failed)
*Duration:* ${currentBuild.durationString}

*Details:*
• Some tests may have failed or skipped
• <${env.BUILD_URL}testReport|View Test Results>
• <${env.BUILD_URL}Allure_20Report|View Detailed Report>
• <${env.BUILD_URL}console|View Console>

Please review test results and fix any failing tests. ⚠️"""
                    )
                    echo "✅ Slack notification sent successfully"
                } catch (Exception e) {
                    echo "⚠️  Slack error: ${e.message}"
                }
                
                echo "[2/2] Sending Email notification..."
                try {
                    withCredentials([string(credentialsId: 'recipient-email', variable: 'RECIPIENT')]) {
                    mail(
                        to: RECIPIENT,
                        subject: "⚠️  BUILD UNSTABLE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        mimeType: 'text/html',
                        body: """
<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; color: #333; }
        .header { background-color: #ff9800; color: white; padding: 20px; border-radius: 5px; margin-bottom: 20px; }
        .section { margin: 20px 0; padding: 15px; background-color: #f5f5f5; border-left: 4px solid #ff9800; }
        .section h3 { margin-top: 0; color: #ff9800; }
        .detail { margin: 8px 0; }
        .label { font-weight: bold; color: #333; }
        .value { color: #666; }
        .footer { margin-top: 30px; font-size: 12px; color: #999; border-top: 1px solid #ddd; padding-top: 10px; }
        a { color: #0066cc; text-decoration: none; }
        a:hover { text-decoration: underline; }
        .status-unstable { color: #ff9800; font-weight: bold; }
    </style>
</head>
<body>
    <div class="header">
        <h2>⚠️  Build Unstable - Review Required</h2>
    </div>
    
    <div class="section">
        <h3>Build Information</h3>
        <div class="detail"><span class="label">Job Name:</span> <span class="value">${env.JOB_NAME}</span></div>
        <div class="detail"><span class="label">Build Number:</span> <span class="value">#${env.BUILD_NUMBER}</span></div>
        <div class="detail"><span class="label">Status:</span> <span class="value status-unstable">⚠️  UNSTABLE</span></div>
        <div class="detail"><span class="label">Reason:</span> <span class="value">Test failures detected</span></div>
        <div class="detail"><span class="label">Duration:</span> <span class="value">${currentBuild.durationString}</span></div>
    </div>
    
    <div class="section">
        <h3>Action Items</h3>
        <ul>
            <li><strong>Review</strong> the <a href="${env.BUILD_URL}testReport">Test Report</a> to see which tests failed</li>
            <li><strong>Analyze</strong> test failures in the <a href="${env.BUILD_URL}Allure_20Report">Allure Report</a></li>
            <li><strong>Fix</strong> the failing tests and commit changes</li>
            <li><strong>Verify</strong> with a new build to ensure all tests pass</li>
        </ul>
    </div>
    
    <div class="footer">
        <p>This automated notification was generated by Jenkins Pipeline for <strong>${env.JOB_NAME}</strong></p>
        <p>Generated at: ${new Date().format('yyyy-MM-dd HH:mm:ss')}</p>
    </div>
</body>
</html>
"""
                    )
                    } // end withCredentials
                    echo "✅ Email notification sent successfully"
                } catch (Exception e) {
                    echo "⚠️  Email error: ${e.message}"
                }
                echo "========== UNSTABLE NOTIFICATIONS COMPLETE =========="
            }
        }
        cleanup {
            echo "========== PIPELINE CLEANUP =========="
            cleanWs(deleteDirs: true, patterns: [[pattern: 'parse-results.ps1, slack-payload.json', type: 'INCLUDE']])
            echo "========== PIPELINE COMPLETE =========="
        }
    }
}
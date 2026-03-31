#!/usr/bin/env groovy

// ── Parses one surefire XML string — runs outside CPS so XmlParser works safely
@NonCPS
def parseSurefireXml(String content) {
    def result = [total: 0, failed: 0, skipped: 0, cases: []]
    try {
        def suite = new XmlParser().parseText(content)
        result.total   = (suite.attribute('tests')    ?: '0').toInteger()
        result.failed  = (suite.attribute('failures') ?: '0').toInteger() +
                         (suite.attribute('errors')   ?: '0').toInteger()
        result.skipped = (suite.attribute('skipped')  ?: '0').toInteger()
        for (def tc : suite.children()) {
            if (tc.name() != 'testcase') continue
            for (def child : tc.children()) {
                if (child.name() == 'failure' || child.name() == 'error') {
                    def rawClass   = tc.attribute('classname') ?: 'Unknown'
                    def simpleName = rawClass.contains('.')
                        ? rawClass.substring(rawClass.lastIndexOf('.') + 1)
                        : rawClass
                    def impact = simpleName
                        .replaceAll('Test$', '')
                        .replaceAll(/(?<=[a-z])(?=[A-Z])/, ' ')
                        .trim()
                    def msg = (child.attribute('message') ?: child.text() ?: 'No details')
                        .split('\n')[0].trim()
                    result.cases << [
                        name     : tc.attribute('name') ?: 'Unknown',
                        className: simpleName,
                        message  : msg,
                        impact   : (impact ?: simpleName)
                    ]
                    break
                }
            }
        }
    } catch (Exception ignored) {}
    return result
}

// ── Reads surefire files via pipeline steps, delegates XML work to @NonCPS above
def getTestSummary() {
    def summary = [total: 0, passed: 0, failed: 0, skipped: 0, failedTests: []]
    try {
        def files = findFiles(glob: 'target/surefire-reports/TEST-*.xml')
        for (def file : files) {
            def content = readFile(file.path)
            def parsed  = parseSurefireXml(content)
            summary.total   += parsed.total
            summary.failed  += parsed.failed
            summary.skipped += parsed.skipped
            for (def c : parsed.cases) {
                summary.failedTests << c
            }
        }
        summary.passed = summary.total - summary.failed - summary.skipped
    } catch (Exception e) {
        echo "⚠️  Could not parse surefire reports: ${e.message}"
    }
    return summary
}

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
                            allowMissing         : true,
                            alwaysLinkToLastBuild: true,
                            keepAll              : true,
                            reportDir            : 'target/site/allure-maven-plugin',
                            reportFiles          : 'index.html',
                            reportName           : 'Allure Report'
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
                def ts = getTestSummary()

                // ── Slack ──────────────────────────────────────────────
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

*📊 Test Results:*
• Tests Run: ${ts.total}
• ✅ Passed: ${ts.passed}
• ❌ Failed: ${ts.failed}
• ⏭️ Skipped: ${ts.skipped}

*Reports & Details:*
• <${env.BUILD_URL}|View Full Build>
• <${env.BUILD_URL}testReport|View Test Results>
• <${env.BUILD_URL}Allure_20Report|View Allure Report>
• <${env.BUILD_URL}execution/node/2/ws|View Workspace>

All tests passed — great work! 🎉"""
                    )
                    echo "✅ Slack notification sent successfully"
                } catch (Exception e) {
                    echo "⚠️  Slack error: ${e.message} - Check webhook configuration"
                }

                // ── Email ──────────────────────────────────────────────
                echo "[2/3] Sending Email notification..."
                try {
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
        .stats-table { width: 100%; border-collapse: collapse; margin: 10px 0; }
        .stats-table td { padding: 10px 14px; border: 1px solid #ddd; text-align: center; font-size: 15px; }
        .stat-total { background: #e8f4fd; font-weight: bold; }
        .stat-pass  { background: #d4edda; color: #155724; font-weight: bold; }
        .stat-fail  { background: #f8d7da; color: #721c24; font-weight: bold; }
        .stat-skip  { background: #fff3cd; color: #856404; font-weight: bold; }
        .footer { margin-top: 30px; font-size: 12px; color: #999; border-top: 1px solid #ddd; padding-top: 10px; }
        a { color: #0066cc; text-decoration: none; }
        .status-success { color: #28a745; font-weight: bold; }
    </style>
</head>
<body>
    <div class="header"><h2>✅ Build Passed Successfully</h2></div>

    <div class="section">
        <h3>Build Information</h3>
        <div class="detail"><span class="label">Job Name:</span> <span class="value">${env.JOB_NAME}</span></div>
        <div class="detail"><span class="label">Build Number:</span> <span class="value">#${env.BUILD_NUMBER}</span></div>
        <div class="detail"><span class="label">Status:</span> <span class="value status-success">✅ SUCCESS</span></div>
        <div class="detail"><span class="label">Duration:</span> <span class="value">${currentBuild.durationString}</span></div>
        <div class="detail"><span class="label">Timestamp:</span> <span class="value">${new Date().format('yyyy-MM-dd HH:mm:ss')}</span></div>
    </div>

    <div class="section">
        <h3>📊 Test Results</h3>
        <table class="stats-table">
            <tr>
                <td class="stat-total">🔢 Tests Run<br/><strong>${ts.total}</strong></td>
                <td class="stat-pass">✅ Passed<br/><strong>${ts.passed}</strong></td>
                <td class="stat-fail">❌ Failed<br/><strong>${ts.failed}</strong></td>
                <td class="stat-skip">⏭️ Skipped<br/><strong>${ts.skipped}</strong></td>
            </tr>
        </table>
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

    <div class="footer">
        <p>This automated notification was generated by Jenkins Pipeline for <strong>${env.JOB_NAME}</strong></p>
        <p>Generated at: ${new Date().format('yyyy-MM-dd HH:mm:ss')} | Build Server: ${env.JENKINS_URL}</p>
    </div>
</body>
</html>
"""
                        )
                    }
                    echo "✅ Email notification sent successfully"
                } catch (Exception e) {
                    echo "⚠️  Email error: ${e.message} - Check SMTP configuration"
                }

                // ── Artifacts ──────────────────────────────────────────
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
                def ts = getTestSummary()

                def slackFailedList = ''
                if (ts.failedTests.size() > 0) {
                    ts.failedTests.eachWithIndex { t, i ->
                        slackFailedList += "\n${i + 1}. ❌ *${t.name}* — ${t.className}\n   📝 *Description:* ${t.message}\n   ⚠️ *Impact:* ${t.impact}\n"
                    }
                } else {
                    slackFailedList = '\nNo test failures detected — build failed at a non-test stage. Check console output.\n'
                }

                def emailFailedRows = ''
                if (ts.failedTests.size() > 0) {
                    ts.failedTests.eachWithIndex { t, i ->
                        def rowColor = i % 2 == 0 ? '#fff5f5' : '#ffffff'
                        emailFailedRows += """
                        <tr style="background:${rowColor}">
                            <td style="padding:10px;border:1px solid #ddd;color:#721c24;font-weight:bold;">${i + 1}</td>
                            <td style="padding:10px;border:1px solid #ddd;font-weight:bold;">${t.name}</td>
                            <td style="padding:10px;border:1px solid #ddd;color:#555;">${t.className}</td>
                            <td style="padding:10px;border:1px solid #ddd;">${t.message}</td>
                            <td style="padding:10px;border:1px solid #ddd;color:#c0392b;font-weight:bold;">${t.impact}</td>
                        </tr>"""
                    }
                } else {
                    emailFailedRows = '<tr><td colspan="5" style="padding:10px;border:1px solid #ddd;text-align:center;">No test failures detected — build failed at a non-test stage. Check console output.</td></tr>'
                }

                // ── Slack ──────────────────────────────────────────────
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

*📊 Test Results:*
• Tests Run: ${ts.total}
• ✅ Passed: ${ts.passed}
• ❌ Failed: ${ts.failed}
• ⏭️ Skipped: ${ts.skipped}

*❌ Failed Tests:*${slackFailedList}
*⚠️ Action Required:*
• <${env.BUILD_URL}|View Full Build Log>
• <${env.BUILD_URL}testReport|View Test Failures>
• <${env.BUILD_URL}Allure_20Report|View Allure Report>
• <${env.BUILD_URL}console|View Console Output>

Please investigate and fix the build issues. ⛔"""
                    )
                    echo "✅ Slack notification sent successfully"
                } catch (Exception e) {
                    echo "⚠️  Slack error: ${e.message} - Check webhook configuration"
                }

                // ── Email ──────────────────────────────────────────────
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
        .stats-table { width: 100%; border-collapse: collapse; margin: 10px 0; }
        .stats-table td { padding: 10px 14px; border: 1px solid #ddd; text-align: center; font-size: 15px; }
        .stat-total { background: #e8f4fd; font-weight: bold; }
        .stat-pass  { background: #d4edda; color: #155724; font-weight: bold; }
        .stat-fail  { background: #f8d7da; color: #721c24; font-weight: bold; }
        .stat-skip  { background: #fff3cd; color: #856404; font-weight: bold; }
        .failures-table { width: 100%; border-collapse: collapse; margin: 10px 0; font-size: 13px; }
        .failures-table th { background: #d32f2f; color: white; padding: 10px; text-align: left; border: 1px solid #c0392b; }
        .warning-box { background-color: #fff3cd; border: 1px solid #ffc107; padding: 15px; border-radius: 4px; margin: 15px 0; }
        .footer { margin-top: 30px; font-size: 12px; color: #999; border-top: 1px solid #ddd; padding-top: 10px; }
        a { color: #0066cc; text-decoration: none; }
        .status-failure { color: #d32f2f; font-weight: bold; }
    </style>
</head>
<body>
    <div class="header"><h2>❌ Build Failed — Action Required</h2></div>

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
        <h3>📊 Test Results</h3>
        <table class="stats-table">
            <tr>
                <td class="stat-total">🔢 Tests Run<br/><strong>${ts.total}</strong></td>
                <td class="stat-pass">✅ Passed<br/><strong>${ts.passed}</strong></td>
                <td class="stat-fail">❌ Failed<br/><strong>${ts.failed}</strong></td>
                <td class="stat-skip">⏭️ Skipped<br/><strong>${ts.skipped}</strong></td>
            </tr>
        </table>
    </div>

    <div class="section">
        <h3>❌ Failed Tests</h3>
        <table class="failures-table">
            <tr>
                <th>#</th><th>Test Name</th><th>Class</th><th>Description</th><th>Impact</th>
            </tr>
            ${emailFailedRows}
        </table>
    </div>

    <div class="section">
        <h3>🔗 Resources</h3>
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
            <li>Fix the issues in your code and commit</li>
            <li>Push changes to trigger a new build</li>
        </ol>
    </div>

    <div class="footer">
        <p>This automated notification was generated by Jenkins Pipeline for <strong>${env.JOB_NAME}</strong></p>
        <p>Generated at: ${new Date().format('yyyy-MM-dd HH:mm:ss')} | Build Server: ${env.JENKINS_URL}</p>
    </div>
</body>
</html>
"""
                        )
                    }
                    echo "✅ Email notification sent successfully"
                } catch (Exception e) {
                    echo "⚠️  Email error: ${e.message} - Check SMTP configuration"
                }

                // ── Artifacts ──────────────────────────────────────────
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
                def ts = getTestSummary()

                def slackFailedList = ''
                if (ts.failedTests.size() > 0) {
                    ts.failedTests.eachWithIndex { t, i ->
                        slackFailedList += "\n${i + 1}. ❌ *${t.name}* — ${t.className}\n   📝 *Description:* ${t.message}\n   ⚠️ *Impact:* ${t.impact}\n"
                    }
                } else {
                    slackFailedList = '\nNo individual test data available — check the Allure Report.\n'
                }

                def emailFailedRows = ''
                if (ts.failedTests.size() > 0) {
                    ts.failedTests.eachWithIndex { t, i ->
                        def rowColor = i % 2 == 0 ? '#fff5f5' : '#ffffff'
                        emailFailedRows += """
                        <tr style="background:${rowColor}">
                            <td style="padding:10px;border:1px solid #ddd;color:#721c24;font-weight:bold;">${i + 1}</td>
                            <td style="padding:10px;border:1px solid #ddd;font-weight:bold;">${t.name}</td>
                            <td style="padding:10px;border:1px solid #ddd;color:#555;">${t.className}</td>
                            <td style="padding:10px;border:1px solid #ddd;">${t.message}</td>
                            <td style="padding:10px;border:1px solid #ddd;color:#c0392b;font-weight:bold;">${t.impact}</td>
                        </tr>"""
                    }
                } else {
                    emailFailedRows = '<tr><td colspan="5" style="padding:10px;border:1px solid #ddd;text-align:center;">No individual test data available — check the Allure Report.</td></tr>'
                }

                // ── Slack ──────────────────────────────────────────────
                echo "[1/2] Sending Slack notification..."
                try {
                    slackSend(
                        color: '#ff9800',
                        channel: '#jenkins-notifications',
                        botUser: false,
                        baseUrl: 'https://hooks.slack.com/services/',
                        message: """⚠️ *BUILD UNSTABLE*
*Job:* ${env.JOB_NAME}
*Build #:* ${env.BUILD_NUMBER}
*Status:* UNSTABLE (Tests Failed)
*Duration:* ${currentBuild.durationString}

*📊 Test Results:*
• Tests Run: ${ts.total}
• ✅ Passed: ${ts.passed}
• ❌ Failed: ${ts.failed}
• ⏭️ Skipped: ${ts.skipped}

*❌ Failed Tests:*${slackFailedList}
*Details:*
• <${env.BUILD_URL}testReport|View Test Results>
• <${env.BUILD_URL}Allure_20Report|View Detailed Report>
• <${env.BUILD_URL}console|View Console>

Please review and fix the failing tests. ⚠️"""
                    )
                    echo "✅ Slack notification sent successfully"
                } catch (Exception e) {
                    echo "⚠️  Slack error: ${e.message}"
                }

                // ── Email ──────────────────────────────────────────────
                echo "[2/2] Sending Email notification..."
                try {
                    withCredentials([string(credentialsId: 'recipient-email', variable: 'RECIPIENT')]) {
                        mail(
                            to: RECIPIENT,
                            subject: "⚠️ BUILD UNSTABLE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                            mimeType: 'text/html',
                            body: """
<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; color: #333; }
        .header { background-color: #ff9800; color: white; padding: 20px; border-radius: 5px; margin-bottom: 20px; }
        .section { margin: 20px 0; padding: 15px; background-color: #f5f5f5; border-left: 4px solid #ff9800; }
        .section h3 { margin-top: 0; color: #e65100; }
        .detail { margin: 8px 0; }
        .label { font-weight: bold; color: #333; }
        .value { color: #666; }
        .stats-table { width: 100%; border-collapse: collapse; margin: 10px 0; }
        .stats-table td { padding: 10px 14px; border: 1px solid #ddd; text-align: center; font-size: 15px; }
        .stat-total { background: #e8f4fd; font-weight: bold; }
        .stat-pass  { background: #d4edda; color: #155724; font-weight: bold; }
        .stat-fail  { background: #f8d7da; color: #721c24; font-weight: bold; }
        .stat-skip  { background: #fff3cd; color: #856404; font-weight: bold; }
        .failures-table { width: 100%; border-collapse: collapse; margin: 10px 0; font-size: 13px; }
        .failures-table th { background: #e65100; color: white; padding: 10px; text-align: left; border: 1px solid #bf360c; }
        .footer { margin-top: 30px; font-size: 12px; color: #999; border-top: 1px solid #ddd; padding-top: 10px; }
        a { color: #0066cc; text-decoration: none; }
        .status-unstable { color: #e65100; font-weight: bold; }
    </style>
</head>
<body>
    <div class="header"><h2>⚠️ Build Unstable — Review Required</h2></div>

    <div class="section">
        <h3>Build Information</h3>
        <div class="detail"><span class="label">Job Name:</span> <span class="value">${env.JOB_NAME}</span></div>
        <div class="detail"><span class="label">Build Number:</span> <span class="value">#${env.BUILD_NUMBER}</span></div>
        <div class="detail"><span class="label">Status:</span> <span class="value status-unstable">⚠️ UNSTABLE</span></div>
        <div class="detail"><span class="label">Reason:</span> <span class="value">Test failures detected</span></div>
        <div class="detail"><span class="label">Duration:</span> <span class="value">${currentBuild.durationString}</span></div>
    </div>

    <div class="section">
        <h3>📊 Test Results</h3>
        <table class="stats-table">
            <tr>
                <td class="stat-total">🔢 Tests Run<br/><strong>${ts.total}</strong></td>
                <td class="stat-pass">✅ Passed<br/><strong>${ts.passed}</strong></td>
                <td class="stat-fail">❌ Failed<br/><strong>${ts.failed}</strong></td>
                <td class="stat-skip">⏭️ Skipped<br/><strong>${ts.skipped}</strong></td>
            </tr>
        </table>
    </div>

    <div class="section">
        <h3>❌ Failed Tests</h3>
        <table class="failures-table">
            <tr>
                <th>#</th><th>Test Name</th><th>Class</th><th>Description</th><th>Impact</th>
            </tr>
            ${emailFailedRows}
        </table>
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
                    }
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
#!/usr/bin/env groovy

// Posts directly to the incoming webhook URL stored in the credential.
// Bypasses the Slack plugin entirely — no teamDomain, no baseUrl issues.
def sendSlack(String color, String message) {
    try {
        def payload = groovy.json.JsonOutput.toJson([
            attachments: [[
                color    : color,
                text     : message,
                mrkdwn_in: ['text']
            ]]
        ])
        writeFile file: 'slack-notify.json', text: payload
        withCredentials([string(credentialsId: 'incoming-webhook', variable: 'SLACK_URL')]) {
            bat 'powershell -NoProfile -ExecutionPolicy Bypass -Command "$b = Get-Content slack-notify.json -Raw; Invoke-RestMethod -Uri $env:SLACK_URL -Method Post -ContentType application/json -Body $b"'
        }
        echo "✅ Slack notification sent successfully"
    } catch (Exception e) {
        echo "⚠️  Slack error: ${e.message}"
    }
}

def getTestSummary() {
    def summary = [total: 0, passed: 0, failed: 0, skipped: 0, failedTests: []]
    try {
        if (!fileExists('test-summary.txt')) {
            echo "⚠️  test-summary.txt not found"
            return summary
        }
        def content = readFile('test-summary.txt')
        def lines = content.split('\n')
        for (def line : lines) {
            line = line.replaceAll('\r', '').trim()
            if (line.isEmpty()) continue
            if      (line.startsWith('TOTAL:'))   { summary.total   = line.substring(6).trim().toInteger() }
            else if (line.startsWith('PASSED:'))  { summary.passed  = line.substring(7).trim().toInteger() }
            else if (line.startsWith('FAILED:'))  { summary.failed  = line.substring(7).trim().toInteger() }
            else if (line.startsWith('SKIPPED:')) { summary.skipped = line.substring(8).trim().toInteger() }
            else if (line.startsWith('TEST:')) {
                def parts = line.substring(5).split('\\|\\|')
                if (parts.length >= 3) {
                    def rawClass   = parts[0].trim()
                    def simpleName = rawClass.contains('.')
                        ? rawClass.substring(rawClass.lastIndexOf('.') + 1)
                        : rawClass
                    def impact = simpleName.endsWith('Test')
                        ? simpleName.substring(0, simpleName.length() - 4)
                        : simpleName
                    summary.failedTests << [
                        name     : parts[1].trim(),
                        className: simpleName,
                        message  : parts[2].trim(),
                        impact   : (impact ?: simpleName)
                    ]
                }
            }
        }
        echo "Parsed summary → total:${summary.total} passed:${summary.passed} failed:${summary.failed} skipped:${summary.skipped} tests:${summary.failedTests.size()}"
    } catch (Exception e) {
        echo "⚠️  Could not parse test summary: ${e.message}"
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

        stage('Parse Test Results') {
            steps {
                catchError(buildResult: currentBuild.result ?: 'SUCCESS', stageResult: 'UNSTABLE') {
                    writeFile file: 'parse-tests.ps1', text: '''
$total   = 0
$failed  = 0
$skipped = 0
$lines   = @()

$files = Get-ChildItem "target\\surefire-reports\\TEST-*.xml" -ErrorAction SilentlyContinue
Write-Output "Found $($files.Count) surefire XML file(s)"

foreach ($f in $files) {
    [xml]$xml = Get-Content $f.FullName -Encoding UTF8
    $suite = $xml.testsuite
    $testcases = @($suite.testcase)
    Write-Output "  $($f.Name): $($testcases.Count) testcase(s)"
    foreach ($tc in $testcases) {
        if ($tc -eq $null) { continue }
        $total++
        if ($tc.skipped -ne $null) {
            $skipped++
        } elseif ($tc.failure -ne $null -or $tc.error -ne $null) {
            $failed++
            $fn  = if ($tc.failure -ne $null) { $tc.failure } else { $tc.error }
            $msg = ""
            if ($fn -is [System.Xml.XmlElement]) {
                $msg = if ($fn.GetAttribute("message")) { $fn.GetAttribute("message") } else { $fn.InnerText }
            } elseif ($fn -is [string]) {
                $msg = $fn
            }
            $msg = (($msg -split "`n")[0]).Trim() -replace "\\|\\|", "-"
            $lines += "TEST:" + $tc.classname + "||" + $tc.name + "||" + $msg
        }
    }
}

$passed = $total - $failed - $skipped

$writer = New-Object System.IO.StreamWriter("test-summary.txt", $false, [System.Text.Encoding]::ASCII)
$writer.WriteLine("TOTAL:$total")
$writer.WriteLine("PASSED:$passed")
$writer.WriteLine("FAILED:$failed")
$writer.WriteLine("SKIPPED:$skipped")
foreach ($line in $lines) {
    $writer.WriteLine($line)
}
$writer.Close()

Write-Output "Done: total=$total passed=$passed failed=$failed skipped=$skipped"
'''
                    bat 'powershell -NoProfile -ExecutionPolicy Bypass -File parse-tests.ps1'
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
                sendSlack('#36a64f', """✅ *BUILD PASSED*
*Job:* ${env.JOB_NAME}
*Build #:* ${env.BUILD_NUMBER}
*Duration:* ${currentBuild.durationString}
*Branch:* ${env.GIT_BRANCH ?: 'N/A'}
*Commit:* ${env.GIT_COMMIT?.take(7) ?: 'N/A'}

*📊 Test Results:*
• Tests Run: ${ts.total}
• ✅ Passed: ${ts.passed}
• ⚠️ Reviewed: ${ts.failed}
• ⏭️ Skipped: ${ts.skipped}

*Reports:*
• <${env.BUILD_URL}|View Full Build>
• <${env.BUILD_URL}testReport|Test Results>
• <${env.BUILD_URL}Allure_20Report|Allure Report>

All tests passed — great work! 🎉""")

                // ── Email ──────────────────────────────────────────────
                echo "[2/3] Sending Email notification..."
                try {
                    withCredentials([
                        string(credentialsId: 'recipient-email', variable: 'RECIPIENT'),
                        string(credentialsId: 'gmail-address',   variable: 'SENDER')
                    ]) {
                        mail(
                            from   : SENDER,
                            to     : RECIPIENT,
                            subject: "✅ Build Passed — ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                            mimeType: 'text/html',
                            body: """
<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; color: #333; margin: 0; padding: 0; }
        .header { background-color: #28a745; color: white; padding: 20px; border-radius: 5px 5px 0 0; margin-bottom: 0; }
        .header h2 { margin: 0; font-size: 20px; }
        .header p  { margin: 5px 0 0 0; opacity: 0.85; font-size: 13px; }
        .body { padding: 20px; }
        .section { margin: 16px 0; padding: 15px; background-color: #f9f9f9; border-left: 4px solid #28a745; border-radius: 0 4px 4px 0; }
        .section h3 { margin-top: 0; color: #28a745; font-size: 14px; text-transform: uppercase; letter-spacing: 0.5px; }
        .detail { margin: 6px 0; font-size: 13px; }
        .label { font-weight: bold; color: #555; }
        .stats-table { width: 100%; border-collapse: collapse; margin: 8px 0; }
        .stats-table td { padding: 12px 10px; border: 1px solid #e0e0e0; text-align: center; font-size: 14px; border-radius: 3px; }
        .stat-total { background: #e8f4fd; }
        .stat-pass  { background: #d4edda; color: #155724; font-weight: bold; }
        .stat-review { background: #fff3cd; color: #856404; font-weight: bold; }
        .stat-skip  { background: #f5f5f5; color: #666; }
        .footer { margin-top: 24px; font-size: 11px; color: #aaa; border-top: 1px solid #eee; padding-top: 12px; }
        a { color: #0066cc; text-decoration: none; }
    </style>
</head>
<body>
<div class="header">
    <h2>✅ Build Completed Successfully</h2>
    <p>${env.JOB_NAME} · Build #${env.BUILD_NUMBER}</p>
</div>
<div class="body">
    <div class="section">
        <h3>Build Details</h3>
        <div class="detail"><span class="label">Job:</span> ${env.JOB_NAME}</div>
        <div class="detail"><span class="label">Build:</span> #${env.BUILD_NUMBER}</div>
        <div class="detail"><span class="label">Branch:</span> ${env.GIT_BRANCH ?: 'N/A'}</div>
        <div class="detail"><span class="label">Duration:</span> ${currentBuild.durationString}</div>
        <div class="detail"><span class="label">Completed:</span> ${new Date().format('yyyy-MM-dd HH:mm:ss')}</div>
    </div>

    <div class="section">
        <h3>📊 Test Summary</h3>
        <table class="stats-table">
            <tr>
                <td class="stat-total">🔢 Total<br/><strong>${ts.total}</strong></td>
                <td class="stat-pass">✅ Passed<br/><strong>${ts.passed}</strong></td>
                <td class="stat-review">⚠️ Reviewed<br/><strong>${ts.failed}</strong></td>
                <td class="stat-skip">⏭️ Skipped<br/><strong>${ts.skipped}</strong></td>
            </tr>
        </table>
    </div>

    <div class="section">
        <h3>🔗 Reports</h3>
        <div class="detail">• <a href="${env.BUILD_URL}">Build Log</a></div>
        <div class="detail">• <a href="${env.BUILD_URL}testReport">Test Results</a></div>
        <div class="detail">• <a href="${env.BUILD_URL}Allure_20Report">Allure Report</a></div>
    </div>

    <div class="footer">
        Automated notification · ${env.JOB_NAME} · ${env.JENKINS_URL}
    </div>
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
                        slackFailedList += "\n${i + 1}. *${t.name}* (${t.className})\n   📝 ${t.message}\n   ⚡ Area: ${t.impact}\n"
                    }
                } else {
                    slackFailedList = '\nNo test data available — build may have failed before tests ran. Check console output.\n'
                }

                def emailTestRows = ''
                if (ts.failedTests.size() > 0) {
                    ts.failedTests.eachWithIndex { t, i ->
                        def rowBg = i % 2 == 0 ? '#fff8f8' : '#ffffff'
                        emailTestRows += """
                        <tr style="background:${rowBg}">
                            <td style="padding:10px;border:1px solid #e0e0e0;text-align:center;font-weight:bold;color:#c0392b;">${i + 1}</td>
                            <td style="padding:10px;border:1px solid #e0e0e0;font-weight:bold;">${t.name}</td>
                            <td style="padding:10px;border:1px solid #e0e0e0;color:#666;">${t.className}</td>
                            <td style="padding:10px;border:1px solid #e0e0e0;">${t.message}</td>
                            <td style="padding:10px;border:1px solid #e0e0e0;font-weight:bold;">${t.impact}</td>
                        </tr>"""
                    }
                } else {
                    emailTestRows = '<tr><td colspan="5" style="padding:12px;border:1px solid #e0e0e0;text-align:center;color:#888;">No individual test data — build may have failed before tests ran.</td></tr>'
                }

                echo "[1/3] Sending Slack notification..."
                sendSlack('#d32f2f', """🔴 *Build Requires Attention*
*Job:* ${env.JOB_NAME}
*Build #:* ${env.BUILD_NUMBER}
*Duration:* ${currentBuild.durationString}
*Branch:* ${env.GIT_BRANCH ?: 'N/A'}
*Commit:* ${env.GIT_COMMIT?.take(7) ?: 'N/A'}

*📊 Test Results:*
• Total: ${ts.total} · ✅ ${ts.passed} passed · ⚠️ ${ts.failed} to review · ⏭️ ${ts.skipped} skipped

*Tests to Review:*${slackFailedList}
*Links:*
• <${env.BUILD_URL}|Full Build Log>
• <${env.BUILD_URL}testReport|Test Results>
• <${env.BUILD_URL}Allure_20Report|Allure Report>
• <${env.BUILD_URL}console|Console Output>""")

                echo "[2/3] Sending Email notification..."
                try {
                    withCredentials([
                        string(credentialsId: 'recipient-email', variable: 'RECIPIENT'),
                        string(credentialsId: 'gmail-address',   variable: 'SENDER')
                    ]) {
                        mail(
                            from   : SENDER,
                            to     : RECIPIENT,
                            subject: "🔴 Build Requires Attention — ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                            mimeType: 'text/html',
                            body: """
<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; color: #333; margin: 0; padding: 0; }
        .header { background-color: #c0392b; color: white; padding: 20px; border-radius: 5px 5px 0 0; }
        .header h2 { margin: 0; font-size: 20px; }
        .header p  { margin: 5px 0 0 0; opacity: 0.85; font-size: 13px; }
        .body { padding: 20px; }
        .section { margin: 16px 0; padding: 15px; background-color: #f9f9f9; border-left: 4px solid #c0392b; border-radius: 0 4px 4px 0; }
        .section h3 { margin-top: 0; color: #c0392b; font-size: 14px; text-transform: uppercase; letter-spacing: 0.5px; }
        .detail { margin: 6px 0; font-size: 13px; }
        .label { font-weight: bold; color: #555; }
        .stats-table { width: 100%; border-collapse: collapse; margin: 8px 0; }
        .stats-table td { padding: 12px 10px; border: 1px solid #e0e0e0; text-align: center; font-size: 14px; }
        .stat-total  { background: #e8f4fd; }
        .stat-pass   { background: #d4edda; color: #155724; font-weight: bold; }
        .stat-review { background: #fff3cd; color: #856404; font-weight: bold; }
        .stat-skip   { background: #f5f5f5; color: #666; }
        .test-table  { width: 100%; border-collapse: collapse; font-size: 12px; }
        .test-table th { background: #c0392b; color: white; padding: 10px; text-align: left; }
        .footer { margin-top: 24px; font-size: 11px; color: #aaa; border-top: 1px solid #eee; padding-top: 12px; }
        a { color: #0066cc; text-decoration: none; }
    </style>
</head>
<body>
<div class="header">
    <h2>🔴 Build Requires Attention</h2>
    <p>${env.JOB_NAME} · Build #${env.BUILD_NUMBER}</p>
</div>
<div class="body">
    <div class="section">
        <h3>Build Details</h3>
        <div class="detail"><span class="label">Job:</span> ${env.JOB_NAME}</div>
        <div class="detail"><span class="label">Build:</span> #${env.BUILD_NUMBER}</div>
        <div class="detail"><span class="label">Branch:</span> ${env.GIT_BRANCH ?: 'N/A'}</div>
        <div class="detail"><span class="label">Duration:</span> ${currentBuild.durationString}</div>
        <div class="detail"><span class="label">Timestamp:</span> ${new Date().format('yyyy-MM-dd HH:mm:ss')}</div>
    </div>

    <div class="section">
        <h3>📊 Test Summary</h3>
        <table class="stats-table">
            <tr>
                <td class="stat-total">🔢 Total<br/><strong>${ts.total}</strong></td>
                <td class="stat-pass">✅ Passed<br/><strong>${ts.passed}</strong></td>
                <td class="stat-review">⚠️ To Review<br/><strong>${ts.failed}</strong></td>
                <td class="stat-skip">⏭️ Skipped<br/><strong>${ts.skipped}</strong></td>
            </tr>
        </table>
    </div>

    <div class="section">
        <h3>🔍 Tests to Review</h3>
        <table class="test-table">
            <tr>
                <th>#</th>
                <th>Test Name</th>
                <th>Class</th>
                <th>Description</th>
                <th>Area</th>
            </tr>
            ${emailTestRows}
        </table>
    </div>

    <div class="section">
        <h3>🔗 Resources</h3>
        <div class="detail">• <a href="${env.BUILD_URL}console">Console Output</a></div>
        <div class="detail">• <a href="${env.BUILD_URL}testReport">Test Results</a></div>
        <div class="detail">• <a href="${env.BUILD_URL}Allure_20Report">Allure Report</a></div>
        <div class="detail">• <a href="${env.BUILD_URL}">Full Build Details</a></div>
    </div>

    <div class="section">
        <h3>Next Steps</h3>
        <div class="detail">1. Review the console output to identify the root cause</div>
        <div class="detail">2. Check the Allure Report for detailed test analytics</div>
        <div class="detail">3. Apply fixes and push to trigger a new build</div>
    </div>

    <div class="footer">
        Automated notification · ${env.JOB_NAME} · ${env.JENKINS_URL}
    </div>
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

                echo "[3/3] Archiving artifacts..."
                archiveArtifacts artifacts: 'target/site/allure-maven-plugin/**,target/surefire-reports/**', allowEmptyArchive: true
                junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                echo "✅ Artifacts archived"
                echo "========== FAILURE NOTIFICATIONS COMPLETE =========="
            }
        }

        unstable {
            script {
                echo "========== SENDING BUILD REVIEW NOTIFICATIONS =========="
                def ts = getTestSummary()

                def slackFailedList = ''
                if (ts.failedTests.size() > 0) {
                    ts.failedTests.eachWithIndex { t, i ->
                        slackFailedList += "\n${i + 1}. *${t.name}* (${t.className})\n   📝 ${t.message}\n   ⚡ Area: ${t.impact}\n"
                    }
                } else {
                    slackFailedList = '\nNo individual test data available — check the Allure Report.\n'
                }

                def emailTestRows = ''
                if (ts.failedTests.size() > 0) {
                    ts.failedTests.eachWithIndex { t, i ->
                        def rowBg = i % 2 == 0 ? '#fff8f8' : '#ffffff'
                        emailTestRows += """
                        <tr style="background:${rowBg}">
                            <td style="padding:10px;border:1px solid #e0e0e0;text-align:center;font-weight:bold;color:#c0392b;">${i + 1}</td>
                            <td style="padding:10px;border:1px solid #e0e0e0;font-weight:bold;">${t.name}</td>
                            <td style="padding:10px;border:1px solid #e0e0e0;color:#666;">${t.className}</td>
                            <td style="padding:10px;border:1px solid #e0e0e0;">${t.message}</td>
                            <td style="padding:10px;border:1px solid #e0e0e0;font-weight:bold;">${t.impact}</td>
                        </tr>"""
                    }
                } else {
                    emailTestRows = '<tr><td colspan="5" style="padding:12px;border:1px solid #e0e0e0;text-align:center;color:#888;">No individual test data available — check the Allure Report.</td></tr>'
                }

                echo "[1/2] Sending Slack notification..."
                sendSlack('#ff9800', """⚠️ *Build Review Required*
*Job:* ${env.JOB_NAME}
*Build #:* ${env.BUILD_NUMBER}
*Duration:* ${currentBuild.durationString}

*📊 Test Results:*
• Total: ${ts.total} · ✅ ${ts.passed} passed · ⚠️ ${ts.failed} to review · ⏭️ ${ts.skipped} skipped

*Tests to Review:*${slackFailedList}
*Links:*
• <${env.BUILD_URL}testReport|Test Results>
• <${env.BUILD_URL}Allure_20Report|Allure Report>
• <${env.BUILD_URL}console|Console Output>""")

                echo "[2/2] Sending Email notification..."
                try {
                    withCredentials([
                        string(credentialsId: 'recipient-email', variable: 'RECIPIENT'),
                        string(credentialsId: 'gmail-address',   variable: 'SENDER')
                    ]) {
                        mail(
                            from   : SENDER,
                            to     : RECIPIENT,
                            subject: "⚠️ Build Review Required — ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                            mimeType: 'text/html',
                            body: """
<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; color: #333; margin: 0; padding: 0; }
        .header { background-color: #e67e22; color: white; padding: 20px; border-radius: 5px 5px 0 0; }
        .header h2 { margin: 0; font-size: 20px; }
        .header p  { margin: 5px 0 0 0; opacity: 0.85; font-size: 13px; }
        .body { padding: 20px; }
        .section { margin: 16px 0; padding: 15px; background-color: #f9f9f9; border-left: 4px solid #e67e22; border-radius: 0 4px 4px 0; }
        .section h3 { margin-top: 0; color: #e67e22; font-size: 14px; text-transform: uppercase; letter-spacing: 0.5px; }
        .detail { margin: 6px 0; font-size: 13px; }
        .label { font-weight: bold; color: #555; }
        .stats-table { width: 100%; border-collapse: collapse; margin: 8px 0; }
        .stats-table td { padding: 12px 10px; border: 1px solid #e0e0e0; text-align: center; font-size: 14px; }
        .stat-total  { background: #e8f4fd; }
        .stat-pass   { background: #d4edda; color: #155724; font-weight: bold; }
        .stat-review { background: #fff3cd; color: #856404; font-weight: bold; }
        .stat-skip   { background: #f5f5f5; color: #666; }
        .test-table  { width: 100%; border-collapse: collapse; font-size: 12px; }
        .test-table th { background: #e67e22; color: white; padding: 10px; text-align: left; }
        .footer { margin-top: 24px; font-size: 11px; color: #aaa; border-top: 1px solid #eee; padding-top: 12px; }
        a { color: #0066cc; text-decoration: none; }
    </style>
</head>
<body>
<div class="header">
    <h2>⚠️ Build Review Required</h2>
    <p>${env.JOB_NAME} · Build #${env.BUILD_NUMBER}</p>
</div>
<div class="body">
    <div class="section">
        <h3>Build Details</h3>
        <div class="detail"><span class="label">Job:</span> ${env.JOB_NAME}</div>
        <div class="detail"><span class="label">Build:</span> #${env.BUILD_NUMBER}</div>
        <div class="detail"><span class="label">Duration:</span> ${currentBuild.durationString}</div>
        <div class="detail"><span class="label">Timestamp:</span> ${new Date().format('yyyy-MM-dd HH:mm:ss')}</div>
    </div>

    <div class="section">
        <h3>📊 Test Summary</h3>
        <table class="stats-table">
            <tr>
                <td class="stat-total">🔢 Total<br/><strong>${ts.total}</strong></td>
                <td class="stat-pass">✅ Passed<br/><strong>${ts.passed}</strong></td>
                <td class="stat-review">⚠️ To Review<br/><strong>${ts.failed}</strong></td>
                <td class="stat-skip">⏭️ Skipped<br/><strong>${ts.skipped}</strong></td>
            </tr>
        </table>
    </div>

    <div class="section">
        <h3>🔍 Tests to Review</h3>
        <table class="test-table">
            <tr>
                <th>#</th>
                <th>Test Name</th>
                <th>Class</th>
                <th>Description</th>
                <th>Area</th>
            </tr>
            ${emailTestRows}
        </table>
    </div>

    <div class="section">
        <h3>🔗 Resources</h3>
        <div class="detail">• <a href="${env.BUILD_URL}testReport">Test Results</a></div>
        <div class="detail">• <a href="${env.BUILD_URL}Allure_20Report">Allure Report</a></div>
        <div class="detail">• <a href="${env.BUILD_URL}console">Console Output</a></div>
        <div class="detail">• <a href="${env.BUILD_URL}">Full Build Details</a></div>
    </div>

    <div class="section">
        <h3>Next Steps</h3>
        <div class="detail">1. Review the tests listed above and examine the Allure Report for detailed analytics</div>
        <div class="detail">2. Apply the necessary fixes and push to trigger a new build</div>
        <div class="detail">3. Verify all tests pass on the next run</div>
    </div>

    <div class="footer">
        Automated notification · ${env.JOB_NAME} · ${env.JENKINS_URL}
    </div>
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
                echo "========== BUILD REVIEW NOTIFICATIONS COMPLETE =========="
            }
        }

        cleanup {
            echo "========== PIPELINE CLEANUP =========="
            cleanWs(deleteDirs: true, patterns: [[pattern: 'parse-tests.ps1, test-summary.txt, slack-notify.json', type: 'INCLUDE']])
            echo "========== PIPELINE COMPLETE =========="
        }
    }
}
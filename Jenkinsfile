#!/usr/bin/env groovy

// ── Sends a plain-text Slack attachment (signature unchanged) ─────────────────
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

// ── Sends a Block Kit payload (used for rich failure notifications) ────────────
def sendSlackBlocks(String color, List blocks) {
    try {
        def payload = groovy.json.JsonOutput.toJson([
            attachments: [[color: color, blocks: blocks]]
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

// ── Reads test-summary.txt written by the Parse Test Results stage ────────────
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

// ── Reads allure-results JSON for rich failure detail (id, severity, expected/actual)
def getAllureFailures() {
    def failures = []
    try {
        if (!fileExists('target/allure-results')) return failures
        writeFile file: 'read-allure.ps1', text: '''
$out = @()
$files = Get-ChildItem "target/allure-results/*-result.json" -ErrorAction SilentlyContinue
foreach ($f in $files) {
    try {
        $data = Get-Content $f.FullName -Raw | ConvertFrom-Json
        if ($data.status -in @("failed","broken")) {
            $name    = if ($data.name) { ($data.name -replace "\\|\\|","-").Trim() } else { "Unknown" }
            $desc    = if ($data.description) { (($data.description -split "`n")[0]).Trim() -replace "\\|\\|","-" } else { "N/A" }
            $fullMsg = if ($data.statusDetails -and $data.statusDetails.message) { $data.statusDetails.message } else { "N/A" }
            $shortMsg= (($fullMsg -split "`n")[0]).Trim() -replace "\\|\\|","-"
            $labels  = @{}
            if ($data.labels) { foreach ($l in $data.labels) { $labels[$l.name] = $l.value } }
            $aid      = if ($labels["allureId"]) { $labels["allureId"] } else { "N/A" }
            $severity = if ($labels["severity"]) { $labels["severity"] } else { "normal" }
            $tClass   = if ($labels["testClass"]) { ($labels["testClass"] -split "[.]")[-1] } else { "N/A" }
            $expected = if ($fullMsg -match "Expected:[ ]*<(.*?)>")           { $Matches[1] } else { "N/A" }
            $actual   = if ($fullMsg -match "(?:Actual|but was):[ ]*<(.*?)>") { $Matches[1] } else { "N/A" }
            $out += "$aid||$name||$tClass||$severity||$desc||$shortMsg||$expected||$actual"
        }
    } catch { }
}
$writer = New-Object System.IO.StreamWriter("allure-failures.txt", $false, [System.Text.Encoding]::ASCII)
foreach ($line in $out) { $writer.WriteLine($line) }
$writer.Close()
Write-Output "Allure: $($out.Count) failure(s) found"
'''
        bat 'powershell -NoProfile -ExecutionPolicy Bypass -File read-allure.ps1'
        if (fileExists('allure-failures.txt')) {
            def lines = readFile('allure-failures.txt').split('\n')
            for (def line : lines) {
                line = line.replaceAll('\r', '').trim()
                if (line.isEmpty()) continue
                def parts = line.split('\\|\\|', -1)
                if (parts.length >= 8) {
                    failures << [
                        id       : parts[0].trim(),
                        name     : parts[1].trim(),
                        className: parts[2].trim(),
                        severity : parts[3].trim().capitalize(),
                        desc     : parts[4].trim(),
                        message  : parts[5].trim(),
                        expected : parts[6].trim(),
                        actual   : parts[7].trim()
                    ]
                }
            }
        }
    } catch (Exception e) {
        echo "⚠️  Could not read allure failures: ${e.message}"
    }
    return failures
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
                        echo "Allure report generated successfully."
                    } else {
                        echo "Warning: Allure report not found at expected location."
                    }
                }
            }
        }

        stage('Publish to GitHub Pages') {
            when {
                expression { fileExists('target/site/allure-maven-plugin/index.html') }
            }
            steps {
                catchError(buildResult: currentBuild.result ?: 'SUCCESS', stageResult: 'UNSTABLE') {
                    withCredentials([string(credentialsId: 'github-pat', variable: 'GH_PAT')]) {
                        writeFile file: 'publish-ghpages.ps1', text: '''
$repo = "https://$env:GH_PAT@github.com/DivineBAYINGANA/api-test-automation-jenkins.git"

if (Test-Path "gh-pages-deploy") {
    Remove-Item "gh-pages-deploy" -Recurse -Force
}

# Try to clone existing gh-pages branch (redirect stderr so git progress does not kill the script)
git clone --branch gh-pages --single-branch $repo gh-pages-deploy 2>&1 | Out-Null

if (Test-Path "gh-pages-deploy/.git") {
    Write-Output "Cloned existing gh-pages branch"
    Push-Location "gh-pages-deploy"
    # Clear old content but keep .git
    Get-ChildItem -Force | Where-Object { $_.Name -ne ".git" } | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
} else {
    Write-Output "gh-pages branch not found - creating fresh"
    New-Item -ItemType Directory "gh-pages-deploy" | Out-Null
    Push-Location "gh-pages-deploy"
    git init 2>&1 | Out-Null
    git remote add origin $repo 2>&1 | Out-Null
    git checkout -b gh-pages 2>&1 | Out-Null
}

# Copy new allure report
Copy-Item -Path "../target/site/allure-maven-plugin/*" -Destination "." -Recurse -Force

git config user.email "jenkins@build"
git config user.name "Jenkins CI"
git add -A 2>&1 | Out-Null

$status = git status --porcelain
if ($status) {
    git commit -m "Allure report - build $env:BUILD_NUMBER" 2>&1 | Out-Null
    git push origin gh-pages --force 2>&1 | Out-Null
    Write-Output "Published successfully - build $env:BUILD_NUMBER"
} else {
    Write-Output "No changes to publish"
}

Pop-Location
Remove-Item "gh-pages-deploy" -Recurse -Force -ErrorAction SilentlyContinue
Write-Output "Done"
'''
                        bat """set BUILD_NUMBER=${env.BUILD_NUMBER}
                            powershell -NoProfile -ExecutionPolicy Bypass -File publish-ghpages.ps1"""
                    }
                    echo "Allure report published → https://divinebayingana.github.io/api-test-automation-jenkins/"
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    post {

        // ── SUCCESS ──────────────────────────────────────────────────────────
        success {
            script {
                echo "========== SENDING SUCCESS NOTIFICATIONS =========="
                def ts       = getTestSummary()
                def passRate = ts.total > 0 ? (int)((ts.passed / ts.total) * 100) : 0
                def REPORT   = 'https://divinebayingana.github.io/api-test-automation-jenkins/'
                def ts_str   = new Date().format('dd MMM yyyy')

                // ── Slack (Block Kit) ─────────────────────────────────────────
                echo "[1/3] Sending Slack notification..."
                sendSlackBlocks('#2e7d32', [
                    [type: 'header', text: [type: 'plain_text', text: '✅ API Tests PASSED', emoji: true]],
                    [type: 'section', fields: [
                        [type: 'mrkdwn', text: "*Total:* ${ts.total}"],
                        [type: 'mrkdwn', text: "*Passed:* ${ts.passed}"],
                        [type: 'mrkdwn', text: "*Failed:* ${ts.failed}"],
                        [type: 'mrkdwn', text: "*Skipped:* ${ts.skipped}"],
                        [type: 'mrkdwn', text: "*Pass Rate:* ${passRate}%"]
                    ]],
                    [type: 'section', fields: [
                        [type: 'mrkdwn', text: "*Job:* ${env.JOB_NAME}"],
                        [type: 'mrkdwn', text: "*Build:* #${env.BUILD_NUMBER}"],
                        [type: 'mrkdwn', text: "*Branch:* ${env.GIT_BRANCH ?: 'N/A'}"],
                        [type: 'mrkdwn', text: "*Commit:* ${env.GIT_COMMIT?.take(7) ?: 'N/A'}"]
                    ]],
                    [type: 'divider'],
                    [type: 'actions', elements: [
                        [type: 'button', text: [type: 'plain_text', text: 'View Allure Report', emoji: true],
                         url: REPORT, style: 'primary'],
                        [type: 'button', text: [type: 'plain_text', text: 'Build Log', emoji: true],
                         url: "${env.BUILD_URL}"]
                    ]]
                ])

                // ── Email ─────────────────────────────────────────────────────
                echo "[2/3] Sending Email notification..."
                try {
                    withCredentials([
                        string(credentialsId: 'recipient-email', variable: 'RECIPIENT'),
                        string(credentialsId: 'gmail-address',   variable: 'SENDER')
                    ]) {
                        mail(
                            from    : SENDER,
                            to      : RECIPIENT,
                            subject : "✅ Build Passed — ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                            mimeType: 'text/html',
                            body    : """<!DOCTYPE html><html><body style="margin:0;padding:20px;background:#f4f6f9;font-family:Arial,sans-serif">
<div style="max-width:600px;margin:0 auto;background:#fff;border-radius:9px;overflow:hidden;border:1px solid #e8eaf0">
  <div style="background:#1a237e;padding:27px;color:#fff">
    <h1 style="margin:0;font-size:22px">API Test Build Passed</h1>
    <p style="margin:5px 0 0;opacity:.8;font-size:13px">${env.JOB_NAME} &middot; Build #${env.BUILD_NUMBER}</p>
  </div>
  <div style="background:#e8f5e9;padding:16px;border-left:5px solid #2e7d32">
    <p style="margin:0;font-size:17px;font-weight:700;color:#2e7d32">&#x2705; Build Passed on ${ts_str}</p>
  </div>
  <div style="padding:27px">
    <table width="100%" style="font-size:13px;border-collapse:collapse;margin-bottom:24px">
      <tr style="background:#f5f7ff">
        <td style="padding:9px;font-weight:700;border-bottom:1px solid #e8eaf0">Metric</td>
        <td style="padding:9px;font-weight:700;border-bottom:1px solid #e8eaf0">Value</td>
      </tr>
      <tr><td style="padding:9px;border-bottom:1px solid #f0f0f0">Job</td><td style="padding:9px;border-bottom:1px solid #f0f0f0">${env.JOB_NAME}</td></tr>
      <tr><td style="padding:9px;border-bottom:1px solid #f0f0f0">Branch</td><td style="padding:9px;border-bottom:1px solid #f0f0f0">${env.GIT_BRANCH ?: 'N/A'}</td></tr>
      <tr><td style="padding:9px;border-bottom:1px solid #f0f0f0">Duration</td><td style="padding:9px;border-bottom:1px solid #f0f0f0">${currentBuild.durationString}</td></tr>
      <tr><td style="padding:9px;border-bottom:1px solid #f0f0f0">Pass Rate</td><td style="padding:9px;border-bottom:1px solid #f0f0f0"><strong>${passRate}%</strong></td></tr>
      <tr><td style="padding:9px;border-bottom:1px solid #f0f0f0">Allure Report</td><td style="padding:9px;border-bottom:1px solid #f0f0f0"><a href="${REPORT}">View Hosted Report</a></td></tr>
    </table>
    <div style="display:flex;justify-content:space-around;text-align:center;margin-bottom:27px">
      <div style="flex:1;background:#e8f5e9;padding:15px;margin:4px;border-radius:7px">
        <div style="font-size:26px;font-weight:700;color:#2e7d32">${ts.passed}</div>
        <div style="font-size:11px;color:#555;margin-top:4px">PASSED</div>
      </div>
      <div style="flex:1;background:#e8f4fd;padding:15px;margin:4px;border-radius:7px">
        <div style="font-size:26px;font-weight:700;color:#1565c0">${ts.total}</div>
        <div style="font-size:11px;color:#555;margin-top:4px">TOTAL</div>
      </div>
      <div style="flex:1;background:#fff3e0;padding:15px;margin:4px;border-radius:7px">
        <div style="font-size:26px;font-weight:700;color:#e65100">${ts.skipped}</div>
        <div style="font-size:11px;color:#555;margin-top:4px">SKIPPED</div>
      </div>
    </div>
    <div style="text-align:center;margin-top:24px">
      <a href="${REPORT}" style="display:inline-block;background:#1a237e;color:#fff;text-decoration:none;padding:12px 28px;border-radius:5px;font-weight:700;margin-right:8px">View Allure Report</a>
      <a href="${env.BUILD_URL}" style="display:inline-block;background:#455a64;color:#fff;text-decoration:none;padding:12px 28px;border-radius:5px;font-weight:700">Build Log</a>
    </div>
  </div>
  <div style="padding:12px 27px;font-size:11px;color:#aaa;border-top:1px solid #eee">Automated notification &middot; ${env.JOB_NAME} &middot; ${env.JENKINS_URL}</div>
</div>
</body></html>"""
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

        // ── FAILURE ───────────────────────────────────────────────────────────
        failure {
            script {
                echo "========== SENDING FAILURE NOTIFICATIONS =========="
                def ts       = getTestSummary()
                def af       = getAllureFailures()
                def passRate = ts.total > 0 ? (int)((ts.passed / ts.total) * 100) : 0
                def REPORT   = 'https://divinebayingana.github.io/api-test-automation-jenkins/'
                def ts_str   = new Date().format('dd MMM yyyy')

                // Use allure failures if available, fall back to surefire data
                def failures = af.size() > 0 ? af : ts.failedTests.collect { t ->
                    [id: 'N/A', name: t.name, className: t.className, severity: 'Normal',
                     desc: t.impact, message: t.message, expected: 'N/A', actual: 'N/A']
                }

                // ── Slack (Block Kit) ─────────────────────────────────────────
                echo "[1/3] Sending Slack notification..."
                def failBlocks = []
                if (failures.size() > 0) {
                    failBlocks << [type: 'section', text: [type: 'mrkdwn', text: '*&#x1F6A8; Detailed Failure Report:*']]
                    failures.take(10).eachWithIndex { t, i ->
                        failBlocks << [type: 'section', text: [type: 'mrkdwn', text:
                            "*${i+1}. ${t.name}*\n" +
                            "*ID:* `${t.id}`  |  *Severity:* `${t.severity}`  |  *Class:* `${t.className}`\n" +
                            "*Description:* ${t.desc}\n" +
                            "*Failure:* ${t.message}\n" +
                            "*Expected:* `${t.expected}`    *Actual:* `${t.actual}`"
                        ]]
                        failBlocks << [type: 'divider']
                    }
                }
                sendSlackBlocks('#b71c1c', [
                    [type: 'header', text: [type: 'plain_text', text: '&#x1F534; API Tests FAILED', emoji: true]],
                    [type: 'section', fields: [
                        [type: 'mrkdwn', text: "*Total:* ${ts.total}"],
                        [type: 'mrkdwn', text: "*Passed:* ${ts.passed}"],
                        [type: 'mrkdwn', text: "*Failed:* ${ts.failed}"],
                        [type: 'mrkdwn', text: "*Skipped:* ${ts.skipped}"],
                        [type: 'mrkdwn', text: "*Pass Rate:* ${passRate}%"]
                    ]],
                    [type: 'section', fields: [
                        [type: 'mrkdwn', text: "*Job:* ${env.JOB_NAME}"],
                        [type: 'mrkdwn', text: "*Build:* #${env.BUILD_NUMBER}"],
                        [type: 'mrkdwn', text: "*Branch:* ${env.GIT_BRANCH ?: 'N/A'}"],
                        [type: 'mrkdwn', text: "*Commit:* ${env.GIT_COMMIT?.take(7) ?: 'N/A'}"]
                    ]],
                    [type: 'divider']
                ] + failBlocks + [
                    [type: 'actions', elements: [
                        [type: 'button', text: [type: 'plain_text', text: 'View Allure Report', emoji: true],
                         url: REPORT, style: 'primary'],
                        [type: 'button', text: [type: 'plain_text', text: 'Build Log', emoji: true],
                         url: "${env.BUILD_URL}"],
                        [type: 'button', text: [type: 'plain_text', text: 'Console Output', emoji: true],
                         url: "${env.BUILD_URL}console"]
                    ]]
                ])

                // ── Email ─────────────────────────────────────────────────────
                echo "[2/3] Sending Email notification..."
                try {
                    def failureCards = ''
                    if (failures.size() > 0) {
                        failures.each { t ->
                            failureCards += """
<div style="border:1px solid #ffcdd2;border-radius:7px;margin-bottom:16px;background:#fff;padding:15px;border-left:5px solid #b71c1c;">
  <p style="margin:0;font-size:14px;font-weight:700;color:#b71c1c">[${t.id}] ${t.name}</p>
  <p style="margin:3px 0;font-size:12px;color:#555"><b>Class:</b> ${t.className} &nbsp;|&nbsp; <b>Severity:</b> ${t.severity}</p>
  <p style="margin:5px 0 9px;font-size:12px;color:#777;font-style:italic">${t.desc}</p>
  <div style="background:#fdf2f2;padding:10px;border-radius:4px;margin-bottom:10px">
    <table style="font-size:12px;border-collapse:collapse;width:100%">
      <tr><td style="color:#555;padding:3px 8px 3px 0;width:70px"><b>Expected:</b></td><td style="color:#2e7d32;font-family:monospace">${t.expected}</td></tr>
      <tr><td style="color:#555;padding:3px 8px 3px 0"><b>Actual:</b></td><td style="color:#b71c1c;font-family:monospace">${t.actual}</td></tr>
    </table>
  </div>
  <div style="background:#f8f9fa;padding:10px;border-radius:4px;font-family:monospace;font-size:11px;color:#555;overflow-x:auto;border-left:3px solid #dee2e6">${t.message}</div>
</div>"""
                        }
                    } else {
                        failureCards = '<p style="color:#888;font-size:13px">No individual test data &mdash; build may have failed before tests ran.</p>'
                    }
                    withCredentials([
                        string(credentialsId: 'recipient-email', variable: 'RECIPIENT'),
                        string(credentialsId: 'gmail-address',   variable: 'SENDER')
                    ]) {
                        mail(
                            from    : SENDER,
                            to      : RECIPIENT,
                            subject : "&#x1F534; Build Failed — ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                            mimeType: 'text/html',
                            body    : """<!DOCTYPE html><html><body style="margin:0;padding:20px;background:#f4f6f9;font-family:Arial,sans-serif">
<div style="max-width:600px;margin:0 auto;background:#fff;border-radius:9px;overflow:hidden;border:1px solid #e8eaf0">
  <div style="background:#1a237e;padding:27px;color:#fff">
    <h1 style="margin:0;font-size:22px">API Test Build Failed</h1>
    <p style="margin:5px 0 0;opacity:.8;font-size:13px">${env.JOB_NAME} &middot; Build #${env.BUILD_NUMBER}</p>
  </div>
  <div style="background:#ffebee;padding:16px;border-left:5px solid #b71c1c">
    <p style="margin:0;font-size:17px;font-weight:700;color:#b71c1c">&#x274C; Build Failed on ${ts_str}</p>
  </div>
  <div style="padding:27px">
    <table width="100%" style="font-size:13px;border-collapse:collapse;margin-bottom:24px">
      <tr style="background:#f5f7ff">
        <td style="padding:9px;font-weight:700;border-bottom:1px solid #e8eaf0">Metric</td>
        <td style="padding:9px;font-weight:700;border-bottom:1px solid #e8eaf0">Value</td>
      </tr>
      <tr><td style="padding:9px;border-bottom:1px solid #f0f0f0">Job</td><td style="padding:9px;border-bottom:1px solid #f0f0f0">${env.JOB_NAME}</td></tr>
      <tr><td style="padding:9px;border-bottom:1px solid #f0f0f0">Branch</td><td style="padding:9px;border-bottom:1px solid #f0f0f0">${env.GIT_BRANCH ?: 'N/A'}</td></tr>
      <tr><td style="padding:9px;border-bottom:1px solid #f0f0f0">Duration</td><td style="padding:9px;border-bottom:1px solid #f0f0f0">${currentBuild.durationString}</td></tr>
      <tr><td style="padding:9px;border-bottom:1px solid #f0f0f0">Pass Rate</td><td style="padding:9px;border-bottom:1px solid #f0f0f0"><strong>${passRate}%</strong></td></tr>
      <tr><td style="padding:9px;border-bottom:1px solid #f0f0f0">Allure Report</td><td style="padding:9px;border-bottom:1px solid #f0f0f0"><a href="${REPORT}">View Hosted Report</a></td></tr>
    </table>
    <div style="display:flex;justify-content:space-around;text-align:center;margin-bottom:24px">
      <div style="flex:1;background:#e8f5e9;padding:15px;margin:4px;border-radius:7px">
        <div style="font-size:26px;font-weight:700;color:#2e7d32">${ts.passed}</div>
        <div style="font-size:11px;color:#555;margin-top:4px">PASSED</div>
      </div>
      <div style="flex:1;background:#ffebee;padding:15px;margin:4px;border-radius:7px">
        <div style="font-size:26px;font-weight:700;color:#b71c1c">${ts.failed}</div>
        <div style="font-size:11px;color:#555;margin-top:4px">FAILED</div>
      </div>
      <div style="flex:1;background:#e8f4fd;padding:15px;margin:4px;border-radius:7px">
        <div style="font-size:26px;font-weight:700;color:#1565c0">${ts.total}</div>
        <div style="font-size:11px;color:#555;margin-top:4px">TOTAL</div>
      </div>
    </div>
    <h2 style="font-size:16px;color:#b71c1c;margin:24px 0 12px">&#x1F6A8; Detailed Failure Report</h2>
    ${failureCards}
    <div style="text-align:center;margin-top:24px">
      <a href="${REPORT}" style="display:inline-block;background:#1a237e;color:#fff;text-decoration:none;padding:12px 28px;border-radius:5px;font-weight:700;margin-right:8px">View Allure Report</a>
      <a href="${env.BUILD_URL}console" style="display:inline-block;background:#455a64;color:#fff;text-decoration:none;padding:12px 28px;border-radius:5px;font-weight:700">Console Output</a>
    </div>
  </div>
  <div style="padding:12px 27px;font-size:11px;color:#aaa;border-top:1px solid #eee">Automated notification &middot; ${env.JOB_NAME} &middot; ${env.JENKINS_URL}</div>
</div>
</body></html>"""
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

        // ── UNSTABLE ──────────────────────────────────────────────────────────
        unstable {
            script {
                echo "========== SENDING BUILD REVIEW NOTIFICATIONS =========="
                def ts       = getTestSummary()
                def af       = getAllureFailures()
                def passRate = ts.total > 0 ? (int)((ts.passed / ts.total) * 100) : 0
                def REPORT   = 'https://divinebayingana.github.io/api-test-automation-jenkins/'
                def ts_str   = new Date().format('dd MMM yyyy')

                // Use allure failures if available, fall back to surefire data
                def failures = af.size() > 0 ? af : ts.failedTests.collect { t ->
                    [id: 'N/A', name: t.name, className: t.className, severity: 'Normal',
                     desc: t.impact, message: t.message, expected: 'N/A', actual: 'N/A']
                }

                // ── Slack (Block Kit) ─────────────────────────────────────────
                echo "[1/2] Sending Slack notification..."
                def failBlocks = []
                if (failures.size() > 0) {
                    failBlocks << [type: 'section', text: [type: 'mrkdwn', text: '*&#x1F6A8; Detailed Failure Report:*']]
                    failures.take(10).eachWithIndex { t, i ->
                        failBlocks << [type: 'section', text: [type: 'mrkdwn', text:
                            "*${i+1}. ${t.name}*\n" +
                            "*ID:* `${t.id}`  |  *Severity:* `${t.severity}`  |  *Class:* `${t.className}`\n" +
                            "*Description:* ${t.desc}\n" +
                            "*Failure:* ${t.message}\n" +
                            "*Expected:* `${t.expected}`    *Actual:* `${t.actual}`"
                        ]]
                        failBlocks << [type: 'divider']
                    }
                }
                sendSlackBlocks('#e65100', [
                    [type: 'header', text: [type: 'plain_text', text: '&#x26A0;&#xFE0F; API Tests — Review Required', emoji: true]],
                    [type: 'section', fields: [
                        [type: 'mrkdwn', text: "*Total:* ${ts.total}"],
                        [type: 'mrkdwn', text: "*Passed:* ${ts.passed}"],
                        [type: 'mrkdwn', text: "*Failed:* ${ts.failed}"],
                        [type: 'mrkdwn', text: "*Skipped:* ${ts.skipped}"],
                        [type: 'mrkdwn', text: "*Pass Rate:* ${passRate}%"]
                    ]],
                    [type: 'section', fields: [
                        [type: 'mrkdwn', text: "*Job:* ${env.JOB_NAME}"],
                        [type: 'mrkdwn', text: "*Build:* #${env.BUILD_NUMBER}"],
                        [type: 'mrkdwn', text: "*Branch:* ${env.GIT_BRANCH ?: 'N/A'}"],
                        [type: 'mrkdwn', text: "*Commit:* ${env.GIT_COMMIT?.take(7) ?: 'N/A'}"]
                    ]],
                    [type: 'divider']
                ] + failBlocks + [
                    [type: 'actions', elements: [
                        [type: 'button', text: [type: 'plain_text', text: 'View Allure Report', emoji: true],
                         url: REPORT, style: 'primary'],
                        [type: 'button', text: [type: 'plain_text', text: 'Test Results', emoji: true],
                         url: "${env.BUILD_URL}testReport"],
                        [type: 'button', text: [type: 'plain_text', text: 'Console Output', emoji: true],
                         url: "${env.BUILD_URL}console"]
                    ]]
                ])

                // ── Email ─────────────────────────────────────────────────────
                echo "[2/2] Sending Email notification..."
                try {
                    def failureCards = ''
                    if (failures.size() > 0) {
                        failures.each { t ->
                            failureCards += """
<div style="border:1px solid #ffcdd2;border-radius:7px;margin-bottom:16px;background:#fff;padding:15px;border-left:5px solid #b71c1c;">
  <p style="margin:0;font-size:14px;font-weight:700;color:#b71c1c">[${t.id}] ${t.name}</p>
  <p style="margin:3px 0;font-size:12px;color:#555"><b>Class:</b> ${t.className} &nbsp;|&nbsp; <b>Severity:</b> ${t.severity}</p>
  <p style="margin:5px 0 9px;font-size:12px;color:#777;font-style:italic">${t.desc}</p>
  <div style="background:#fdf2f2;padding:10px;border-radius:4px;margin-bottom:10px">
    <table style="font-size:12px;border-collapse:collapse;width:100%">
      <tr><td style="color:#555;padding:3px 8px 3px 0;width:70px"><b>Expected:</b></td><td style="color:#2e7d32;font-family:monospace">${t.expected}</td></tr>
      <tr><td style="color:#555;padding:3px 8px 3px 0"><b>Actual:</b></td><td style="color:#b71c1c;font-family:monospace">${t.actual}</td></tr>
    </table>
  </div>
  <div style="background:#f8f9fa;padding:10px;border-radius:4px;font-family:monospace;font-size:11px;color:#555;overflow-x:auto;border-left:3px solid #dee2e6">${t.message}</div>
</div>"""
                        }
                    } else {
                        failureCards = '<p style="color:#888;font-size:13px">No individual test data available &mdash; check the Allure Report.</p>'
                    }
                    withCredentials([
                        string(credentialsId: 'recipient-email', variable: 'RECIPIENT'),
                        string(credentialsId: 'gmail-address',   variable: 'SENDER')
                    ]) {
                        mail(
                            from    : SENDER,
                            to      : RECIPIENT,
                            subject : "&#x26A0;&#xFE0F; Build Review Required — ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                            mimeType: 'text/html',
                            body    : """<!DOCTYPE html><html><body style="margin:0;padding:20px;background:#f4f6f9;font-family:Arial,sans-serif">
<div style="max-width:600px;margin:0 auto;background:#fff;border-radius:9px;overflow:hidden;border:1px solid #e8eaf0">
  <div style="background:#1a237e;padding:27px;color:#fff">
    <h1 style="margin:0;font-size:22px">API Test Build &mdash; Review Required</h1>
    <p style="margin:5px 0 0;opacity:.8;font-size:13px">${env.JOB_NAME} &middot; Build #${env.BUILD_NUMBER}</p>
  </div>
  <div style="background:#fff3e0;padding:16px;border-left:5px solid #e65100">
    <p style="margin:0;font-size:17px;font-weight:700;color:#e65100">&#x26A0;&#xFE0F; Tests require attention &mdash; ${ts_str}</p>
  </div>
  <div style="padding:27px">
    <table width="100%" style="font-size:13px;border-collapse:collapse;margin-bottom:24px">
      <tr style="background:#f5f7ff">
        <td style="padding:9px;font-weight:700;border-bottom:1px solid #e8eaf0">Metric</td>
        <td style="padding:9px;font-weight:700;border-bottom:1px solid #e8eaf0">Value</td>
      </tr>
      <tr><td style="padding:9px;border-bottom:1px solid #f0f0f0">Job</td><td style="padding:9px;border-bottom:1px solid #f0f0f0">${env.JOB_NAME}</td></tr>
      <tr><td style="padding:9px;border-bottom:1px solid #f0f0f0">Branch</td><td style="padding:9px;border-bottom:1px solid #f0f0f0">${env.GIT_BRANCH ?: 'N/A'}</td></tr>
      <tr><td style="padding:9px;border-bottom:1px solid #f0f0f0">Duration</td><td style="padding:9px;border-bottom:1px solid #f0f0f0">${currentBuild.durationString}</td></tr>
      <tr><td style="padding:9px;border-bottom:1px solid #f0f0f0">Pass Rate</td><td style="padding:9px;border-bottom:1px solid #f0f0f0"><strong>${passRate}%</strong></td></tr>
      <tr><td style="padding:9px;border-bottom:1px solid #f0f0f0">Allure Report</td><td style="padding:9px;border-bottom:1px solid #f0f0f0"><a href="${REPORT}">View Hosted Report</a></td></tr>
    </table>
    <div style="display:flex;justify-content:space-around;text-align:center;margin-bottom:24px">
      <div style="flex:1;background:#e8f5e9;padding:15px;margin:4px;border-radius:7px">
        <div style="font-size:26px;font-weight:700;color:#2e7d32">${ts.passed}</div>
        <div style="font-size:11px;color:#555;margin-top:4px">PASSED</div>
      </div>
      <div style="flex:1;background:#ffebee;padding:15px;margin:4px;border-radius:7px">
        <div style="font-size:26px;font-weight:700;color:#b71c1c">${ts.failed}</div>
        <div style="font-size:11px;color:#555;margin-top:4px">TO REVIEW</div>
      </div>
      <div style="flex:1;background:#e8f4fd;padding:15px;margin:4px;border-radius:7px">
        <div style="font-size:26px;font-weight:700;color:#1565c0">${ts.total}</div>
        <div style="font-size:11px;color:#555;margin-top:4px">TOTAL</div>
      </div>
    </div>
    <h2 style="font-size:16px;color:#b71c1c;margin:24px 0 12px">&#x1F6A8; Detailed Failure Report</h2>
    ${failureCards}
    <div style="text-align:center;margin-top:24px">
      <a href="${REPORT}" style="display:inline-block;background:#1a237e;color:#fff;text-decoration:none;padding:12px 28px;border-radius:5px;font-weight:700;margin-right:8px">View Allure Report</a>
      <a href="${env.BUILD_URL}testReport" style="display:inline-block;background:#455a64;color:#fff;text-decoration:none;padding:12px 28px;border-radius:5px;font-weight:700">Test Results</a>
    </div>
  </div>
  <div style="padding:12px 27px;font-size:11px;color:#aaa;border-top:1px solid #eee">Automated notification &middot; ${env.JOB_NAME} &middot; ${env.JENKINS_URL}</div>
</div>
</body></html>"""
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
            cleanWs(deleteDirs: true, patterns: [[pattern: 'parse-tests.ps1, read-allure.ps1, test-summary.txt, allure-failures.txt, slack-notify.json, publish-ghpages.ps1, gh-pages-deploy', type: 'INCLUDE']])
            echo "========== PIPELINE COMPLETE =========="
        }
    }
}
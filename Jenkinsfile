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

                    if (fileExists('target/site/allure-report/index.html')) {
                        echo "Allure report found and publishing..."
                        publishHTML([
                            allowMissing: false,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'target/site/allure-report',
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
        always {
            script {
                // ── 1. Simple status detection ──────────────────────────────────
                def buildResult = currentBuild.currentResult ?: 'UNKNOWN'

                // ── 2. Build status context ────────────────────────────────────
                def isSuccess   = buildResult == 'SUCCESS'
                def isFailure   = buildResult == 'FAILURE'
                def slackEmoji  = isSuccess ? ':white_check_mark:' : (isFailure ? ':x:' : ':warning:')
                def emailIcon   = isSuccess ? '✅' : (isFailure ? '❌' : '⚠️')
                def slackColor  = isSuccess ? '#2e7d32' : (isFailure ? '#e01e5a' : '#ff8800')
                def statusColor = isSuccess ? '#2e7d32' : (isFailure ? '#b71c1c' : '#e65100')
                def statusBg    = isSuccess ? '#e8f5e9' : (isFailure ? '#ffebee' : '#fff3e0')

                // ── 3. Slack notification (rich blocks) ────────────────────────
                echo "Sending Slack notification..."
                try {
                    withCredentials([string(credentialsId: 'incoming-webhook', variable: 'SLACK_WEBHOOK')]) {
                        def blocks = [
                            [
                                type: 'header',
                                text: [type: 'plain_text', text: "${slackEmoji} API Tests — ${buildResult}", emoji: true]
                            ],
                            [
                                type  : 'section',
                                fields: [
                                    [type: 'mrkdwn', text: "*Job:*\n${env.JOB_NAME}"],
                                    [type: 'mrkdwn', text: "*Build:*\n#${env.BUILD_NUMBER}"],
                                    [type: 'mrkdwn', text: "*Status:*\n${buildResult}"],
                                    [type: 'mrkdwn', text: "*Duration:*\n${currentBuild.durationString}"]
                                ]
                            ],
                            [type: 'divider'],
                            [
                                type    : 'actions',
                                elements: [
                                    [type: 'button', text: [type: 'plain_text', text: 'View Build',    emoji: true], url: "${env.BUILD_URL}",              style: 'primary'],
                                    [type: 'button', text: [type: 'plain_text', text: 'Allure Report', emoji: true], url: "${env.BUILD_URL}Allure%20Report/"]
                                ]
                            ]
                        ]

                        def payload = groovy.json.JsonOutput.toJson([attachments: [[color: slackColor, blocks: blocks]]])
                        writeFile file: 'slack-payload.json', text: payload
                        bat 'powershell -Command "Invoke-RestMethod -Uri $env:SLACK_WEBHOOK -Method Post -ContentType \'application/json\' -Body (Get-Content slack-payload.json -Raw)"'
                    }
                    echo "✅ Slack notification sent"
                } catch (Exception ex) {
                    echo "❌ Slack notification failed: ${ex.message}"
                }

                // ── 4. HTML email notification ─────────────────────────────────
                echo "Sending email notification..."
                try {
                    withCredentials([
                        string(credentialsId: 'gmail-address',      variable: 'GMAIL_USER'),
                        string(credentialsId: 'gmail-app-password', variable: 'GMAIL_PASS'),
                        string(credentialsId: 'recipient-email',    variable: 'REPORT_TO')
                    ]) {
                        def htmlBody = """<!DOCTYPE html>
<html><body style="margin:0;padding:20px;background:#f4f6f9;font-family:Arial,sans-serif">
<div style="max-width:600px;margin:0 auto;background:#fff;border-radius:8px;overflow:hidden;border:1px solid #e0e0e0">

  <div style="background:#1a237e;padding:24px;color:#fff">
    <h1 style="margin:0;font-size:22px">API Test Build ${buildResult}</h1>
  </div>

  <div style="background:${statusBg};padding:16px;border-left:5px solid ${statusColor}">
    <p style="margin:0;font-size:18px;font-weight:700;color:${statusColor}">${emailIcon} ${buildResult}</p>
  </div>

  <div style="padding:24px">
    <table width="100%" style="font-size:13px;border-collapse:collapse;margin-bottom:20px">
      <tr style="background:#f5f7ff">
        <td style="padding:9px 10px;font-weight:700;border-bottom:1px solid #e8eaf0">Metric</td>
        <td style="padding:9px 10px;font-weight:700;border-bottom:1px solid #e8eaf0">Value</td>
      </tr>
      <tr><td style="padding:9px 10px;border-bottom:1px solid #f0f0f0">Job</td><td style="padding:9px 10px;border-bottom:1px solid #f0f0f0">${env.JOB_NAME} #${env.BUILD_NUMBER}</td></tr>
      <tr><td style="padding:9px 10px;border-bottom:1px solid #f0f0f0">Status</td><td style="padding:9px 10px;border-bottom:1px solid #f0f0f0">${buildResult}</td></tr>
      <tr><td style="padding:9px 10px;border-bottom:1px solid #f0f0f0">Duration</td><td style="padding:9px 10px;border-bottom:1px solid #f0f0f0">${currentBuild.durationString}</td></tr>
    </table>

    <div style="text-align:center;margin-top:28px">
      <a href="${env.BUILD_URL}" style="display:inline-block;background:#1a237e;color:#fff;text-decoration:none;padding:12px 24px;border-radius:5px;font-weight:700;margin-right:8px">View Build</a>
      <a href="${env.BUILD_URL}Allure%20Report/" style="display:inline-block;background:#37474f;color:#fff;text-decoration:none;padding:12px 24px;border-radius:5px;font-weight:700">Allure Report</a>
    </div>
  </div>

</div></body></html>"""

                        writeFile file: 'email-body.html', text: htmlBody

                        def emailSubject = "${buildResult}: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
                        def psScript = """\
\$smtp = New-Object Net.Mail.SmtpClient('smtp.gmail.com', 587)
\$smtp.EnableSsl = \$true
\$smtp.Credentials = New-Object Net.NetworkCredential(\$env:GMAIL_USER, \$env:GMAIL_PASS)
\$msg = New-Object Net.Mail.MailMessage(\$env:GMAIL_USER, \$env:REPORT_TO)
\$msg.Subject = "${emailSubject}"
\$msg.IsBodyHtml = \$true
\$msg.Body = [IO.File]::ReadAllText((Join-Path (Get-Location) 'email-body.html'), [Text.Encoding]::UTF8)
\$smtp.Send(\$msg)
"""
                        writeFile file: 'send-email.ps1', text: psScript
                        bat 'powershell -ExecutionPolicy Bypass -File send-email.ps1'
                    }
                    echo "✅ Email notification sent"
                } catch (Exception ex) {
                    echo "❌ Email notification failed: ${ex.message}"
                }
            }

            archiveArtifacts artifacts: 'target/site/allure-report/**,target/surefire-reports/**', allowEmptyArchive: true
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
        }
    }
}

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
                    echo "✅ Slack notification sent"
                } catch (Exception ex) {
                    echo "❌ Slack notification failed: ${ex.message}"
                }

                // ── 5. HTML email notification ─────────────────────────────────
                echo "Sending email notification..."
                try {
                    withCredentials([
                        string(credentialsId: 'gmail-address',      variable: 'GMAIL_USER'),
                        string(credentialsId: 'gmail-app-password', variable: 'GMAIL_PASS'),
                        string(credentialsId: 'recipient-email',    variable: 'REPORT_TO')
                    ]) {
                        def failBlock = ''
                        if (failingTests) {
                            failBlock = '<h2 style="margin:24px 0 12px;font-size:16px;color:#b71c1c">&#x1F6A8; Failing Tests</h2>'
                            failingTests.each { t ->
                                failBlock += """
<div style="border:1px solid #ffcdd2;border-radius:6px;margin-bottom:14px;padding:14px;border-left:5px solid #b71c1c">
  <p style="margin:0;font-size:13px;font-weight:700;color:#b71c1c">[${esc(t.id)}] ${esc(t.name)}</p>
  <p style="margin:3px 0;font-size:12px;color:#555"><b>Severity:</b> ${esc(t.severity)}</p>
  <p style="margin:3px 0;font-size:11px;color:#666;font-style:italic">${esc(t.description)}</p>
  <div style="background:#fdf2f2;padding:10px;border-radius:4px;margin-top:8px">
    <table style="font-size:12px;border-collapse:collapse;width:100%">
      <tr>
        <td style="color:#555;padding:2px 8px 2px 0;width:80px"><b>Expected:</b></td>
        <td style="color:#2e7d32;font-family:monospace">${esc(t.expected)}</td>
      </tr>
      <tr>
        <td style="color:#555;padding:2px 8px 2px 0"><b>Actual:</b></td>
        <td style="color:#b71c1c;font-family:monospace">${esc(t.actual)}</td>
      </tr>
    </table>
  </div>
  <div style="background:#f8f9fa;padding:10px;border-radius:4px;margin-top:8px;font-family:monospace;font-size:11px;color:#444;word-break:break-word">${esc(t.message)}</div>
</div>"""
                            }
                        }

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
      <tr><td style="padding:9px 10px;border-bottom:1px solid #f0f0f0">Duration</td><td style="padding:9px 10px;border-bottom:1px solid #f0f0f0">${currentBuild.durationString}</td></tr>
      <tr><td style="padding:9px 10px;border-bottom:1px solid #f0f0f0">Pass Rate</td><td style="padding:9px 10px;border-bottom:1px solid #f0f0f0"><strong>${passPct}%</strong></td></tr>
    </table>

    <table width="100%" style="text-align:center;margin-bottom:20px;border-collapse:collapse">
      <tr>
        <td style="width:25%;padding:4px">
          <div style="background:#e8f5e9;padding:14px;border-radius:6px">
            <div style="font-size:22px;font-weight:700;color:#2e7d32">${passedCount}</div>
            <div style="font-size:11px;color:#555;margin-top:2px">PASSED</div>
          </div>
        </td>
        <td style="width:25%;padding:4px">
          <div style="background:#ffebee;padding:14px;border-radius:6px">
            <div style="font-size:22px;font-weight:700;color:#b71c1c">${failedCount}</div>
            <div style="font-size:11px;color:#555;margin-top:2px">FAILED</div>
          </div>
        </td>
        <td style="width:25%;padding:4px">
          <div style="background:#f5f5f5;padding:14px;border-radius:6px">
            <div style="font-size:22px;font-weight:700;color:#757575">${skippedCount}</div>
            <div style="font-size:11px;color:#555;margin-top:2px">SKIPPED</div>
          </div>
        </td>
        <td style="width:25%;padding:4px">
          <div style="background:#e3f2fd;padding:14px;border-radius:6px">
            <div style="font-size:22px;font-weight:700;color:#1565c0">${total}</div>
            <div style="font-size:11px;color:#555;margin-top:2px">TOTAL</div>
          </div>
        </td>
      </tr>
    </table>

    ${failBlock}

    <div style="text-align:center;margin-top:28px">
      <a href="${env.BUILD_URL}" style="display:inline-block;background:#1a237e;color:#fff;text-decoration:none;padding:12px 24px;border-radius:5px;font-weight:700;margin-right:8px">View Build</a>
      <a href="${env.BUILD_URL}allure/" style="display:inline-block;background:#37474f;color:#fff;text-decoration:none;padding:12px 24px;border-radius:5px;font-weight:700">Allure Report</a>
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

            archiveArtifacts artifacts: 'target/site/allure-maven-plugin/**,target/surefire-reports/**', allowEmptyArchive: true
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
        }
    }
}

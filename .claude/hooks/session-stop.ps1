$ErrorActionPreference = 'Continue'

$projectDir = $env:CLAUDE_PROJECT_DIR
if (-not $projectDir) { $projectDir = (Get-Location).Path }

Push-Location $projectDir
try {
    $diffStat = (git diff --stat HEAD 2>&1 | Out-String).TrimEnd()
} finally {
    Pop-Location
}

if (-not $diffStat) { $diffStat = "(no changes vs HEAD)" }

$reminder = @"
Reminder: update .claude/SESSION_LOG.md before closing this session:
  - What shipped
  - Any blockers
  - Updated TODO

git diff --stat HEAD:
$diffStat
"@

$out = @{ systemMessage = $reminder } | ConvertTo-Json -Compress
Write-Output $out
exit 0

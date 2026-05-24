$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$projectDir = $env:CLAUDE_PROJECT_DIR
if (-not $projectDir) { $projectDir = (Get-Location).Path }
$logPath = Join-Path $projectDir '.claude/SESSION_LOG.md'

if (-not (Test-Path $logPath)) {
    Write-Output "SESSION_LOG.md not present yet."
    exit 0
}

$lines = Get-Content $logPath -Encoding UTF8

# --- Most recent session: first '### ' heading after '## Sessions' ---
$sessionsIdx = -1
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match '^##\s+Sessions\s*$') { $sessionsIdx = $i; break }
}

$sessionDate = $null
$shipped = @()
if ($sessionsIdx -ge 0) {
    $headingIdx = -1
    for ($i = $sessionsIdx + 1; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match '^###\s+(.+?)\s*$') {
            $sessionDate = $matches[1]
            $headingIdx = $i
            break
        }
    }
    if ($headingIdx -ge 0) {
        for ($j = $headingIdx + 1; $j -lt $lines.Count; $j++) {
            if ($lines[$j] -match '^###\s') { break }
            if ($lines[$j] -match '^\*\*Shipped:\*\*') {
                for ($k = $j + 1; $k -lt $lines.Count; $k++) {
                    if ($lines[$k] -match '^-\s+(.+)$') {
                        $shipped += $matches[1]
                        if ($shipped.Count -ge 3) { break }
                    } elseif ($lines[$k] -match '^\*\*' -or $lines[$k] -match '^###\s') {
                        break
                    } elseif ($lines[$k].Trim() -eq '' -and $shipped.Count -gt 0) {
                        break
                    }
                }
                break
            }
        }
    }
}

# --- TODO: first 5 '- [ ]' items under '## Current TODO' ---
$todoIdx = -1
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match '^##\s+Current TODO\s*$') { $todoIdx = $i; break }
}
$todos = @()
if ($todoIdx -ge 0) {
    for ($i = $todoIdx + 1; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match '^##\s' -or $lines[$i] -match '^---\s*$') { break }
        if ($lines[$i] -match '^-\s+\[\s\]\s+(.+)$') {
            $todos += $matches[1]
            if ($todos.Count -ge 5) { break }
        }
    }
}

# --- Print ---
if ($sessionDate) {
    Write-Output "Last session $sessionDate"
    foreach ($s in $shipped) { Write-Output "  - $s" }
} else {
    Write-Output "Last session: (no session entries found)"
}
if ($todos.Count -gt 0) {
    Write-Output "Current TODO (top $($todos.Count)):"
    foreach ($t in $todos) { Write-Output "  - $t" }
} else {
    Write-Output "Current TODO: (no '- [ ]' items found in 'Current TODO' section)"
}
exit 0

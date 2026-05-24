$ErrorActionPreference = 'Continue'

$raw = [Console]::In.ReadToEnd()
if (-not $raw) { exit 0 }

$payload = $raw | ConvertFrom-Json
$filePath = [string]$payload.tool_input.file_path
if (-not $filePath) { exit 0 }

$leaf = Split-Path $filePath -Leaf
$isJava = $filePath -match '\.java$'
$isPom  = $leaf -eq 'pom.xml'
if (-not ($isJava -or $isPom)) { exit 0 }

$modules = @(
    'trade-ingestion-service',
    'activity-monitor-service',
    'alert-service',
    'case-management-service',
    'surveillance-events-lib'
)
$normalized = $filePath -replace '\\','/'
$module = $null
foreach ($m in $modules) {
    if ($normalized -match "(^|/)$m/") { $module = $m; break }
}
if (-not $module) { exit 0 }

$projectDir = $env:CLAUDE_PROJECT_DIR
if (-not $projectDir) { $projectDir = (Get-Location).Path }

Push-Location $projectDir
try {
    $mvnOut = & mvn -pl $module compile -B 2>&1
    $code = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($code -ne 0) {
    [Console]::Error.WriteLine("mvn compile FAILED for module: $module (exit $code)")
    [Console]::Error.WriteLine("---")
    $mvnOut | ForEach-Object { [Console]::Error.WriteLine($_) }
    exit 2
}
exit 0

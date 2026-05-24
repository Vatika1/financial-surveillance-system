$ErrorActionPreference = 'Stop'

$raw = [Console]::In.ReadToEnd()
if (-not $raw) { exit 0 }

$payload = $raw | ConvertFrom-Json

$parts = @()
if ($payload.tool_input.content)    { $parts += [string]$payload.tool_input.content }
if ($payload.tool_input.new_string) { $parts += [string]$payload.tool_input.new_string }
$body = $parts -join "`n"

if ($body -match ':latest\b') {
    [Console]::Error.WriteLine("BLOCKED: ':latest' Docker tag detected in proposed write.")
    [Console]::Error.WriteLine('CI tags images by short git SHA (${GITHUB_SHA::7}). Pin to a specific SHA instead of :latest.')
    [Console]::Error.WriteLine("File: $($payload.tool_input.file_path)")
    exit 2
}
exit 0

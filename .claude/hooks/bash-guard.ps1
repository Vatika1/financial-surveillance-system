$ErrorActionPreference = 'Stop'

$raw = [Console]::In.ReadToEnd()
if (-not $raw) { exit 0 }

$payload = $raw | ConvertFrom-Json
$cmd = [string]$payload.tool_input.command
if (-not $cmd) { exit 0 }

# Exception: command invokes a .ps1 or .sh script directly (user's own teardown script).
# Matches: "scripts/stop-dev.ps1", ".\scripts\foo.ps1", "bash teardown.sh", "& ./x.sh", etc.
if ($cmd -match '(^|\s|&\s*|\.[\\/])[\w\-./\\]+\.(ps1|sh)(\s|$)') { exit 0 }

$patterns = @(
    @{ name = 'terraform destroy'; regex = 'terraform\s+destroy\b' },
    @{ name = 'kubectl delete';    regex = 'kubectl\s+delete\b' },
    @{ name = 'aws rds delete';    regex = 'aws\s+rds\s+delete' },
    @{ name = 'aws kafka delete';  regex = 'aws\s+kafka\s+delete' },
    @{ name = 'aws ecr delete';    regex = 'aws\s+ecr\s+delete' },
    @{ name = 'git push --force';  regex = 'git\s+push\s+(--force\b|-f\b)' },
    @{ name = 'git reset --hard';  regex = 'git\s+reset\s+--hard\b' },
    @{ name = 'rm -rf';            regex = 'rm\s+-rf?\b' }
)

$matched = $null
foreach ($p in $patterns) {
    if ($cmd -match $p.regex) { $matched = $p.name; break }
}
if (-not $matched) { exit 0 }

$reason = @"
DANGEROUS COMMAND -- confirmation required.

Command:
  $cmd

Matched pattern: $matched

Approve only if you intend this destructive action. To bypass guard in bulk, invoke it from a .ps1/.sh script.
"@

$out = @{
    hookSpecificOutput = @{
        hookEventName            = 'PreToolUse'
        permissionDecision       = 'ask'
        permissionDecisionReason = $reason
    }
} | ConvertTo-Json -Depth 5 -Compress

Write-Output $out
exit 0

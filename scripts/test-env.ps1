# Configuration-only checks: no MySQL connection, HTTP listener or real Java process.
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'env-common.ps1')
$fixture = Join-Path $root ('tmp/env-check-' + [guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path "$fixture/scripts", "$fixture/.local", "$fixture/air-ticket-server/target" -Force
Copy-Item "$root/.env.example" "$fixture/.env.example"
foreach ($name in @('env-common.ps1', 'db-common.ps1', 'setup-env.ps1', 'run-server.ps1')) {
 Copy-Item (Join-Path $PSScriptRoot $name) "$fixture/scripts/$name"
}
Copy-Item "$root/start_backend.ps1" "$fixture/start_backend.ps1"
Set-Content "$fixture/air-ticket-server/target/air-ticket-server-1.0.0.jar" ''

# New setup generates a valid secret and never overwrites an existing file.
& "$fixture/scripts/setup-env.ps1"
$created = [IO.File]::ReadAllText("$fixture/.env")
$jwt = [regex]::Match($created, '(?m)^JWT_SECRET=(.+)$').Groups[1].Value.Trim()
if ([Convert]::FromBase64String($jwt).Length -ne 32) { throw 'JWT generation failed' }
& "$fixture/scripts/setup-env.ps1"
if ([IO.File]::ReadAllText("$fixture/.env") -cne $created) { throw 'Existing .env was overwritten' }
Remove-Item -LiteralPath "$fixture/.env"

# Migrate literal credentials without writing secrets to the console.
@'
local:
  db-username: 'fixture_member'
  db-password: 'it''s # a: $literal \ password'
  jwt-secret: 'Zml4dHVyZS1qd3Qtc2VjcmV0LW9ubHktbm90LXJlYWw='
  ai-enabled: false
  deepseek-api-key: 'fixture-key'
'@ | Set-Content "$fixture/.local/application-local.yml"
& "$fixture/scripts/setup-env.ps1" -FromLocalConfig
$migrated = [IO.File]::ReadAllText("$fixture/.env")
if ($migrated -notmatch "(?m)^DB_USERNAME='fixture_member'" -or $migrated -match 'replace_with_local_password') {
 throw 'Local YAML migration failed'
}

# Child harness exercises the real startup wrapper; java.exe is a test function.
@'
param([string]$Fixture, [string]$Mode)
$ErrorActionPreference = 'Stop'
foreach ($name in @('JAVA_HOME','MAVEN_HOME','DB_USERNAME','DB_PASSWORD','DB_NAME','JWT_SECRET','AI_ENABLED','DEEPSEEK_API_KEY','SERVER_PORT','ALLOWED_ORIGINS')) {
 Remove-Item -LiteralPath "Env:$name" -ErrorAction SilentlyContinue
}
function global:java.exe {
 if ($env:DB_USERNAME -ne 'fixture_member') { throw 'Username not loaded' }
 if ($env:DB_PASSWORD -cne 'it''s # a: $literal \ password') { throw 'Password changed while loading' }
 if ($Mode -eq 'file') {
  if ('--server.port=18080' -notin $args -or $env:DB_NAME -ne 'air_ticket') { throw 'File settings not used' }
 } elseif ($Mode -eq 'environment') {
  if ('--server.port=19080' -notin $args -or $env:DB_NAME -ne 'air_ticket_env') { throw 'Environment precedence failed' }
 } else {
  if ('--server.port=20080' -notin $args -or $env:DB_NAME -ne 'air_ticket_cli') { throw 'CLI precedence failed' }
 }
 $global:LASTEXITCODE = 0
}
if ($Mode -ne 'file') { $env:SERVER_PORT='19080'; $env:DB_NAME='air_ticket_env' }
if ($Mode -eq 'cli') { & "$Fixture/start_backend.ps1" -Port 20080 -Database air_ticket_cli }
else { & "$Fixture/start_backend.ps1" }
'@ | Set-Content "$fixture/harness.ps1"
foreach ($mode in @('file', 'environment', 'cli')) {
 & (Get-Process -Id $PID).Path -NoProfile -File "$fixture/harness.ps1" -Fixture $fixture -Mode $mode
 if ($LASTEXITCODE) { throw "Startup configuration check failed: $mode" }
}

# Reject malformed dotenv files before applying any entry.
@'
PLANE_ENV_PARTIAL=should_not_load
PLANE_ENV_BROKEN='unclosed
'@ | Set-Content "$fixture/invalid.env"
$rejected = $false
try { Import-ProjectEnvironment -Path "$fixture/invalid.env" } catch { $rejected = $true }
if (-not $rejected -or $env:PLANE_ENV_PARTIAL) { throw 'Invalid dotenv file was partially applied' }
Write-Host 'PASS: JWT generation, setup idempotence, YAML migration, literal passwords, startup file/environment/CLI precedence, invalid-file rejection.'

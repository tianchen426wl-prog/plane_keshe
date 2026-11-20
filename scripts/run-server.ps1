param([int]$Port=8080,[string]$Database='air_ticket',[string]$AllowedOrigins='http://localhost:8081,http://localhost:8082,http://127.0.0.1:8081,http://127.0.0.1:8082')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'env-common.ps1')
. (Join-Path $PSScriptRoot 'backend-artifact.ps1')
Import-ProjectEnvironment
Set-ProjectToolPaths
if($env:DB_PASSWORD -eq 'replace_with_local_password'){throw 'Edit DB_USERNAME and DB_PASSWORD in the project root .env before starting.'}
if($env:JWT_SECRET -eq 'replace_with_a_random_base64_secret_at_least_32_bytes'){throw 'Generate a random JWT_SECRET in .env, or use scripts/setup-env.ps1.'}
if(-not $PSBoundParameters.ContainsKey('Port') -and $env:SERVER_PORT){$Port=[int]$env:SERVER_PORT}
if(-not $PSBoundParameters.ContainsKey('Database') -and $env:DB_NAME){$Database=$env:DB_NAME}
if(-not $PSBoundParameters.ContainsKey('AllowedOrigins') -and $env:ALLOWED_ORIGINS){$AllowedOrigins=$env:ALLOWED_ORIGINS}
$env:DB_NAME=$Database
$java=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/java.exe'}else{'java.exe'}
Push-Location (Join-Path $root 'air-ticket-server')
try {
 $jar=Get-ProjectBackendArtifact -ProjectRoot $root
 Write-Host "Backend package: $jar"
 & $java -jar $jar "--server.port=$Port" "--app.allowed-origins=$AllowedOrigins"
 exit $LASTEXITCODE
}finally{Pop-Location}

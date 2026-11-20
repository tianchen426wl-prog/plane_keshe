$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'db-common.ps1')
Set-ProjectToolPaths
if(-not $env:DB_USERNAME){
 $env:DB_USERNAME=Get-LocalDbValue 'db-username'
}
if(-not $env:DB_PASSWORD){
 $env:DB_PASSWORD=Get-LocalDbValue 'db-password'
}
$maven=Get-Command mvn.cmd -ErrorAction SilentlyContinue
if(-not $maven){throw 'Install Maven 3.8+ and add mvn.cmd to PATH'}
Push-Location (Join-Path $root 'air-ticket-server')
try { & $maven.Source verify; exit $LASTEXITCODE } finally{Pop-Location}

# Dot-source this script to configure the current terminal: . ./scripts/activate.ps1
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'env-common.ps1')
Import-ProjectEnvironment
Set-ProjectToolPaths
if (-not (Get-Command java.exe -ErrorAction SilentlyContinue)) {
 throw 'Install JDK 17 and set JAVA_HOME in .env, or add java.exe to PATH.'
}
Write-Host 'Project environment loaded for this terminal (JDK/Maven paths and .env).'

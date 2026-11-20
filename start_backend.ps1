param([int]$Port, [string]$Database, [string]$AllowedOrigins)
$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'scripts/run-server.ps1') @PSBoundParameters

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/env-common.ps1')
Import-ProjectEnvironment
if (-not (Get-Command node -ErrorAction SilentlyContinue)) { throw 'Install Node.js and add node to PATH.' }
& node (Join-Path $PSScriptRoot 'scripts/serve-built.cjs')
exit $LASTEXITCODE

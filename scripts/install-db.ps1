param([string]$Database='air_ticket_install_test',[switch]$DemoData)
$ErrorActionPreference='Stop'
if($DemoData){Write-Warning 'Legacy dated flight samples are no longer imported. Normal backend startup generates flights from current schedules.'}
& (Join-Path $PSScriptRoot 'database-setup.ps1') -Database $Database -RequireEmpty

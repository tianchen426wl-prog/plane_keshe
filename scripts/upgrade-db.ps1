param([string]$Database='air_ticket',[switch]$RestoreTest)
$ErrorActionPreference='Stop'
if($RestoreTest){throw 'Private legacy test restoration is retired. Use a separate backup/restore workflow; this command only performs non-destructive migrations.'}
& (Join-Path $PSScriptRoot 'database-setup.ps1') -Database $Database -ExistingOnly

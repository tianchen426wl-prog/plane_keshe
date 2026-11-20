param([string]$Database='air_ticket')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'db-common.ps1')
if(-not $PSBoundParameters.ContainsKey('Database') -and $env:DB_NAME){$Database=$env:DB_NAME}
if($Database -notin @('air_ticket','air_ticket_test','air_ticket_install_test','air_ticket_feature_install_test')){throw 'Unexpected database'}
Set-DbCredential
try {
 $directory=Join-Path $ProjectRoot '.local/backups'
 $null=New-Item -ItemType Directory -Path $directory -Force
 $backup=Join-Path $directory ($Database+'_before_flight_features_'+(Get-Date -Format 'yyyyMMdd_HHmmss')+'.sql')
 $dbHost=if($env:DB_HOST){$env:DB_HOST}else{'127.0.0.1'}
 $dbPort=if($env:DB_PORT){$env:DB_PORT}else{'3306'}
 & mysqldump.exe --no-defaults "--host=$dbHost" "--port=$dbPort" "--user=$env:DB_USERNAME" --default-character-set=utf8mb4 --single-transaction --routines --triggers --events --set-gtid-purged=OFF "--result-file=$backup" $Database
 if($LASTEXITCODE){throw 'Backup failed'}
 if((Get-Item -LiteralPath $backup).Length -lt 1000){throw 'Backup unexpectedly small'}
 Write-Output $backup
}finally{Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue}

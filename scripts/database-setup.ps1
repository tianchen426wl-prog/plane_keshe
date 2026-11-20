param(
 [Parameter(Mandatory=$true)][string]$Database,
 [switch]$RequireEmpty,
 [switch]$ExistingOnly
)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'env-common.ps1')
. (Join-Path $PSScriptRoot 'backend-artifact.ps1')
Import-ProjectEnvironment
Set-ProjectToolPaths
if($Database -notmatch '^[A-Za-z0-9_]{1,64}$'){throw 'Database name may contain only letters, digits and underscores'}
$jar=Get-ProjectBackendArtifact -ProjectRoot $root
$java=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/java.exe'}else{'java.exe'}
$arguments=@('-jar',$jar,'--database-init-only=true',"--spring.datasource.url=jdbc:mysql://$($env:DB_HOST ?? '127.0.0.1'):$($env:DB_PORT ?? '3306')/${Database}?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false",'--spring.flyway.enabled=true','--app.database.initialize=true')
if($RequireEmpty){$arguments+='--app.database.require-empty=true'}
if($ExistingOnly){$arguments+='--app.database.create-if-missing=false'}
Push-Location $root
try {
 & $java @arguments
 if($LASTEXITCODE){throw 'Database setup failed; see the backend initialization error above'}
}finally{Pop-Location}

$ErrorActionPreference='Stop'
$ProjectRoot=Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'env-common.ps1')
Import-ProjectEnvironment
function Get-LocalDbValue([string]$Key) {
 $file=Join-Path $ProjectRoot '.local/application-local.yml'
 if(-not(Test-Path $file)){return $null}
 $value=[regex]::Match((Get-Content $file -Raw),"(?m)^\s+$Key\s*:\s*([^\r\n]*)").Groups[1].Value.Trim()
 if($value.StartsWith("'")) {
  $quoted=[regex]::Match($value,"^'((?:[^']|'')*)'\s*(?:#.*)?$")
  if(-not $quoted.Success){throw "Invalid single-quoted local setting: $Key"}
  return $quoted.Groups[1].Value.Replace("''", "'")
 }
 return ($value -replace '\s+#.*$', '').Trim().Trim('"')
}
function Set-DbCredential {
 if(-not $env:DB_USERNAME){
  $env:DB_USERNAME=Get-LocalDbValue 'db-username'
  if(-not $env:DB_USERNAME){$env:DB_USERNAME='root'}
 }
 if(-not $env:DB_PASSWORD){
  $env:DB_PASSWORD=Get-LocalDbValue 'db-password'
  if(-not $env:DB_PASSWORD){throw 'Set DB_PASSWORD or local.db-password in .local/application-local.yml'}
 }
 $env:MYSQL_PWD=$env:DB_PASSWORD
}
function Invoke-Database([string]$Database,[string]$Sql) {
 if($Database -and $Database -notmatch '^[A-Za-z0-9_]+$'){throw 'Invalid database name'}
 $start=[Diagnostics.ProcessStartInfo]::new()
 $start.FileName=(Get-Command mysql.exe -ErrorAction Stop).Source
 $start.UseShellExecute=$false;$start.CreateNoWindow=$true
 $dbHost=if($env:DB_HOST){$env:DB_HOST}else{'127.0.0.1'}
 $dbPort=if($env:DB_PORT){$env:DB_PORT}else{'3306'}
 foreach($arg in @('--no-defaults',"--host=$dbHost","--port=$dbPort","--user=$env:DB_USERNAME",'--default-character-set=utf8mb4','--batch','--skip-column-names')){$start.ArgumentList.Add($arg)}
 if($Database){$start.ArgumentList.Add($Database)}
 $start.RedirectStandardInput=$true;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true
 $start.StandardInputEncoding=[Text.UTF8Encoding]::new($false)
 $start.StandardOutputEncoding=[Text.UTF8Encoding]::new($false)
 $start.StandardErrorEncoding=[Text.UTF8Encoding]::new($false)
 $process=[Diagnostics.Process]::new();$process.StartInfo=$start
 $null=$process.Start();$stdout=$process.StandardOutput.ReadToEndAsync();$stderr=$process.StandardError.ReadToEndAsync()
 $process.StandardInput.Write($Sql);$process.StandardInput.Close();$process.WaitForExit()
 $output=$stdout.GetAwaiter().GetResult();$errorText=$stderr.GetAwaiter().GetResult()
 if($process.ExitCode){throw "Database command failed: $errorText"}
 return $output.Trim()
}

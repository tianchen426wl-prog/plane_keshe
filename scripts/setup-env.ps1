param([switch]$FromLocalConfig)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$destination = Join-Path $root '.env'
if (Test-Path -LiteralPath $destination) {
 Write-Host '.env already exists; kept all existing settings.'
 return
}
$template = [IO.File]::ReadAllText((Join-Path $root '.env.example'))
$secret = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$template = $template.Replace('replace_with_a_random_base64_secret_at_least_32_bytes', $secret)
if ($FromLocalConfig) {
 $localFile = Join-Path $root '.local/application-local.yml'
 if (-not (Test-Path -LiteralPath $localFile)) { throw 'No .local/application-local.yml to migrate.' }
 . (Join-Path $PSScriptRoot 'db-common.ps1')
 $mapping = [ordered]@{
  DB_USERNAME='db-username'; DB_PASSWORD='db-password'; JWT_SECRET='jwt-secret'
  AI_ENABLED='ai-enabled'; DEEPSEEK_API_KEY='deepseek-api-key'
 }
 foreach ($key in $mapping.Keys) {
  $value = Get-LocalDbValue $mapping[$key]
  if ($null -ne $value -and $value -ne '') {
   if ($value -match '[\r\n]') { throw "Local setting must be on a single line: $key" }
   $quoted = "'" + $value.Replace('\', '\\').Replace("'", "\'") + "'"
   $template = [regex]::Replace($template, "(?m)^$key=.*$", [Text.RegularExpressions.MatchEvaluator]{param($match) "$key=$quoted"})
  }
 }
}
$stream = [IO.File]::Open($destination, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write)
try {
 $bytes = [Text.UTF8Encoding]::new($false).GetBytes($template)
 $stream.Write($bytes, 0, $bytes.Length)
} finally { $stream.Dispose() }
if ($FromLocalConfig) {
 Write-Host 'Created .env from local configuration; kept the original YAML file. Check database settings before starting.'
} else {
 Write-Host 'Created .env with a random JWT secret. Set DB_USERNAME and DB_PASSWORD before starting.'
}

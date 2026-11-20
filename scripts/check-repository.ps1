# Inspect the staged Git snapshot. Report paths only, never secret values.
$ErrorActionPreference='Stop'
$repoRoot=(& git rev-parse --show-toplevel 2>$null)
if($LASTEXITCODE -ne 0){throw 'Run this check inside the project Git repository.'}
Push-Location $repoRoot
try {
 $files=@(& git -c core.quotepath=false ls-files --cached)
 if($LASTEXITCODE -ne 0){throw 'Cannot read the Git index.'}
 $issues=[Collections.Generic.List[string]]::new()
 $sensitive=@{}
 $envFile=Join-Path $repoRoot '.env'
 if(Test-Path -LiteralPath $envFile){
  foreach($line in Get-Content -LiteralPath $envFile){
   if($line -match '^\s*(DB_PASSWORD|JWT_SECRET|DEEPSEEK_API_KEY|GH_TOKEN|GITHUB_TOKEN)\s*=\s*(.+?)\s*$'){
    $value=$Matches[2].Trim().Trim([char]39,[char]34)
    if($value.Length -ge 8 -and $value -notmatch '^replace_with_'){$sensitive[$Matches[1]]=$value}
   }
  }
 }
 foreach($file in $files){
  $normalized=$file.Replace('\','/')
  if($normalized -match '(^|/)(node_modules|target|dist|logs|tmp|\.local|\.git|\.aws|\.codex|__pycache__)(/|$)' -or
     ($normalized -match '(^|/)\.env($|\.)' -and $normalized -notmatch '(^|/)\.env\.example$') -or
     $normalized -match '\.(log|jar|war|class|pem|key|p12|pfx|jks|dump|bak)$' -or
     $normalized -match '^air-ticket-server/sql/legacy/'){
   $issues.Add("Excluded local/generated file is staged: $file");continue
  }
  $entry=(& git ls-files --stage -- $file)
  if($entry -match '^120000 '){$issues.Add("Review symbolic link before committing: $file");continue}
  $size=(& git cat-file -s ":$file")
  if($LASTEXITCODE -ne 0){throw "Cannot inspect staged file: $file"}
  if([long]$size -gt 20MB){$issues.Add("File exceeds project 20 MB limit: $file")}
  if($file -notmatch '\.(java|xml|yml|yaml|json|md|ps1|py|cjs|js|vue|txt|sql|html|properties|css|sh|example)$' -and $file -notmatch '(^|/)(\.gitignore|\.gitattributes|pre-commit)$'){continue}
  $content=((& git show ":$file") -join "`n")
  if($LASTEXITCODE -ne 0){throw "Cannot read staged contents: $file"}
  if($content -match '(?m)-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----|\bgh[pousr]_[A-Za-z0-9]{20,}|\bgithub_pat_[A-Za-z0-9_]{30,}|\bsk-[A-Za-z0-9_-]{20,}'){
   $issues.Add("Possible credential/private key in staged file: $file")
  }
  foreach($key in $sensitive.Keys){if($content.Contains($sensitive[$key])){$issues.Add("Local $key value appears in staged file: $file")}}
 }
 if($issues.Count){$issues|Sort-Object -Unique|ForEach-Object{Write-Host $_ -ForegroundColor Red};throw 'Repository check failed. Unstage local files or remove credentials before committing.'}
 Write-Host "Repository check passed: $($files.Count) staged files; no excluded files or detected credentials."
}finally{Pop-Location}

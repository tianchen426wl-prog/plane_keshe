param([Parameter(Mandatory=$true)][ValidateNotNullOrEmpty()][string]$Message)
$ErrorActionPreference='Stop'
$repoRoot=Split-Path $PSScriptRoot -Parent
function Invoke-CheckedGit {
 param([Parameter(ValueFromRemainingArguments=$true)][string[]]$GitArguments)
 & git @GitArguments
 if($LASTEXITCODE -ne 0){throw "Git command failed: $($GitArguments[0])"}
}
Push-Location $repoRoot
try {
 $branch=(& git branch --show-current)
 if($branch -ne 'main'){throw 'Switch to main before using this project sync helper.'}
 if(Test-Path (Join-Path $repoRoot '.git/rebase-merge')){throw 'Finish or abort the existing rebase first.'}
 if(Test-Path (Join-Path $repoRoot '.git/rebase-apply')){throw 'Finish or abort the existing rebase first.'}
 if(Test-Path (Join-Path $repoRoot '.git/MERGE_HEAD')){throw 'Finish or abort the existing merge first.'}
 if($IsWindows){
  $proxy=Get-ItemProperty -LiteralPath 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings' -ErrorAction SilentlyContinue
  if($proxy.ProxyEnable -eq 1 -and $proxy.ProxyServer -match '^https?://'){
   Invoke-CheckedGit config --local http.https://github.com.proxy $proxy.ProxyServer
  }elseif($proxy.ProxyEnable -eq 0){
   & git config --local --unset-all http.https://github.com.proxy
   if($LASTEXITCODE -gt 1){throw 'Cannot refresh the repository proxy configuration.'}
  }
 }
 Invoke-CheckedGit config core.hooksPath .githooks
 Invoke-CheckedGit add --all
 & (Join-Path $PSScriptRoot 'check-repository.ps1')
 & git diff --cached --quiet
 if($LASTEXITCODE -eq 1){Invoke-CheckedGit commit -m $Message}elseif($LASTEXITCODE -ne 0){throw 'Cannot inspect staged changes.'}
 Invoke-CheckedGit fetch origin main
 & git rebase origin/main
 if($LASTEXITCODE -ne 0){
  & git rebase --abort
  throw 'Remote changes conflict with your commit. Rebase was aborted; your local commit is preserved. Resolve the changes before retrying. Never force-push main.'
 }
 Invoke-CheckedGit push -u origin main
 Write-Host 'GitHub sync completed. Local configuration and generated files stay on this computer.'
}finally{Pop-Location}

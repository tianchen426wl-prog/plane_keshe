# Shared dotenv loader. Values are data: never execute or expand shell expressions.
function Import-ProjectEnvironment {
 param([string]$Path = (Join-Path (Split-Path $PSScriptRoot -Parent) '.env'))
 if (-not (Test-Path -LiteralPath $Path)) { return }
 $settings = [ordered]@{}
 $lineNumber = 0
 foreach ($line in [IO.File]::ReadAllLines($Path)) {
  $lineNumber++
  if ($line -match '^\s*(#.*)?$') { continue }
  $entry = [regex]::Match($line, '^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$')
  if (-not $entry.Success) { throw "Invalid .env syntax at line $lineNumber (expected KEY=value)" }
  $key = $entry.Groups[1].Value
  $value = $entry.Groups[2].Value.Trim()
  if ($value.StartsWith("'")) {
   $quoted = [regex]::Match($value, "^'((?:\\.|[^'\\])*)'\s*(?:#.*)?$")
   if (-not $quoted.Success) { throw "Invalid .env quoted value at line $lineNumber" }
   $value = [regex]::Replace($quoted.Groups[1].Value, "\\(['\\])", '$1')
  } elseif ($value.StartsWith('"')) {
   $quoted = [regex]::Match($value, '^"((?:\\.|[^"\\])*)"\s*(?:#.*)?$')
   if (-not $quoted.Success) { throw "Invalid .env quoted value at line $lineNumber" }
   $value = [regex]::Replace($quoted.Groups[1].Value, '\\([nrt"\\])', {
    param($match)
    switch ($match.Groups[1].Value) {
     'n' { "`n" }; 'r' { "`r" }; 't' { "`t" }
     default { $match.Groups[1].Value }
    }
   })
  } else {
   $value = ($value -replace '\s+#.*$', '').TrimEnd()
  }
  $settings[$key] = $value
 }
 # Parse the complete file before changing the process environment.
 foreach ($key in $settings.Keys) {
  if ($null -eq [Environment]::GetEnvironmentVariable($key, 'Process')) {
   [Environment]::SetEnvironmentVariable($key, $settings[$key], 'Process')
  }
 }
}

function Set-ProjectToolPaths {
 foreach ($setting in @('JAVA_HOME', 'MAVEN_HOME')) {
  $directory = [Environment]::GetEnvironmentVariable($setting, 'Process')
  if ($directory) {
   $bin = Join-Path $directory 'bin'
   if (-not (Test-Path -LiteralPath $bin -PathType Container)) { throw "Check $setting in .env: bin directory not found" }
   $otherPaths = @($env:PATH -split [IO.Path]::PathSeparator | Where-Object { $_ -ne $bin })
   $env:PATH = (@($bin) + $otherPaths) -join [IO.Path]::PathSeparator
  }
 }
}

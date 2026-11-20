# Prefer the newest executable package; a running Windows JAR may prevent Maven replacement.
function Get-ProjectBackendArtifact {
 param([Parameter(Mandatory=$true)][string]$ProjectRoot)
 Add-Type -AssemblyName System.IO.Compression.FileSystem
 $candidates=@('air-ticket-server/target/air-ticket-server-1.0.0.jar','air-ticket-server/target/air-ticket-server-1.0.0-experience.jar') |
  ForEach-Object { Get-Item -LiteralPath (Join-Path $ProjectRoot $_) -ErrorAction SilentlyContinue } |
  Sort-Object LastWriteTime -Descending
 foreach($candidate in $candidates){
  $archive=$null
  try {
   $archive=[IO.Compression.ZipFile]::OpenRead($candidate.FullName)
   $entry=$archive.GetEntry('META-INF/MANIFEST.MF')
   if($null -eq $entry){continue}
   $reader=[IO.StreamReader]::new($entry.Open())
   try {$manifest=$reader.ReadToEnd()} finally {$reader.Dispose()}
   if($manifest.Contains('org.springframework.boot.loader.JarLauncher')){return $candidate.FullName}
  } catch { Write-Verbose "Skipping unavailable backend package: $($candidate.Name)" }
  finally { if($null -ne $archive){$archive.Dispose()} }
 }
 throw 'Build an executable backend package first: mvn.cmd -f air-ticket-server/pom.xml -DskipTests package'
}

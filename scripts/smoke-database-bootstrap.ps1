# Packaged-jar acceptance test. Only a freshly allocated local test database is removed.
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'db-common.ps1')
Set-DbCredential
$database='air_ticket_boot_smoke_'+[guid]::NewGuid().ToString('N').Substring(0,12)
if($database -notmatch '^air_ticket_boot_smoke_[a-f0-9]{12}$'){throw 'Invalid test fixture name'}
$owned=$false
$memberProcess=$null
$minimumRunId=0
$runDirectory=Join-Path $ProjectRoot ('.local/member-startup-'+[guid]::NewGuid().ToString('N').Substring(0,8))
$jar=Join-Path $ProjectRoot 'air-ticket-server/target/air-ticket-server-1.0.0.jar'
$javaExecutable=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/java.exe'}else{(Get-Command java.exe).Source}
$null=New-Item -ItemType Directory -Path $runDirectory -Force
$copy=Join-Path $runDirectory 'air-ticket-server.jar'
Copy-Item -LiteralPath $jar -Destination $copy
$listener=[Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,0)
$listener.Start();$port=$listener.LocalEndpoint.Port;$listener.Stop()
$baseUrl="http://127.0.0.1:$port"

function Stop-MemberTestProcess {
 if($script:memberProcess){
  if(-not $script:memberProcess.HasExited){$script:memberProcess.Kill();$script:memberProcess.WaitForExit()}
  [IO.File]::WriteAllText((Join-Path $runDirectory ($script:phase+'-stdout.log')),$script:stdoutTask.GetAwaiter().GetResult(),[Text.UTF8Encoding]::new($false))
  [IO.File]::WriteAllText((Join-Path $runDirectory ($script:phase+'-stderr.log')),$script:stderrTask.GetAwaiter().GetResult(),[Text.UTF8Encoding]::new($false))
  $script:memberProcess.Dispose();$script:memberProcess=$null
 }
}
function Start-MemberTestProcess([string]$PhaseName) {
 $script:phase=$PhaseName
 $start=[Diagnostics.ProcessStartInfo]::new()
 $start.FileName=$javaExecutable;$start.WorkingDirectory=$runDirectory
 $start.UseShellExecute=$false;$start.CreateNoWindow=$true
 $start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true
 $start.StandardOutputEncoding=[Text.UTF8Encoding]::new($false);$start.StandardErrorEncoding=[Text.UTF8Encoding]::new($false)
 foreach($argument in @('-jar',$copy,"--server.port=$port",'--ai.enabled=false','--flights.window-days=2','--flights.generation.enabled=true','--app.database.initialize=true','--app.database.create-if-missing=true','--spring.flyway.enabled=true')){$start.ArgumentList.Add($argument)}
 $start.Environment['DB_HOST']='127.0.0.1';$start.Environment['DB_PORT']='3306'
 $start.Environment['DB_NAME']=$database;$start.Environment['DB_USERNAME']=$env:DB_USERNAME;$start.Environment['DB_PASSWORD']=$env:DB_PASSWORD
 $start.Environment['JWT_SECRET']='cGFja2FnZWQtYm9vdHN0cmFwLXRlc3Qtb25seS1zZWNyZXQ='
 # The application process has neither source SQL folders nor mysql.exe on its PATH.
 $start.Environment['PATH']=(Split-Path $javaExecutable -Parent)+';'+(Join-Path $env:SystemRoot 'System32')
 $script:memberProcess=[Diagnostics.Process]::new();$script:memberProcess.StartInfo=$start
 $null=$script:memberProcess.Start()
 $script:stdoutTask=$script:memberProcess.StandardOutput.ReadToEndAsync();$script:stderrTask=$script:memberProcess.StandardError.ReadToEndAsync()
 $deadline=(Get-Date).AddSeconds(180)
 while((Get-Date) -lt $deadline){
  if($script:memberProcess.HasExited){Stop-MemberTestProcess;throw "Packaged app exited early; inspect private logs in $runDirectory"}
  try{
   $calendar=Invoke-RestMethod "$baseUrl/api/flights/calendar" -TimeoutSec 2
   if($calendar.code -eq 200 -and $calendar.data.latestRun.status -eq 'SUCCESS' -and [long]$calendar.data.latestRun.id -gt $script:minimumRunId){return $calendar}
  }catch{}
  Start-Sleep -Milliseconds 500
 }
 throw "Packaged app startup timed out; inspect private logs in $runDirectory"
}

try {
 if([int](Invoke-Database '' "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$database';") -ne 0){throw 'Fixture database already exists; refusing to reuse it'}
 $owned=$true
 Write-Output 'Starting copied jar with no pre-created database and no source SQL directory.'
 $first=Start-MemberTestProcess 'first-start'
 if([int](Invoke-Database $database 'SELECT COUNT(*) FROM company;') -ne 14){throw 'Airline seed mismatch'}
 if([int](Invoke-Database $database 'SELECT COUNT(*) FROM flyway_schema_history WHERE success=1;') -ne 8){throw 'Migration history mismatch'}
 if([int](Invoke-Database $database 'SELECT COUNT(*) FROM hbxx WHERE source_type="DEMO";') -ne 224){throw 'Flight generation mismatch'}
 $adminLogin=Invoke-RestMethod "$baseUrl/api/admin/login" -Method Post -ContentType 'application/json' -Body (@{username='admin';password='admin123'}|ConvertTo-Json)
 if($adminLogin.code -ne 200){throw 'New administrator cannot log in'}
 $registration=Invoke-RestMethod "$baseUrl/api/user/register" -Method Post -ContentType 'application/json' -Body (@{username='member_smoke';password='MemberSmoke123456';name='Startup test';gender=0}|ConvertTo-Json)
 if($registration.code -ne 200){throw 'Registration failed'}
 $login=Invoke-RestMethod "$baseUrl/api/user/login" -Method Post -ContentType 'application/json' -Body (@{username='member_smoke';password='MemberSmoke123456'}|ConvertTo-Json)
 $headers=@{Authorization='Bearer '+$login.data.token}
 $date=([datetime]$first.data.dates[1].serviceDate).ToString('yyyy-MM-dd')
 $search=Invoke-RestMethod "$baseUrl/api/flights?date=$date"
 $flight=$search.data.records[0]
 $detail=Invoke-RestMethod "$baseUrl/api/flights/$($flight.hbid)"
 $cabin=$detail.data.cabins[0]
 $body=@{hbid=$flight.hbid;cabinId=$cabin.cabinId;quantity=1;passengers=@('Startup test');seats=@()}
 $quote=Invoke-RestMethod "$baseUrl/api/quotes" -Method Post -Headers $headers -ContentType 'application/json' -Body ($body|ConvertTo-Json)
 $body.quoteId=$quote.data.quoteId;$body.requestId=[guid]::NewGuid().ToString()
 $hold=Invoke-RestMethod "$baseUrl/api/jpdd/hold" -Method Post -Headers $headers -ContentType 'application/json' -Body ($body|ConvertTo-Json)
 $paid=Invoke-RestMethod "$baseUrl/api/jpdd/create" -Method Post -Headers $headers -ContentType 'application/json' -Body (@{holdId=$hold.data.holdId}|ConvertTo-Json)
 if($paid.code -ne 200){throw 'Fresh database booking failed'}
 $before=Invoke-Database $database 'SELECT ddid,hbid,cabin_id,quantity,cost,status FROM jpdd ORDER BY ddid;'
 Stop-MemberTestProcess
 Write-Output 'Restarting the same copied jar and checking preserved paid order.'
 $minimumRunId=[long]$first.data.latestRun.id
 $second=Start-MemberTestProcess 'second-start'
 $after=Invoke-Database $database 'SELECT ddid,hbid,cabin_id,quantity,cost,status FROM jpdd ORDER BY ddid;'
 if($before -cne $after){throw 'Paid order changed after restart'}
 if([int]$second.data.latestRun.createdCount -ne 0){throw 'Repeated startup duplicated dated flights'}
 if([int](Invoke-Database $database 'SELECT COUNT(*) FROM v_inventory_health WHERE difference<>0;') -ne 0){throw 'Inventory mismatch'}
 if([int](Invoke-Database $database 'SELECT COUNT(*) FROM v_seat_health WHERE difference<>0 OR capacity_difference<>0;') -ne 0){throw 'Seat mismatch'}
 $result=[ordered]@{passed=$true;packagedJarOnly=$true;sourceSqlDirectoryAvailable=$false;mysqlClientInChildPath=$false;databaseAbsentBeforeStart=$true;flywaySqlVersions=8;airlines=14;dailySchedules=112;windowDays=2;generatedFlights=224;administratorLogin=$true;bookingPassed=$true;paidOrderPreservedAfterRestart=$true;repeatedStartupCreatedFlights=0;inventoryDifferences=0;seatDifferences=0}
 $report=$result|ConvertTo-Json
 [IO.File]::WriteAllText((Join-Path $ProjectRoot 'docs/evidence/database-bootstrap-jar-smoke.json'),$report,[Text.UTF8Encoding]::new($false))
 Write-Output $report
}finally{
 Stop-MemberTestProcess
 if($owned -and $database -match '^air_ticket_boot_smoke_[a-f0-9]{12}$') {Invoke-Database '' "DROP DATABASE IF EXISTS ``$database``;"}
 Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}

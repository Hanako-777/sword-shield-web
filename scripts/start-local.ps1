# Start the whole demo stack without Docker/Nacos:
#   MySQL (D:\xhlj-env\mysql) + 8 Spring Boot services + Vue dev server.
# Usage: powershell -ExecutionPolicy Bypass -File scripts\start-local.ps1
$ErrorActionPreference = "Stop"

$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Logs = Join-Path $Root "logs"
New-Item -ItemType Directory -Force -Path $Logs | Out-Null

$Java = "C:\Program Files\Java\jdk-21.0.10\bin\java.exe"
if (-not (Test-Path $Java)) { $Java = (Get-Command java).Source }

$MysqlHome = "D:\xhlj-env\mysql"
$MysqlIni = "D:\xhlj-env\my.ini"

function Test-Port([int]$Port) {
  try {
    $c = New-Object Net.Sockets.TcpClient
    $c.Connect("127.0.0.1", $Port)
    $c.Close()
    return $true
  } catch { return $false }
}

function Wait-Port([int]$Port, [int]$TimeoutSec = 120) {
  $sw = [Diagnostics.Stopwatch]::StartNew()
  while ($sw.Elapsed.TotalSeconds -lt $TimeoutSec) {
    if (Test-Port $Port) { return $true }
    Start-Sleep -Milliseconds 800
  }
  return $false
}

function Start-Service-Jar([string]$Name, [string]$Jar, [int]$Port, [string[]]$Extra) {
  if (Test-Port $Port) {
    Write-Host "[skip] $Name already listening on $Port"
    return
  }
  if (-not (Test-Path $Jar)) {
    Write-Host "[fail] jar not found: $Jar (run scripts\build-local.ps1 first)"
    return
  }
  $args = @("-Xms128m", "-Xmx420m", "-Dfile.encoding=UTF-8", "-jar", $Jar,
            "--spring.cloud.nacos.discovery.enabled=false",
            "--spring.cloud.nacos.config.enabled=false") + $Extra
  $quoted = $args | ForEach-Object { if ($_ -match '\s') { '"' + $_ + '"' } else { $_ } }
  Start-Process -FilePath $Java -ArgumentList ($quoted -join ' ') -WorkingDirectory $Root `
    -RedirectStandardOutput (Join-Path $Logs "$Name.out.log") `
    -RedirectStandardError (Join-Path $Logs "$Name.err.log") `
    -WindowStyle Hidden | Out-Null
  if (Wait-Port $Port 180) { Write-Host "[ ok ] $Name -> $Port" }
  else { Write-Host "[fail] $Name did not open $Port, see logs\$Name.err.log" }
}

# 1. MySQL
if (-not (Test-Port 3306)) {
  if (-not (Test-Path $MysqlIni)) { throw "MySQL config not found: $MysqlIni (run scripts\setup-mysql.ps1 first)" }
  Write-Host "Starting MySQL ..."
  Start-Process -FilePath (Join-Path $MysqlHome "bin\mysqld.exe") `
    -ArgumentList "--defaults-file=$MysqlIni" -WindowStyle Hidden `
    -RedirectStandardError (Join-Path $Logs "mysql.err.log") | Out-Null
  if (-not (Wait-Port 3306 90)) { throw "MySQL failed to start, see logs\mysql.err.log" }
  Write-Host "[ ok ] MySQL -> 3306"
} else {
  Write-Host "[skip] MySQL already listening on 3306"
}

# 2. Business services
$suffix = "0.1.0-SNAPSHOT-exec.jar"
Start-Service-Jar "user"        (Join-Path $Root "sword-shield-user\target\sword-shield-user-$suffix") 8101 @()
Start-Service-Jar "activity"    (Join-Path $Root "sword-shield-activity\target\sword-shield-activity-$suffix") 8102 @()
Start-Service-Jar "content"     (Join-Path $Root "sword-shield-content\target\sword-shield-content-$suffix") 8103 @()
Start-Service-Jar "interaction" (Join-Path $Root "sword-shield-interaction\target\sword-shield-interaction-$suffix") 8104 @()
Start-Service-Jar "file"        (Join-Path $Root "sword-shield-file\target\sword-shield-file-$suffix") 8105 @("--minio.enabled=false", "--minio.local-dir=$(Join-Path $Root 'uploads')")
Start-Service-Jar "admin"       (Join-Path $Root "sword-shield-admin\target\sword-shield-admin-$suffix") 8106 @()
Start-Service-Jar "ai"          (Join-Path $Root "sword-shield-ai\target\sword-shield-ai-$suffix") 8107 @("--ai.activity-service-url=http://localhost:8102")
Start-Service-Jar "payment"     (Join-Path $Root "sword-shield-payment\target\sword-shield-payment-$suffix") 8108 @()

# 3. Gateway (static routes, no Nacos)
Start-Service-Jar "gateway" (Join-Path $Root "sword-shield-gateway\target\sword-shield-gateway-$suffix") 8080 @("--spring.profiles.active=local")

# 4. Frontend
if (-not (Test-Port 5173)) {
  $ui = Join-Path $Root "sword-shield-web-ui"
  if (-not (Test-Path (Join-Path $ui "node_modules"))) {
    Write-Host "Installing frontend dependencies (first run) ..."
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "npm install --no-audit --no-fund" -WorkingDirectory $ui -Wait -NoNewWindow | Out-Null
  }
  $webLog = Join-Path $Logs "web-ui.log"
  Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "npm run dev > `"$webLog`" 2>&1" -WorkingDirectory $ui -WindowStyle Hidden | Out-Null
  if (Wait-Port 5173 90) { Write-Host "[ ok ] web-ui -> 5173" } else { Write-Host "[fail] web-ui did not start, see logs\web-ui.log" }
} else {
  Write-Host "[skip] web-ui already listening on 5173"
}

Write-Host ""
Write-Host "Demo is up:"
Write-Host "  Web      http://localhost:5173"
Write-Host "  Gateway  http://localhost:8080/actuator/health"
Write-Host "  Accounts demo/123456   admin/admin123"
Write-Host "Stop with: powershell -ExecutionPolicy Bypass -File scripts\stop-local.ps1"

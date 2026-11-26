# One-time MySQL setup for the offline demo (no installer, no admin rights).
# Usage: powershell -ExecutionPolicy Bypass -File scripts\setup-mysql.ps1 [-ZipPath "D:\JAVA EE 课设\env\mysql.zip"]
param(
  [string]$ZipPath = "D:\JAVA EE 课设\env\mysql.zip",
  [string]$Base = "D:\xhlj-env"
)
$ErrorActionPreference = "Stop"

$MysqlHome = Join-Path $Base "mysql"
$Data = Join-Path $Base "mysql-data"
$Ini = Join-Path $Base "my.ini"

if (-not (Test-Path $ZipPath)) { throw "MySQL zip not found: $ZipPath" }
if (-not (Test-Path (Join-Path $MysqlHome "bin\mysqld.exe"))) {
  New-Item -ItemType Directory -Force -Path $Base | Out-Null
  Write-Host "Extracting MySQL ..."
  $tar = Join-Path $env:SystemRoot "System32\tar.exe"
  if (Test-Path $tar) { & $tar -xf $ZipPath -C $Base }
  else { Expand-Archive -Path $ZipPath -DestinationPath $Base -Force }
  $extracted = Get-ChildItem $Base -Directory | Where-Object { $_.Name -like "mysql-8.0*" } | Select-Object -First 1
  if ($extracted -and $extracted.FullName -ne $MysqlHome) {
    if (Test-Path $MysqlHome) { Remove-Item $MysqlHome -Recurse -Force }
    Rename-Item $extracted.FullName "mysql"
  }
}

if (-not (Test-Path (Join-Path $MysqlHome "bin\mysqld.exe"))) { throw "mysqld.exe not found after extraction" }

Set-Content -Path $Ini -Encoding ASCII -Value @"
[mysqld]
basedir=$($MysqlHome.Replace('\','/'))
datadir=$($Data.Replace('\','/'))
port=3306
bind-address=127.0.0.1
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
default_authentication_plugin=mysql_native_password
max_connections=300

[client]
port=3306
default-character-set=utf8mb4
"@

$mysqld = Join-Path $MysqlHome "bin\mysqld.exe"
$mysql = Join-Path $MysqlHome "bin\mysql.exe"

if (-not (Test-Path $Data)) {
  Write-Host "Initializing data directory ..."
  & $mysqld "--defaults-file=$Ini" "--initialize-insecure" "--console"
  if ($LASTEXITCODE -ne 0) { throw "mysqld initialize failed" }
}

Write-Host "Starting MySQL ..."
Start-Process -FilePath $mysqld -ArgumentList "--defaults-file=$Ini" -WindowStyle Hidden | Out-Null
$ok = $false
for ($i = 0; $i -lt 90; $i++) {
  Start-Sleep -Seconds 1
  try { $c = New-Object Net.Sockets.TcpClient; $c.Connect("127.0.0.1", 3306); $c.Close(); $ok = $true; break } catch {}
}
if (-not $ok) { throw "MySQL failed to start" }

Write-Host "Creating database and setting root password ..."
& $mysql --protocol=TCP -h 127.0.0.1 -P 3306 -u root --skip-password -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '123456'; CREATE DATABASE IF NOT EXISTS sword_shield CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if ($LASTEXITCODE -ne 0) { throw "Failed to create database" }

Write-Host ""
Write-Host "MySQL is ready:"
Write-Host "  basedir $MysqlHome"
Write-Host "  datadir $Data"
Write-Host "  jdbc:mysql://localhost:3306/sword_shield  root/123456"

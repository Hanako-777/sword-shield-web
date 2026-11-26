# Stop all demo processes started by start-local.ps1.
# Usage: powershell -ExecutionPolicy Bypass -File scripts\stop-local.ps1 [-StopMysql]
param([switch]$StopMysql)

$java = Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
  Where-Object { $_.CommandLine -match 'sword-shield-.*\.jar' }
foreach ($p in $java) {
  try {
    Stop-Process -Id $p.ProcessId -Force -ErrorAction Stop
    Write-Host "stopped java $($p.ProcessId)"
  } catch {
    Write-Host "skip $($p.ProcessId): $($_.Exception.Message)"
  }
}

$node = Get-CimInstance Win32_Process -Filter "Name='node.exe'" |
  Where-Object { $_.CommandLine -match 'vite' }
foreach ($p in $node) {
  try {
    Stop-Process -Id $p.ProcessId -Force -ErrorAction Stop
    Write-Host "stopped vite $($p.ProcessId)"
  } catch {
    Write-Host "skip $($p.ProcessId): $($_.Exception.Message)"
  }
}

if ($StopMysql) {
  Get-Process mysqld -ErrorAction SilentlyContinue | ForEach-Object {
    Stop-Process -Id $_.Id -Force
    Write-Host "stopped mysqld $($_.Id)"
  }
}

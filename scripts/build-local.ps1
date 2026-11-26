# Build all modules as executable Spring Boot jars.
# Usage: powershell -ExecutionPolicy Bypass -File scripts\build-local.ps1
$ErrorActionPreference = "Stop"

$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$settings = Join-Path $PSScriptRoot "maven-settings.xml"

$jdk = "C:\Program Files\Java\jdk-21.0.10"
if (Test-Path $jdk) {
  $env:JAVA_HOME = $jdk
  $env:Path = "$jdk\bin;$env:Path"
}

$mvn = (Get-Command mvn -ErrorAction SilentlyContinue).Source
if (-not $mvn) {
  $mvn = "D:\IntelliJ IDEA 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd"
}
if (-not (Test-Path $mvn)) { throw "Maven not found. Install Maven or set the path in this script." }

Push-Location $Root
try {
  & $mvn -s $settings -B -DskipTests package
  if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }
  Write-Host "Build OK. Run scripts\start-local.ps1 next."
} finally {
  Pop-Location
}

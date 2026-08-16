$ErrorActionPreference = 'Stop'

$serverDirectory = $PSScriptRoot
$projectDirectory = Split-Path -Parent $serverDirectory
$javaExecutable = Join-Path $projectDirectory '.tools\jdk25\jdk-25.0.4+7\bin\java.exe'
$pluginSource = Join-Path $projectDirectory 'build\libs\worldgenerator-0.2.0-SNAPSHOT.jar'
$pluginDirectory = Join-Path $serverDirectory 'plugins'

if (-not (Test-Path -LiteralPath $javaExecutable)) {
    throw "Portable Java 25 was not found: $javaExecutable"
}
if (-not (Test-Path -LiteralPath $pluginSource)) {
    throw 'Plugin JAR not found. Build the project before starting the server.'
}

New-Item -ItemType Directory -Path $pluginDirectory -Force | Out-Null
Copy-Item -LiteralPath $pluginSource -Destination (Join-Path $pluginDirectory 'WorldGenerator.jar') -Force

Push-Location $serverDirectory
try {
    & $javaExecutable -Xms1G -Xmx2G -jar paper.jar --nogui
}
finally {
    Pop-Location
}

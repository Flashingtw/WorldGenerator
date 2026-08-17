$ErrorActionPreference = 'Stop'

$serverDirectory = $PSScriptRoot
$projectDirectory = Split-Path -Parent $serverDirectory
$javaExecutable = Join-Path $projectDirectory '.tools\jdk25\jdk-25.0.4+7\bin\java.exe'
$pluginSource = Get-ChildItem -LiteralPath (Join-Path $projectDirectory 'build\libs') `
        -Filter 'worldgenerator-*.jar' -File |
    Sort-Object LastWriteTimeUtc -Descending |
    Select-Object -First 1
$pluginDirectory = Join-Path $serverDirectory 'plugins'

if (-not (Test-Path -LiteralPath $javaExecutable)) {
    throw "Portable Java 25 was not found: $javaExecutable"
}
if ($null -eq $pluginSource) {
    throw 'Plugin JAR not found. Build the project before starting the server.'
}

New-Item -ItemType Directory -Path $pluginDirectory -Force | Out-Null
Copy-Item -LiteralPath $pluginSource.FullName `
    -Destination (Join-Path $pluginDirectory 'WorldGenerator.jar') -Force
Write-Host "Using plugin build: $($pluginSource.Name)"

Push-Location $serverDirectory
try {
    # Pin the normal test-server identity so one-off Paper command-line overrides
    # cannot leave server.properties pointing at a temporary world or port.
    & $javaExecutable -Xms1G -Xmx2G -jar paper.jar --nogui --world lobby --port 25565
}
finally {
    Pop-Location
}

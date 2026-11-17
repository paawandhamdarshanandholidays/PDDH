$ErrorActionPreference = 'Stop'

$backendDirectory = $PSScriptRoot
$backendJar = Join-Path $backendDirectory 'target\pddh-backend-0.0.1-SNAPSHOT.jar'

if (-not (Test-Path -LiteralPath $backendJar -PathType Leaf)) {
    Push-Location $backendDirectory
    try {
        & .\mvnw.cmd -f pom.xml package -DskipTests
        if ($LASTEXITCODE -ne 0) {
            throw "PDDH backend build failed with exit code $LASTEXITCODE."
        }
    }
    finally {
        Pop-Location
    }
}

if (-not (Test-Path -LiteralPath $backendJar -PathType Leaf)) {
    throw "PDDH backend JAR not found at '$backendJar'."
}

if ([string]::IsNullOrWhiteSpace($env:JAVA_TOOL_OPTIONS)) {
    $env:JAVA_TOOL_OPTIONS = '-XX:+UseG1GC -Xms512m -Xmx4g -XX:MaxMetaspaceSize=1g'
}

Write-Host "Starting PDDH from '$backendJar'."
& java -jar $backendJar
if ($LASTEXITCODE -ne 0) {
    throw "PDDH backend exited with code $LASTEXITCODE."
}

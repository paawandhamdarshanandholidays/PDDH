$ErrorActionPreference = 'Stop'

$backendDirectory = $PSScriptRoot
$repositoryDirectory = Split-Path -Parent $backendDirectory
$defaultLogRoot = Split-Path -Parent $repositoryDirectory
$backendJar = Join-Path $backendDirectory 'target\pddh-backend-0.0.1-SNAPSHOT.jar'
$logRoot = $env:PAWANDHAM_LOG_ROOT
if ([string]::IsNullOrWhiteSpace($logRoot)) {
    $logRoot = $defaultLogRoot
}
$repositoryPath = [System.IO.Path]::GetFullPath($repositoryDirectory).TrimEnd('\') + '\'
$resolvedLogRoot = [System.IO.Path]::GetFullPath($logRoot)
$logRootIsRepository = $resolvedLogRoot.TrimEnd('\').Equals(
    $repositoryPath.TrimEnd('\'), [System.StringComparison]::OrdinalIgnoreCase)
$logRootIsInsideRepository = $resolvedLogRoot.StartsWith(
    $repositoryPath, [System.StringComparison]::OrdinalIgnoreCase)
if ($logRootIsRepository -or $logRootIsInsideRepository) {
    $resolvedLogRoot = [System.IO.Path]::GetFullPath($defaultLogRoot)
}
$env:PAWANDHAM_LOG_ROOT = $resolvedLogRoot
New-Item -ItemType Directory -Path $resolvedLogRoot -Force | Out-Null

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

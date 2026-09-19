$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$testEnv = Join-Path $projectRoot 'backend/.env.test'
$names = @('TEST_DB_URL', 'TEST_DB_USERNAME', 'TEST_DB_PASSWORD')
$previous = @{}
foreach ($name in $names) { $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
Push-Location (Join-Path $projectRoot 'backend')
try {
    if (Test-Path -LiteralPath $testEnv) {
        foreach ($line in Get-Content -LiteralPath $testEnv) {
            if ($line -match '^\s*(TEST_DB_URL|TEST_DB_USERNAME|TEST_DB_PASSWORD)=(.*)$') {
                # Explicit shell values take precedence over a local file.
                if (![Environment]::GetEnvironmentVariable($Matches[1], 'Process')) {
                    [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
                }
            }
        }
    }
    foreach ($name in $names) {
        if (![Environment]::GetEnvironmentVariable($name, 'Process')) { throw "Configure $name in backend/.env.test or the environment." }
    }
    & .\mvnw.cmd -B -ntp -Pdatabase-tests verify
    if ($LASTEXITCODE -ne 0) { throw 'Database verification failed. See backend/target/failsafe-reports.' }
} finally {
    Pop-Location
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
}

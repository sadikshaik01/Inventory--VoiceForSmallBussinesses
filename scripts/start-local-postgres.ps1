param(
    [string]$PostgresBin = 'C:\Program Files\PostgreSQL\17\bin',
    [switch]$ConfigureBackend
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$localRoot = Join-Path $projectRoot '.run/postgres'
$dataPath = Join-Path $localRoot 'data'
$adminPasswordPath = Join-Path $localRoot 'admin-password.txt'
$appPasswordPath = Join-Path $localRoot 'app-password.txt'
$backendEnv = Join-Path $projectRoot 'backend/.env'
$testEnv = Join-Path $projectRoot 'backend/.env.test'
$pgCtl = Join-Path $PostgresBin 'pg_ctl.exe'
$psql = Join-Path $PostgresBin 'psql.exe'
if (!(Test-Path -LiteralPath $pgCtl)) { throw 'Set -PostgresBin to your PostgreSQL bin directory.' }
if ($ConfigureBackend -and (Test-Path -LiteralPath $backendEnv) -and
    (Select-String -LiteralPath $backendEnv -Pattern '^\s*DB_URL=' -Quiet)) {
    throw 'backend/.env already has DB_URL. Preserve it; omit -ConfigureBackend to start the existing local cluster.'
}
New-Item -ItemType Directory -Path $localRoot -Force | Out-Null
function New-LocalPassword {
    return [Convert]::ToHexString([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
}
if (!(Test-Path -LiteralPath (Join-Path $dataPath 'PG_VERSION'))) {
    if (Get-NetTCPConnection -State Listen -LocalPort 55432 -ErrorAction SilentlyContinue) {
        throw 'Port 55432 is occupied; no existing service was changed.'
    }
    [IO.File]::WriteAllText($adminPasswordPath, (New-LocalPassword))
    & (Join-Path $PostgresBin 'initdb.exe') -D $dataPath -U voicestock_admin '--auth=scram-sha-256' --encoding=UTF8 --locale=C --pwfile=$adminPasswordPath
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL cluster initialization failed.' }
}
& $pgCtl status -D $dataPath *> $null
if ($LASTEXITCODE -ne 0) {
    & $pgCtl start -D $dataPath -l (Join-Path $localRoot 'postgres.log') -o '-h 127.0.0.1 -p 55432' -w
    if ($LASTEXITCODE -ne 0) { throw 'Project-local PostgreSQL startup failed.' }
}
$previousPassword = $env:PGPASSWORD
try {
    $env:PGPASSWORD = [IO.File]::ReadAllText($adminPasswordPath).Trim()
    if (!(Test-Path -LiteralPath $appPasswordPath)) {
        $appPassword = New-LocalPassword
        [IO.File]::WriteAllText($appPasswordPath, $appPassword)
        # The password is passed via stdin, never command-line arguments or logs.
        "CREATE ROLE voicestock_app LOGIN PASSWORD '$appPassword';" |
            & $psql -h 127.0.0.1 -p 55432 -U voicestock_admin -d postgres -v ON_ERROR_STOP=1
        if ($LASTEXITCODE -ne 0) { throw 'Application role creation failed.' }
    }
    foreach ($databaseName in @('voicestock', 'voicestock_test')) {
        $exists = & $psql -h 127.0.0.1 -p 55432 -U voicestock_admin -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$databaseName'"
        if ($LASTEXITCODE -ne 0) { throw 'Database existence check failed.' }
        if ($exists -ne '1') {
            & $psql -h 127.0.0.1 -p 55432 -U voicestock_admin -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE $databaseName OWNER voicestock_app"
            if ($LASTEXITCODE -ne 0) { throw 'Database creation failed.' }
        }
    }
} finally { $env:PGPASSWORD = $previousPassword }
if ($ConfigureBackend) {
    $appPassword = [IO.File]::ReadAllText($appPasswordPath).Trim()
    $lines = if (Test-Path -LiteralPath $backendEnv) { @(Get-Content -LiteralPath $backendEnv) } else { @('PORT=8081') }
    $lines = @($lines | Where-Object { $_ -notmatch '^\s*SPRING_PROFILES_ACTIVE=' })
    $lines += @('SPRING_PROFILES_ACTIVE=postgres', 'DB_URL=jdbc:postgresql://127.0.0.1:55432/voicestock', 'DB_USERNAME=voicestock_app', "DB_PASSWORD=$appPassword", 'DB_POOL_SIZE=5')
    if (!($lines -match '^JWT_SECRET=')) {
        $lines += 'JWT_SECRET=' + [Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
        $lines += 'JWT_EXPIRATION_SECONDS=3600'
    }
    [IO.File]::WriteAllLines($backendEnv, $lines)
    if (!(Test-Path -LiteralPath $testEnv)) {
        [IO.File]::WriteAllLines($testEnv, @('TEST_DB_URL=jdbc:postgresql://127.0.0.1:55432/voicestock_test', 'TEST_DB_USERNAME=voicestock_app', "TEST_DB_PASSWORD=$appPassword"))
    }
}
Write-Output 'Project-local PostgreSQL is running on 127.0.0.1:55432. Existing PostgreSQL services were not modified.'
Write-Output 'Random local credentials are stored only in Git-ignored files; no credentials were printed.'

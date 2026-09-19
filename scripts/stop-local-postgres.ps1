param([string]$PostgresBin = 'C:\Program Files\PostgreSQL\17\bin')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$dataPath = Join-Path $projectRoot '.run/postgres/data'
if (!(Test-Path -LiteralPath (Join-Path $dataPath 'PG_VERSION'))) { throw 'No project-local PostgreSQL cluster found.' }
& (Join-Path $PostgresBin 'pg_ctl.exe') stop -D $dataPath -m fast -w
if ($LASTEXITCODE -ne 0) { throw 'Could not stop the project-local cluster.' }

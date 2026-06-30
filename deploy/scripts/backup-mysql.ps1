@echo off
REM Щоденний backup MySQL (retention 30 днів)
setlocal
set BACKUP_DIR=%~dp0..\backups
set RETENTION_DAYS=30
set MYSQL_HOST=%MYSQL_HOST%
if "%MYSQL_HOST%"=="" set MYSQL_HOST=localhost
set MYSQL_PORT=%MYSQL_PORT%
if "%MYSQL_PORT%"=="" set MYSQL_PORT=3307
set MYSQL_USER=%MYSQL_USER%
if "%MYSQL_USER%"=="" set MYSQL_USER=rmpd
set MYSQL_PASSWORD=%MYSQL_PASSWORD%
if "%MYSQL_PASSWORD%"=="" set MYSQL_PASSWORD=rmpd
set MYSQL_DATABASE=%MYSQL_DATABASE%
if "%MYSQL_DATABASE%"=="" set MYSQL_DATABASE=rmpd

if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

for /f "tokens=1-3 delims=/:. " %%a in ("%date% %time%") do set STAMP=%%c%%b%%a
set OUTFILE=%BACKUP_DIR%\rmpd_%STAMP%.sql.gz

echo Backup to %OUTFILE%
docker exec rmpd-mysql mysqldump -u%MYSQL_USER% -p%MYSQL_PASSWORD% %MYSQL_DATABASE% | gzip > "%OUTFILE%"
if errorlevel 1 exit /b 1

forfiles /p "%BACKUP_DIR%" /m rmpd_*.sql.gz /d -%RETENTION_DAYS% /c "cmd /c del @path" 2>nul
echo Done.

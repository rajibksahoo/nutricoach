@echo off
REM restart-dev.cmd - restart the NutriCoach stack (backend + frontend).
REM Works from CMD, PowerShell (.\restart-dev.cmd), or Git Bash (./restart-dev.cmd).
REM
REM Lives in the backend repo because the repo root (D:\Work\nutriapp) is not a
REM git repo, so a script sitting there is untracked and lost on a fresh clone.
REM It assumes the two repos are siblings:
REM
REM   <parent>/nutricoach/scripts/restart-dev.cmd   <- this file
REM   <parent>/nutricoach-web/
REM
REM Closes the windows it opened last time before starting new ones. Killing by
REM port alone is not enough: `mvn spring-boot:run` forks a separate JVM, so a
REM port kill takes down the forked app and leaves the parent Maven process and
REM its console window running. Repeat a few times and you have a pile of stray
REM JVMs, and the next start races the old one for port 8080.

setlocal
set "BACKEND=%~dp0.."
set "FRONTEND=%~dp0..\..\nutricoach-web"

if not exist "%FRONTEND%\package.json" (
  echo ERROR: could not find the frontend repo at:
  echo   %FRONTEND%
  echo This script expects nutricoach/ and nutricoach-web/ to be siblings.
  exit /b 1
)

echo === Closing previous NutriCoach windows ===
REM /T kills the whole process tree (cmd -^> mvn -^> java), which is the part a
REM port-based kill misses. The "Administrator:" variants match when the window
REM was opened from an elevated prompt.
taskkill /F /T /FI "WINDOWTITLE eq NutriCoach API" >nul 2>&1
taskkill /F /T /FI "WINDOWTITLE eq NutriCoach Web" >nul 2>&1
taskkill /F /T /FI "WINDOWTITLE eq Administrator:  NutriCoach API" >nul 2>&1
taskkill /F /T /FI "WINDOWTITLE eq Administrator:  NutriCoach Web" >nul 2>&1

echo === Stopping anything still on ports 8080 / 3000 ===
REM Backstop for processes this script did not start: an IDE run configuration,
REM a stray `mvn spring-boot:run`, an old `npm run dev`.
REM
REM Do NOT "improve" this by sweeping processes whose command line contains the
REM repo path. Verified on a dev machine: that also matches IntelliJ's Maven
REM embedder (parent idea64.exe) and the editor's MCP filesystem servers, all of
REM which carry the project path in their arguments. Killing by port and by the
REM window titles above only ever touches this stack.
call :killport 8080
call :killport 3000

echo === Waiting for ports to be released ===
call :waitport 8080
call :waitport 3000

echo === Ensuring Postgres container pg-test is running ===
docker start pg-test >nul 2>&1
if errorlevel 1 echo   WARNING: could not start pg-test - is Docker Desktop running?

echo === Starting backend  ^(http://localhost:8080^) ===
start "NutriCoach API" cmd /k "cd /d "%BACKEND%" && mvn spring-boot:run -Dspring-boot.run.profiles=local"

echo === Starting frontend ^(http://localhost:3000^) ===
start "NutriCoach Web" cmd /k "cd /d "%FRONTEND%" && npm run dev"

echo.
echo Waiting for the stack to come up ^(Ctrl+C to stop waiting; startup continues^)...
call :waitup 3000 "Frontend"
call :waitup 8080 "Backend "

echo.
echo Done. Two windows opened: "NutriCoach API" and "NutriCoach Web".
echo Swagger: http://localhost:8080/swagger-ui.html ^| App: http://localhost:3000
echo If a service is not ready above, read its window - the error is in there.
endlocal
goto :eof


REM ---------------------------------------------------------------------------
:sleep1
REM Roughly one second. NOT `timeout`, which aborts with "Input redirection is
REM not supported" the moment stdin is redirected - piping this script, running
REM it from CI or from a tool then floods the console and skips every wait.
ping -n 2 127.0.0.1 >nul 2>&1
goto :eof


REM ---------------------------------------------------------------------------
:killport
REM %1 = port. /T so a forked child (Maven's app JVM) goes with its parent.
for /f "tokens=5" %%p in ('netstat -ano ^| findstr "LISTENING" ^| findstr ":%~1 "') do taskkill /F /T /PID %%p >nul 2>&1
goto :eof


REM ---------------------------------------------------------------------------
:waitport
REM %1 = port. Block until nothing is LISTENING on it, so the new process does
REM not lose a bind race with the one we just killed.
set "_p=%~1"
set /a _n=0
:waitport_loop
netstat -ano | findstr "LISTENING" | findstr ":%_p% " >nul 2>&1
if errorlevel 1 goto :eof
set /a _n+=1
if %_n% geq 15 (
  echo   WARNING: port %_p% is still in use - the new process may fail to start.
  goto :eof
)
call :sleep1
goto :waitport_loop


REM ---------------------------------------------------------------------------
:waitup
REM %1 = port, %2 = label. Report readiness instead of printing "Done" while the
REM app is still booting - the backend takes ~10-20s and the old script said
REM nothing, so a failed start looked identical to a slow one.
set "_p=%~1"
set "_label=%~2"
set /a _n=0
:waitup_loop
netstat -ano | findstr "LISTENING" | findstr ":%_p% " >nul 2>&1
if not errorlevel 1 (
  echo   %_label% ready on http://localhost:%_p%
  goto :eof
)
set /a _n+=1
if %_n% geq 90 (
  echo   WARNING: %_label% did not come up within 90s - check its window.
  goto :eof
)
call :sleep1
goto :waitup_loop

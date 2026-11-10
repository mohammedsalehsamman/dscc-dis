@echo off
rem Starts the SOAP server, the RMI server and the portal (in this order).
cd /d "%~dp0"
if not exist "dscc-portal\target\dscc-portal.jar" call build-all.bat || exit /b 1
echo Starting Employee SOAP server...
start "Employee SOAP Server" cmd /c run.bat soap-server
timeout /t 6 /nobreak >nul
echo Starting Library RMI server...
start "Library RMI Server" cmd /c run.bat rmi-server
timeout /t 4 /nobreak >nul
echo Starting the portal...
call run.bat portal

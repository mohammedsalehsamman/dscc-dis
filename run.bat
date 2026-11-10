@echo off
rem Usage: run.bat soap-server ^| rmi-server ^| portal ^| soap-client ^| rmi-client ^| soap-simple ^| rmi-simple   [extra args]
setlocal
cd /d "%~dp0"
set "ROOT=%CD%"
if not exist "dscc-portal\target\dscc-portal.jar" call build-all.bat || exit /b 1
set "EMP=%ROOT%\employee-soap\target\employee-soap.jar;%ROOT%\employee-soap\target\lib\*"
set "LIBR=%ROOT%\library-rmi\target\library-rmi.jar;%ROOT%\library-rmi\target\lib\*"
set "PORTAL=%ROOT%\dscc-portal\target\dscc-portal.jar;%ROOT%\dscc-portal\target\lib\*"
set TARGET=%1
shift
set ARGS=
:collect
if "%~1"=="" goto :run
set ARGS=%ARGS% %1
shift
goto :collect
:run
rem servers run inside their module folder, so their SQLite files live in <module>\data
if /i "%TARGET%"=="soap-server" (cd employee-soap & title Employee SOAP Server & java -cp "%EMP%" employee.server.EmployeePublisher %ARGS% & goto :eof)
if /i "%TARGET%"=="rmi-server"  (cd library-rmi & title Library RMI Server & java -cp "%LIBR%" library.server.LibraryServer %ARGS% & goto :eof)
if /i "%TARGET%"=="portal"      (start "" javaw -cp "%PORTAL%" dscc.portal.PortalApp & goto :eof)
if /i "%TARGET%"=="soap-client" (start "" javaw -cp "%EMP%" employee.client.ui.EmployeeClientApp & goto :eof)
if /i "%TARGET%"=="rmi-client"  (start "" javaw -cp "%LIBR%" library.client.ui.LibraryClientApp & goto :eof)
if /i "%TARGET%"=="soap-simple" (start "" javaw -cp "%EMP%" employee.client.EmployeeClientGUI & goto :eof)
if /i "%TARGET%"=="rmi-simple"  (start "" javaw -cp "%LIBR%" library.client.LibraryClientGUI & goto :eof)
echo Unknown target "%TARGET%"
echo Usage: run.bat soap-server ^| rmi-server ^| portal ^| soap-client ^| rmi-client ^| soap-simple ^| rmi-simple
pause

@echo off
rem Builds all four modules with the Maven Wrapper (downloads Maven on first use; needs JDK 17+).
rem Output: <module>\target\<module>.jar plus its libraries in <module>\target\lib
cd /d "%~dp0"
call mvnw.cmd -B -q package -DskipTests
if errorlevel 1 (
  echo.
  echo BUILD FAILED - is a JDK 17 or newer installed and JAVA_HOME / java on the PATH?
  pause
  exit /b 1
)
echo Build OK

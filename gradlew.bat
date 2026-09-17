@echo off
setlocal
if not "%GRADLE_HOME%"=="" (
  call "%GRADLE_HOME%\bin\gradle.bat" %*
  exit /b %errorlevel%
)
echo Gradle 9.6 is required. Set GRADLE_HOME or install Gradle in PATH.
exit /b 2

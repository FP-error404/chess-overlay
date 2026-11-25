@echo off
setlocal
set APP_HOME=%~dp0
set DIST_NAME=gradle-8.2.1
set GRADLE_USER_HOME_DIR=%USERPROFILE%\.gradle
if not "%GRADLE_USER_HOME%"=="" set GRADLE_USER_HOME_DIR=%GRADLE_USER_HOME%
set INSTALL_DIR=%GRADLE_USER_HOME_DIR%\wrapper\dists\chess-overlay\%DIST_NAME%
set GRADLE_BIN=%INSTALL_DIR%\%DIST_NAME%\bin\gradle.bat
if not exist "%GRADLE_BIN%" (
  if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
  echo Downloading %DIST_NAME% (first run only)...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/%DIST_NAME%-bin.zip' -OutFile '%INSTALL_DIR%\%DIST_NAME%.zip'; Expand-Archive -Force '%INSTALL_DIR%\%DIST_NAME%.zip' '%INSTALL_DIR%'; Remove-Item '%INSTALL_DIR%\%DIST_NAME%.zip'"
  if errorlevel 1 exit /b 1
)
call "%GRADLE_BIN%" -p "%APP_HOME%" %*
exit /b %ERRORLEVEL%

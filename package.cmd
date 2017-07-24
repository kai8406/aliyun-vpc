@echo off
echo [INFO] package jar to target.

cd %~dp0
call mvn clean package -Dmaven.test.skip=true
pause
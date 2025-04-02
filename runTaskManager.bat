@echo off
echo Checking Java installation...
java -version >nul 2>&1
if errorlevel 1 (
    echo Error: Java is not installed. Please install Java first.
    exit /b 1
)

echo Building and running the project...
call mvnw.cmd clean javafx:run
if errorlevel 1 (
    echo Error: Build or run failed. Please check the errors above.
    exit /b 1
) 
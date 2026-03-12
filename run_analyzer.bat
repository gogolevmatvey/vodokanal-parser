@echo off
cd /d %~dp0
echo Current directory: %CD%
echo Compiling...
javac -d target/classes src/main/java/org/example/AnalyzeValidRecords.java
if errorlevel 1 (
    echo Compilation failed!
    exit /b 1
)
echo Running analyzer...
java -cp target/classes org.example.AnalyzeValidRecords
echo Done!

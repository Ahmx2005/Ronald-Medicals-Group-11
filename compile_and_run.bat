@echo off
REM ----------------------------------------------------------------
REM  Ronald Medicals - compile and run helper (Group 11)
REM  Double-click this file, or run it from the command prompt.
REM  It compiles the program into a folder called bin\, then starts it.
REM  javac finds every other file automatically, because Main.java
REM  imports the modules and -sourcepath tells javac where src\ is.
REM ----------------------------------------------------------------

REM Always work from the folder this file is in, even if run from somewhere else.
cd /d "%~dp0"

REM Delete the old build folder first, so no out-of-date class files are left behind.
if exist bin rmdir /s /q bin
mkdir bin

javac -d bin -sourcepath src src\ug\ac\vu\g11\app\Main.java
if errorlevel 1 (
    echo.
    echo Compilation failed. Read the error messages above.
    pause
    exit /b 1
)

java -cp bin ug.ac.vu.g11.app.Main

@echo off
REM ScaffoldX Windows Launcher
REM Sets REPO_ROOT and executes ScaffoldX commands
REM 
REM Usage: scaffoldx <command> [args...]
REM Example: scaffoldx x-task-list

set SCAFFOLDX_REPO_ROOT={{REPO_ROOT}}
node "%SCAFFOLDX_REPO_ROOT%\.scaffoldx\scaffoldx.js" %*

# ScaffoldX Binary Directory

This directory contains executable scripts and utilities for ScaffoldX operations.

## Why These Files Are Here

When ScaffoldX is deployed to other projects, we want to keep the root directory clean. All ScaffoldX-specific executables and helper scripts should be contained within the `.scaffoldx` directory structure to avoid polluting the target project.

## Available Scripts

This directory is primarily for user utilities. Framework commands are located in `.scaffoldx/xcore/bin/`.

## Usage

For framework commands, use the commands in `.scaffoldx/xcore/bin/`:
```bash
# Windows
.scaffoldx\xcore\bin\x-task-create.bat

# Linux/Mac
./.scaffoldx/xcore/bin/x-task-create.sh
```

Or add `.scaffoldx\xcore\bin` to your PATH for easier access.

## Design Principle

ScaffoldX follows a "zero root pollution" principle. When you add ScaffoldX to a project, it should only create:
- The `.scaffoldx/` directory (all framework files)
- Optional: A single `scaffoldx.json` config file (if needed)

No batch files, scripts, or other files should be placed in the project root unless explicitly requested by the user.
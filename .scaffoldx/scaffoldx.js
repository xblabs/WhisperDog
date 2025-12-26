#!/usr/bin/env node

/**
 * ScaffoldX Command Launcher
 * 
 * This launcher ensures that all ScaffoldX commands respect the configured REPO_ROOT.
 * It sets the SCAFFOLDX_REPO_ROOT environment variable before executing commands.
 * 
 * Usage:
 *   node scaffoldx.js <command> [args...]
 *   
 * Example:
 *   node scaffoldx.js x-task-list
 *   node scaffoldx.js x-task-create "New Task" --priority high
 * 
 * @created 2025-06-11
 */

const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

// Get REPO_ROOT dynamically
const { getRepoRoot } = require("./xcore/scripts/utils/config-loader');
const REPO_ROOT = process.env.SCAFFOLDX_REPO_ROOT || getRepoRoot();

// Validate that REPO_ROOT exists and contains .scaffoldx
if (!fs.existsSync(path.join(REPO_ROOT, '.scaffoldx'))) {
  console.error(`[ERROR] Invalid REPO_ROOT: "${REPO_ROOT}"`);
  console.error(`[ERROR] The directory must contain a .scaffoldx folder.`);
  process.exit(1);
}

// Get command from arguments
const args = process.argv.slice(2);
if (args.length === 0) {
  console.log('ScaffoldX Command Launcher');
  console.log('');
  console.log('Usage: node scaffoldx.js <command> [args...]');
  console.log('');
  console.log('Example:');
  console.log('  node scaffoldx.js x-task-list');
  console.log('  node scaffoldx.js x-task-create "New Task"');
  console.log('');
  console.log(`Current REPO_ROOT: ${REPO_ROOT}`);
  process.exit(0);
}
const commandName = args[0];
const commandArgs = args.slice(1);

// Construct the script path
const scriptPath = path.join(REPO_ROOT, '.scaffoldx', 'xcore', 'scripts', `${commandName}.js`);

// Check if the script exists
if (!fs.existsSync(scriptPath)) {
  console.error(`[ERROR] Command script not found: ${scriptPath}`);
  console.error(`[ERROR] Make sure the command name is correct (e.g., x-task-list)`);
  process.exit(1);
}

console.log(`[INFO] Executing ${commandName} with REPO_ROOT: ${REPO_ROOT}`);

// Set up environment with SCAFFOLDX_REPO_ROOT
const env = {
  ...process.env,
  SCAFFOLDX_REPO_ROOT: REPO_ROOT
};

// Execute the command with the configured environment
const child = spawn('node', [scriptPath, ...commandArgs], {
  env: env,
  stdio: 'inherit',
  cwd: REPO_ROOT // Also set working directory to REPO_ROOT
});

// Handle exit
child.on('exit', (code) => {
  process.exit(code || 0);
});

// Handle errors
child.on('error', (err) => {
  console.error(`[ERROR] Failed to execute command: ${err.message}`);
  process.exit(1);
});

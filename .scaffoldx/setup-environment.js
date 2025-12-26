#!/usr/bin/env node

/**
 * ScaffoldX Environment Setup
 * 
 * This script helps configure the SCAFFOLDX_REPO_ROOT environment variable
 * to ensure all commands work correctly regardless of execution location.
 * 
 * @created 2025-06-11
 */

const fs = require('fs');
const path = require('path');
const os = require('os');
const { execSync } = require('child_process');

// Detect the current project root
const currentDir = process.cwd();
const scaffoldxPath = path.join(currentDir, '.scaffoldx');

if (!fs.existsSync(scaffoldxPath)) {
  console.error('[ERROR] This script must be run from the ScaffoldX project root.');
  console.error('[ERROR] No .scaffoldx directory found in:', currentDir);
  process.exit(1);
}

console.log('ScaffoldX Environment Setup');
console.log('===========================');
console.log('');
console.log('Detected Project Root:', currentDir);
console.log('');

// Platform-specific instructions
if (path.sep === path.sep) {
  console.log('Windows Setup Instructions:');
  console.log('');
  console.log('Option 1: Set for current session (temporary):');
  console.log(`  set SCAFFOLDX_REPO_ROOT=${currentDir}`);
  console.log('');
  console.log('Option 2: Set permanently (requires admin privileges):');
  console.log(`  setx SCAFFOLDX_REPO_ROOT "${currentDir}"`);
  console.log('');
  console.log('Option 3: Use the provided launcher (recommended):');
  console.log('  - Use: scaffoldx.bat <command> [args...]');
  console.log('  - Example: scaffoldx.bat x-task-list');
  console.log('');
  console.log('Option 4: Add to your PowerShell profile:');
  console.log(`  $env:SCAFFOLDX_REPO_ROOT = "${currentDir}"`);
} else {
  console.log('Unix/Linux/macOS Setup Instructions:');
  console.log('');
  console.log('Add to your shell profile (~/.bashrc, ~/.zshrc, etc.):');
  console.log(`  export SCAFFOLDX_REPO_ROOT="${currentDir}"`);
  console.log('');
  console.log('Then reload your shell:');
  console.log('  source ~/.bashrc  # or ~/.zshrc');
}

console.log('');
console.log('Testing the configuration...');

// Test if environment variable is already set
if (process.env.SCAFFOLDX_REPO_ROOT) {
  console.log(`✓ SCAFFOLDX_REPO_ROOT is already set to: ${process.env.SCAFFOLDX_REPO_ROOT}`);
  if (process.env.SCAFFOLDX_REPO_ROOT !== currentDir) {
    console.log(`⚠ WARNING: It points to a different directory than the current one.`);
  }
} else {
  console.log('✗ SCAFFOLDX_REPO_ROOT is not set in the current environment.');
}

console.log('');
console.log('Quick Test Commands:');
console.log('  node .scaffoldx/scaffoldx.js x-task-list');
console.log('  node .scaffoldx/scaffoldx.js x-system-prime');

---
title: ScaffoldX Function Call Translator for Claude
description: Utility for mapping Cursor operations to Claude MCP functions
context_type: adapter
priority: high
last_updated: 2025-05-02
---

# ScaffoldX Function Call Translator for Claude

This utility provides a standardized approach for translating Cursor operations to Claude's MCP functions. It enables seamless execution of ScaffoldX commands without requiring rewriting the underlying logic.

## Overview

The Function Call Translator is a crucial component of the ScaffoldX Cursor-to-Claude port, serving as a bridge between the original command implementations and Claude's MCP capabilities. It:

1. Identifies common patterns in Cursor command implementations
2. Maps these patterns to equivalent Claude MCP functions
3. Handles parameter transformations and adaptations
4. Provides a consistent interface for command execution

This approach maintains a single source of truth for commands while adapting to the execution requirements of each environment.

## Common Operation Patterns

The translator handles these common operation patterns found in ScaffoldX commands:

### File System Operations

| Cursor Operation | Claude MCP Equivalent |
|------------------|----------------------|
| `fs.readFileSync()` | `read_file()` |
| `fs.writeFileSync()` | `write_file()` |
| `fs.existsSync()` | `get_file_info()` (with try/catch) |
| `fs.mkdirSync()` | `create_directory()` |
| `fs.readdirSync()` | `list_directory()` |
| `fs.statSync()` | `get_file_info()` |
| `path.join()` | Path resolution utility |
| `path.dirname()` | Path resolution utility |
| `path.basename()` | Path resolution utility |

### Command Execution

| Cursor Operation | Claude MCP Equivalent |
|------------------|----------------------|
| `require('child_process').execSync()` | `execute_command()` |
| `require('child_process').exec()` | `execute_command()` (with timeout) |

### Package Integration

| Cursor Operation | Claude MCP Equivalent |
|------------------|----------------------|
| `require('fs')` | MCP file operations |
| `require('path')` | Path resolution utility |
| `require('child_process')` | MCP command execution |
| `require('yaml')` | Manual YAML parsing or string operations |

## Translation Functions

### translateFileRead(cursorPattern, resolvedPath)

Translates a Cursor file read operation to a Claude MCP function.

**Parameters:**
- `cursorPattern`: The matched Cursor operation pattern (string)
- `resolvedPath`: The resolved absolute path (string)

**Returns:**
- Translated Claude MCP function call (string)

**Usage Example:**
```javascript
const cursorCode = "const content = fs.readFileSync('.scaffoldx/xcontext/01_prd.md', 'utf8');";
const resolvedPath = "C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcontext\\01_prd.md";
const translated = translateFileRead(cursorCode, resolvedPath);
// Returns: "const content = await read_file({ path: 'C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcontext\\01_prd.md' });"
```

**Implementation:**
```javascript
function translateFileRead(cursorPattern, resolvedPath) {
  // Extract encoding if specified
  const encodingMatch = cursorPattern.match(/'(utf8|utf-8|ascii|binary)'/i);
  const encoding = encodingMatch ? encodingMatch[1].toLowerCase() : null;
  
  // Build the MCP function call
  let mcpCall = `await read_file({ path: '${resolvedPath}'`;
  
  // Add encoding if specified
  if (encoding) {
    mcpCall += `, encoding: '${encoding}'`;
  }
  
  mcpCall += " })";
  
  // Replace the Cursor pattern with the MCP call
  // This is a simplified approach - a real implementation would handle more edge cases
  return cursorPattern.replace(/fs\.readFileSync\([^)]+\)/, mcpCall);
}
```

### translateFileWrite(cursorPattern, resolvedPath)

Translates a Cursor file write operation to a Claude MCP function.

**Parameters:**
- `cursorPattern`: The matched Cursor operation pattern (string)
- `resolvedPath`: The resolved absolute path (string)

**Returns:**
- Translated Claude MCP function call (string)

**Usage Example:**
```javascript
const cursorCode = "fs.writeFileSync('.scaffoldx/xmemory/session.md', content, 'utf8');";
const resolvedPath = "C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xmemory\\session.md";
const translated = translateFileWrite(cursorCode, resolvedPath);
// Returns: "await write_file({ path: 'C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xmemory\\session.md', content: content });"
```

**Implementation:**
```javascript
function translateFileWrite(cursorPattern, resolvedPath) {
  // Extract the content variable reference
  const contentMatch = cursorPattern.match(/writeFileSync\([^,]+,\s*([^,)]+)/);
  const contentVar = contentMatch ? contentMatch[1].trim() : "''";
  
  // Build the MCP function call
  const mcpCall = `await write_file({ path: '${resolvedPath}', content: ${contentVar} })`;
  
  // Replace the Cursor pattern with the MCP call
  return cursorPattern.replace(/fs\.writeFileSync\([^)]+\)/, mcpCall);
}
```

### translateDirectoryCheck(cursorPattern, resolvedPath)

Translates a Cursor directory existence check to a Claude MCP function.

**Parameters:**
- `cursorPattern`: The matched Cursor operation pattern (string)
- `resolvedPath`: The resolved absolute path (string)

**Returns:**
- Translated Claude MCP function call (string)

**Usage Example:**
```javascript
const cursorCode = "if (!fs.existsSync('.scaffoldx/xmemory/session_summaries')) { /* ... */ }";
const resolvedPath = "C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xmemory\\session_summaries";
const translated = translateDirectoryCheck(cursorCode, resolvedPath);
// Returns: "let exists = false;\ntry {\n  await get_file_info({ path: 'C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xmemory\\session_summaries' });\n  exists = true;\n} catch (error) {\n  exists = false;\n}\nif (!exists) { /* ... */ }"
```

**Implementation:**
```javascript
function translateDirectoryCheck(cursorPattern, resolvedPath) {
  // Extract the condition (whether checking for existence or non-existence)
  const notExistsCheck = cursorPattern.includes('!fs.existsSync');
  
  // Build the MCP function call with try/catch
  const mcpCall = `let exists = false;
try {
  await get_file_info({ path: '${resolvedPath}' });
  exists = true;
} catch (error) {
  exists = false;
}
if (${notExistsCheck ? '!exists' : 'exists'})`;
  
  // Replace the Cursor pattern with the MCP call
  const regex = notExistsCheck ? /if\s*\(\s*!fs\.existsSync\([^)]+\)\s*\)/ : /if\s*\(\s*fs\.existsSync\([^)]+\)\s*\)/;
  return cursorPattern.replace(regex, mcpCall);
}
```

### translateDirectoryCreate(cursorPattern, resolvedPath)

Translates a Cursor directory creation operation to a Claude MCP function.

**Parameters:**
- `cursorPattern`: The matched Cursor operation pattern (string)
- `resolvedPath`: The resolved absolute path (string)

**Returns:**
- Translated Claude MCP function call (string)

**Usage Example:**
```javascript
const cursorCode = "fs.mkdirSync('.scaffoldx/xmemory/session_summaries', { recursive: true });";
const resolvedPath = "C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xmemory\\session_summaries";
const translated = translateDirectoryCreate(cursorCode, resolvedPath);
// Returns: "await create_directory({ path: 'C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xmemory\\session_summaries' });"
```

**Implementation:**
```javascript
function translateDirectoryCreate(cursorPattern, resolvedPath) {
  // Build the MCP function call
  const mcpCall = `await create_directory({ path: '${resolvedPath}' })`;
  
  // Replace the Cursor pattern with the MCP call
  return cursorPattern.replace(/fs\.mkdirSync\([^)]+\)/, mcpCall);
}
```

### translatePathJoin(cursorPattern, basePath)

Translates a Cursor path join operation to a Claude path utility.

**Parameters:**
- `cursorPattern`: The matched Cursor operation pattern (string)
- `basePath`: The base path for resolution (string)

**Returns:**
- Translated Claude path utility call (string)

**Usage Example:**
```javascript
const cursorCode = "const fullPath = path.join(__dirname, '.scaffoldx', 'xcontext', '01_prd.md');";
const basePath = "C:\\__dev\\_projects\\ScaffoldX";
const translated = translatePathJoin(cursorCode, basePath);
// Returns: "const fullPath = joinPaths('C:\\__dev\\_projects\\ScaffoldX', '.scaffoldx', 'xcontext', '01_prd.md');"
```

**Implementation:**
```javascript
function translatePathJoin(cursorPattern, basePath) {
  // Extract the path segments
  const joinMatch = cursorPattern.match(/path\.join\(([^)]+)\)/);
  if (!joinMatch) {
    return cursorPattern;
  }
  
  // Parse the segments
  const segmentsText = joinMatch[1];
  const segments = segmentsText.split(',').map(seg => seg.trim());
  
  // Handle __dirname replacement
  const processedSegments = segments.map(seg => {
    if (seg === '__dirname') {
      return `'${basePath}'`;
    }
    return seg;
  });
  
  // Build the joinPaths call
  const joinCall = `joinPaths(${processedSegments.join(', ')})`;
  
  // Replace the Cursor pattern with the utility call
  return cursorPattern.replace(/path\.join\([^)]+\)/, joinCall);
}
```

### translateCommandExecution(cursorPattern)

Translates a Cursor command execution operation to a Claude MCP function.

**Parameters:**
- `cursorPattern`: The matched Cursor operation pattern (string)

**Returns:**
- Translated Claude MCP function call (string)

**Usage Example:**
```javascript
const cursorCode = "const output = require('child_process').execSync('git log --since=\"1 day ago\"').toString();";
const translated = translateCommandExecution(cursorCode);
// Returns: "const output = await execute_command({ command: 'git log --since=\"1 day ago\"' });"
```

**Implementation:**
```javascript
function translateCommandExecution(cursorPattern) {
  // Extract the command
  const commandMatch = cursorPattern.match(/execSync\('([^']+)'/);
  if (!commandMatch) {
    return cursorPattern;
  }
  
  const command = commandMatch[1];
  
  // Build the MCP function call
  const mcpCall = `await execute_command({ command: '${command}' })`;
  
  // Replace the Cursor pattern with the MCP call
  return cursorPattern.replace(/require\('child_process'\)\.execSync\([^)]+\)(\.toString\(\))?/, mcpCall);
}
```

## Advanced Pattern Recognition

### identifyOperationPatterns(code)

Identifies operation patterns in code for translation.

**Parameters:**
- `code`: JavaScript code to analyze (string)

**Returns:**
- Array of identified operation patterns with metadata

**Usage Example:**
```javascript
const code = `
const fs = require('fs');
const path = require('path');

// Read the file
const content = fs.readFileSync('.scaffoldx/xcontext/01_prd.md', 'utf8');

// Create directory if it doesn't exist
if (!fs.existsSync('.scaffoldx/xmemory/session_summaries')) {
  fs.mkdirSync('.scaffoldx/xmemory/session_summaries', { recursive: true });
}

// Write the file
fs.writeFileSync(path.join('.scaffoldx/xmemory/session_summaries', '2025-05.md'), content);
`;

const patterns = identifyOperationPatterns(code);
```

**Implementation:**
```javascript
function identifyOperationPatterns(code) {
  if (!code) {
    return [];
  }
  
  const patterns = [];
  
  // Define pattern matchers
  const patternMatchers = [
    {
      type: 'fileRead',
      regex: /fs\.readFileSync\([^)]+\)/g,
      extractPath: (match) => {
        const pathMatch = match.match(/readFileSync\(['"]([^'"]+)['"]/);
        return pathMatch ? pathMatch[1] : null;
      }
    },
    {
      type: 'fileWrite',
      regex: /fs\.writeFileSync\([^)]+\)/g,
      extractPath: (match) => {
        const pathMatch = match.match(/writeFileSync\(['"]([^'"]+)['"]/);
        return pathMatch ? pathMatch[1] : null;
      }
    },
    {
      type: 'directoryCheck',
      regex: /(?:if\s*\(\s*!?fs\.existsSync\([^)]+\)\s*\))/g,
      extractPath: (match) => {
        const pathMatch = match.match(/existsSync\(['"]([^'"]+)['"]/);
        return pathMatch ? pathMatch[1] : null;
      }
    },
    {
      type: 'directoryCreate',
      regex: /fs\.mkdirSync\([^)]+\)/g,
      extractPath: (match) => {
        const pathMatch = match.match(/mkdirSync\(['"]([^'"]+)['"]/);
        return pathMatch ? pathMatch[1] : null;
      }
    },
    {
      type: 'pathJoin',
      regex: /path\.join\([^)]+\)/g,
      extractPath: (match) => {
        // This is a special case, return the full match for processing
        return match;
      }
    },
    {
      type: 'commandExecution',
      regex: /require\('child_process'\)\.execSync\([^)]+\)(\.toString\(\))?/g,
      extractPath: (match) => {
        // No path extraction needed for command execution
        return null;
      }
    }
  ];
  
  // Identify patterns
  for (const matcher of patternMatchers) {
    let match;
    while ((match = matcher.regex.exec(code)) !== null) {
      const fullMatch = match[0];
      const path = matcher.extractPath(fullMatch);
      
      patterns.push({
        type: matcher.type,
        pattern: fullMatch,
        path: path,
        index: match.index
      });
    }
  }
  
  // Sort patterns by their index in the code
  return patterns.sort((a, b) => a.index - b.index);
}
```

### translateCode(code, basePath = null)

Translates an entire code block from Cursor operations to Claude MCP functions.

**Parameters:**
- `code`: JavaScript code to translate (string)
- `basePath`: Base path for resolution (string, default: project root)

**Returns:**
- Translated code (string)

**Usage Example:**
```javascript
const cursorCode = `
// Read the PRD file
const prdContent = fs.readFileSync('.scaffoldx/xcontext/01_prd.md', 'utf8');

// Create directory for logs
if (!fs.existsSync('.scaffoldx/xmemory/session_summaries')) {
  fs.mkdirSync('.scaffoldx/xmemory/session_summaries', { recursive: true });
}

// Write the summary file
const summaryPath = path.join('.scaffoldx/xmemory/session_summaries', '2025-05.md');
fs.writeFileSync(summaryPath, summaryContent, 'utf8');
`;

const translatedCode = translateCode(cursorCode);
```

**Implementation:**
```javascript
function translateCode(code, basePath = null) {
  if (!code) {
    return code;
  }
  
  // Set default base path
  if (!basePath) {
    basePath = "C:\\__dev\\_projects\\ScaffoldX";
  }
  
  // Identify operation patterns
  const patterns = identifyOperationPatterns(code);
  
  // Start with the original code
  let translatedCode = code;
  
  // Apply translations in reverse order to avoid index shifts
  for (let i = patterns.length - 1; i >= 0; i--) {
    const pattern = patterns[i];
    const originalPattern = pattern.pattern;
    
    // Skip patterns without the necessary information
    if (!originalPattern) {
      continue;
    }
    
    // Resolve path if available
    let resolvedPath = null;
    if (pattern.path && typeof pattern.path === 'string' && !pattern.path.startsWith('path.join')) {
      resolvedPath = resolvePath(pattern.path, basePath);
    }
    
    // Apply the appropriate translation based on the pattern type
    let translatedPattern;
    switch (pattern.type) {
      case 'fileRead':
        translatedPattern = translateFileRead(originalPattern, resolvedPath);
        break;
      case 'fileWrite':
        translatedPattern = translateFileWrite(originalPattern, resolvedPath);
        break;
      case 'directoryCheck':
        translatedPattern = translateDirectoryCheck(originalPattern, resolvedPath);
        break;
      case 'directoryCreate':
        translatedPattern = translateDirectoryCreate(originalPattern, resolvedPath);
        break;
      case 'pathJoin':
        translatedPattern = translatePathJoin(originalPattern, basePath);
        break;
      case 'commandExecution':
        translatedPattern = translateCommandExecution(originalPattern);
        break;
      default:
        translatedPattern = originalPattern;
    }
    
    // Replace the pattern in the code
    if (translatedPattern !== originalPattern) {
      const patternIndex = translatedCode.indexOf(originalPattern);
      if (patternIndex !== -1) {
        translatedCode = translatedCode.substring(0, patternIndex) + 
                         translatedPattern + 
                         translatedCode.substring(patternIndex + originalPattern.length);
      }
    }
  }
  
  // Add MCP utility imports
  translatedCode = `// Using Claude MCP functions
${translatedCode}`;
  
  // Replace common require statements
  translatedCode = translatedCode.replace(/const\s+fs\s*=\s*require\(['"]fs['"]\);?/g, '// fs module replaced with MCP functions');
  translatedCode = translatedCode.replace(/const\s+path\s*=\s*require\(['"]path['"]\);?/g, '// path module replaced with MCP path utilities');
  translatedCode = translatedCode.replace(/const\s+\{\s*execSync\s*\}\s*=\s*require\(['"]child_process['"]\);?/g, '// child_process replaced with MCP execute_command');
  translatedCode = translatedCode.replace(/const\s+child_process\s*=\s*require\(['"]child_process['"]\);?/g, '// child_process replaced with MCP execute_command');
  
  // Add async wrapper if needed
  if (translatedCode.includes('await ')) {
    translatedCode = `async function executeCommand() {
${translatedCode}
}

executeCommand().catch(error => {
  console.error('Error executing command:', error);
});`;
  }
  
  return translatedCode;
}
```

## Command Execution Wrapper

The function call translator includes a wrapper for executing commands in Claude's environment.

### executeCommand(commandInfo, args)

Executes a command based on its definition and arguments.

**Parameters:**
- `commandInfo`: Parsed command information (object)
- `args`: Parsed command arguments (object)

**Returns:**
- Command execution result (string or object)

**Usage Example:**
```javascript
const result = await executeCommand(commandInfo, args);
```

**Implementation:**
```javascript
async function executeCommand(commandInfo, args) {
  try {
    // Extract behavior description from command info
    const behavior = commandInfo.behavior;
    
    // Analyze the behavior to determine the command's operations
    const operations = analyzeBehavior(behavior);
    
    // Execute each operation
    const results = {};
    
    for (const operation of operations) {
      switch (operation.type) {
        case 'fileRead':
          results[operation.name] = await read_file({ path: resolvePath(operation.path) });
          break;
        case 'fileWrite':
          await write_file({ 
            path: resolvePath(operation.path), 
            content: evaluateContent(operation.content, args, results) 
          });
          results[operation.name] = true;
          break;
        case 'directoryCheck':
          try {
            await get_file_info({ path: resolvePath(operation.path) });
            results[operation.name] = true;
          } catch (error) {
            results[operation.name] = false;
          }
          break;
        case 'directoryCreate':
          await create_directory({ path: resolvePath(operation.path) });
          results[operation.name] = true;
          break;
        case 'commandExecution':
          results[operation.name] = await execute_command({ command: operation.command });
          break;
        default:
          results[operation.name] = null;
      }
    }
    
    // Generate command output
    return generateCommandOutput(commandInfo, args, results);
  } catch (error) {
    return `Error executing command ${commandInfo.name}: ${error.message}`;
  }
}
```

## Integration with the Execution Adapter

The function call translator works seamlessly with the other components of the execution adapter layer:

- The **MDC Parser** extracts command information that's used for operation analysis
- The **Path Resolution Adapter** resolves paths before they're used in MCP functions
- The **Error Handling** system catches and manages problems during translation or execution
- The **State Persistence** system maintains context between operations

Together, these components enable faithful execution of commands in the Claude environment while leveraging the original ScaffoldX command definitions.

## Future Enhancements

1. **Advanced Code Analysis**: More sophisticated code analysis to handle complex patterns
2. **Custom Script Support**: Support for translating custom scripts in `.scaffoldx/xcustom/`
3. **Testing Framework**: Automated testing of translations against original implementations
4. **Graceful Degradation**: Fallback mechanisms for operations that can't be directly translated
5. **Performance Optimizations**: Caching and lazy loading for frequently used operations

This function call translator provides the foundation for executing ScaffoldX commands in Claude with minimal duplication of functionality.

---
title: ScaffoldX Error Handler for Claude
description: Error handling and fallback mechanisms for ScaffoldX in Claude
context_type: adapter
priority: high
last_updated: 2025-05-02
---

# ScaffoldX Error Handler for Claude

This utility provides a comprehensive error handling and fallback system for ScaffoldX in Claude. It ensures that commands execute reliably even when facing unexpected conditions or limitations.

## Overview

The Error Handler is an essential component of the ScaffoldX Cursor-to-Claude port, providing:

1. Standardized error handling for all command operations
2. Graceful fallback mechanisms when primary operations fail
3. User-friendly error messages with actionable information
4. Recovery strategies for common failure scenarios
5. Logging and reporting for error diagnostics

This system helps maintain a smooth user experience even when issues arise during command execution.

## Error Categories

The handler organizes errors into distinct categories for appropriate responses:

### 1. File System Errors

- **File Not Found**: When a requested file doesn't exist
- **Path Not Accessible**: When permissions prevent access
- **Directory Not Found**: When a required directory doesn't exist
- **Write Failed**: When file writing operations fail
- **Read Failed**: When file reading operations fail

### 2. Command Execution Errors

- **Command Not Found**: When a system command isn't available
- **Execution Failed**: When a command fails to execute properly
- **Timeout**: When a command takes too long to complete
- **Permission Denied**: When permissions prevent command execution

### 3. Parsing Errors

- **Invalid Command**: When a command can't be recognized
- **Invalid Arguments**: When command arguments are incorrect
- **Syntax Error**: When command syntax is invalid
- **Context Error**: When command requires unavailable context

### 4. MCP Errors

- **MCP Function Failed**: When an MCP function fails unexpectedly
- **MCP Limit Exceeded**: When reaching MCP usage limits
- **MCP Unavailable**: When MCP functionality is temporarily unavailable

## Error Handling Functions

### handleFileSystemError(error, operation, path)

Handles file system related errors with appropriate fallbacks.

**Parameters:**
- `error`: The error object (Error)
- `operation`: The operation that failed (string, e.g., 'read', 'write', 'create')
- `path`: The file path involved (string)

**Returns:**
- Error handling result with fallback information (object)

**Usage Example:**
```javascript
try {
  const content = await read_file({ path: filePath });
  return content;
} catch (error) {
  const result = handleFileSystemError(error, 'read', filePath);
  if (result.fallbackContent) {
    return result.fallbackContent;
  }
  throw new Error(result.userMessage);
}
```

**Implementation:**
```javascript
function handleFileSystemError(error, operation, path) {
  // Default result structure
  const result = {
    handled: false,
    userMessage: `Error during file operation: ${error.message}`,
    fallbackContent: null,
    recoverStrategy: null
  };
  
  // Normalize path for display
  const displayPath = path.replace(/\\/g, '/');
  
  // Handle based on operation type and error
  switch (operation) {
    case 'read':
      if (error.message.includes('ENOENT') || error.message.includes('not exist')) {
        result.handled = true;
        result.userMessage = `File not found: ${displayPath}`;
        result.fallbackContent = '';
        result.recoverStrategy = 'create';
      } else if (error.message.includes('EACCES') || error.message.includes('permission')) {
        result.handled = true;
        result.userMessage = `Permission denied when reading file: ${displayPath}`;
        result.recoverStrategy = 'elevate';
      }
      break;
    
    case 'write':
      if (error.message.includes('ENOENT') || error.message.includes('not exist')) {
        result.handled = true;
        result.userMessage = `Directory doesn't exist for file: ${displayPath}`;
        result.recoverStrategy = 'create_directory';
      } else if (error.message.includes('EACCES') || error.message.includes('permission')) {
        result.handled = true;
        result.userMessage = `Permission denied when writing file: ${displayPath}`;
        result.recoverStrategy = 'elevate';
      }
      break;
    
    case 'create_directory':
      if (error.message.includes('EEXIST')) {
        result.handled = true;
        result.userMessage = `Directory already exists: ${displayPath}`;
        result.fallbackContent = true; // Directory exists, so operation is essentially successful
        result.recoverStrategy = 'ignore';
      } else if (error.message.includes('EACCES') || error.message.includes('permission')) {
        result.handled = true;
        result.userMessage = `Permission denied when creating directory: ${displayPath}`;
        result.recoverStrategy = 'elevate';
      }
      break;
    
    case 'list_directory':
      if (error.message.includes('ENOENT') || error.message.includes('not exist')) {
        result.handled = true;
        result.userMessage = `Directory not found: ${displayPath}`;
        result.fallbackContent = [];
        result.recoverStrategy = 'create_directory';
      } else if (error.message.includes('EACCES') || error.message.includes('permission')) {
        result.handled = true;
        result.userMessage = `Permission denied when listing directory: ${displayPath}`;
        result.recoverStrategy = 'elevate';
      }
      break;
    
    case 'get_file_info':
      if (error.message.includes('ENOENT') || error.message.includes('not exist')) {
        result.handled = true;
        result.userMessage = `File or directory not found: ${displayPath}`;
        result.fallbackContent = null;
        result.recoverStrategy = 'check_parent';
      }
      break;
      
    default:
      // Generic file system error handling
      result.userMessage = `Error during ${operation} operation on ${displayPath}: ${error.message}`;
  }
  
  return result;
}
```

### handleCommandExecutionError(error, command)

Handles errors during command execution with appropriate fallbacks.

**Parameters:**
- `error`: The error object (Error)
- `command`: The command that failed (string)

**Returns:**
- Error handling result with fallback information (object)

**Usage Example:**
```javascript
try {
  const output = await execute_command({ command: 'git log --since="1 day ago"' });
  return output;
} catch (error) {
  const result = handleCommandExecutionError(error, 'git log --since="1 day ago"');
  if (result.fallbackOutput) {
    return result.fallbackOutput;
  }
  throw new Error(result.userMessage);
}
```

**Implementation:**
```javascript
function handleCommandExecutionError(error, command) {
  // Default result structure
  const result = {
    handled: false,
    userMessage: `Error executing command: ${error.message}`,
    fallbackOutput: null,
    recoverStrategy: null
  };
  
  // Extract the base command (first word)
  const baseCommand = command.split(' ')[0];
  
  // Handle based on command and error
  if (error.message.includes('not found') || error.message.includes('not recognized')) {
    result.handled = true;
    result.userMessage = `Command not found: ${baseCommand}`;
    result.recoverStrategy = 'suggest_alternative';
    
    // Suggest alternatives for common commands
    if (baseCommand === 'git') {
      result.userMessage += '. Please ensure Git is installed and in your PATH.';
    } else if (baseCommand === 'npm' || baseCommand === 'node') {
      result.userMessage += '. Please ensure Node.js is installed and in your PATH.';
    }
  } else if (error.message.includes('timeout') || error.message.includes('timed out')) {
    result.handled = true;
    result.userMessage = `Command timed out: ${command}`;
    result.recoverStrategy = 'retry_with_timeout';
  } else if (error.message.includes('permission') || error.message.includes('EACCES')) {
    result.handled = true;
    result.userMessage = `Permission denied when executing: ${command}`;
    result.recoverStrategy = 'elevate';
  } else if (error.message.includes('exit code')) {
    result.handled = true;
    const exitCode = error.message.match(/exit code (\d+)/);
    const code = exitCode ? exitCode[1] : 'non-zero';
    result.userMessage = `Command failed with exit code ${code}: ${command}`;
    
    // For git commands, provide more helpful messages
    if (baseCommand === 'git') {
      if (command.includes('log')) {
        result.fallbackOutput = 'No git history found for the specified time range.';
        result.recoverStrategy = 'expand_time_range';
      } else if (command.includes('status')) {
        result.fallbackOutput = 'No git repository found in the current directory.';
      }
    }
  }
  
  return result;
}
```

### handleParsingError(error, commandInput)

Handles errors during command parsing with suggestions.

**Parameters:**
- `error`: The error object (Error)
- `commandInput`: The original command input (string)

**Returns:**
- Error handling result with suggestions (object)

**Usage Example:**
```javascript
try {
  const args = parseCommandArgs(input, commandInfo);
  return executeCommand(commandInfo, args);
} catch (error) {
  const result = handleParsingError(error, input);
  return result.userMessage;
}
```

**Implementation:**
```javascript
function handleParsingError(error, commandInput) {
  // Default result structure
  const result = {
    handled: false,
    userMessage: `Error parsing command: ${error.message}`,
    suggestions: [],
    correctedInput: null
  };
  
  // Extract command name (if possible)
  const commandMatch = commandInput.match(/^(x-[\w-]+)/);
  const commandName = commandMatch ? commandMatch[1] : null;
  
  // Handle based on error type
  if (error.message.includes('not found') || error.message.includes('unknown')) {
    result.handled = true;
    result.userMessage = `Command not found: ${commandName || commandInput}`;
    
    // Suggest similar commands
    if (commandName) {
      // This would typically use a more sophisticated matching algorithm
      if (commandName === 'x-task') {
        result.suggestions.push('x-task-create', 'x-task-list', 'x-task-complete');
        result.userMessage += '\nDid you mean one of these: x-task-create, x-task-list, x-task-complete?';
      } else if (commandName.includes('summar')) {
        result.suggestions.push('x-session-summarize');
        result.userMessage += '\nDid you mean: x-session-summarize (alias: x-sum)?';
      }
    }
  } else if (error.message.includes('invalid argument') || error.message.includes('missing argument')) {
    result.handled = true;
    
    // Extract argument name if available
    const argMatch = error.message.match(/argument '([^']+)'/);
    const argName = argMatch ? argMatch[1] : null;
    
    if (argName) {
      result.userMessage = `Invalid or missing argument '${argName}' for command ${commandName}`;
      
      // Suggest correct usage for common arguments
      if (argName === 'month') {
        result.suggestions.push('--month=YYYY-MM');
        result.userMessage += '\nUse format: --month=YYYY-MM (e.g., --month=2025-05)';
      } else if (argName === 'task-id') {
        result.suggestions.push('<task-id>');
        result.userMessage += '\nPlease provide a valid task ID as the first argument';
      }
    } else {
      result.userMessage = `Invalid argument for command ${commandName || commandInput}`;
    }
    
    // Suggest using help
    result.userMessage += `\nTry '${commandName} --help' for usage information.`;
  } else if (error.message.includes('syntax')) {
    result.handled = true;
    result.userMessage = `Syntax error in command: ${commandInput}`;
    
    // If quotes are unbalanced, suggest correction
    if ((commandInput.match(/"/g) || []).length % 2 !== 0) {
      result.userMessage += '\nUnbalanced quotes detected. Make sure all quotes are properly closed.';
      
      // Attempt to correct by adding missing quote
      result.correctedInput = commandInput + '"';
      result.suggestions.push(result.correctedInput);
    }
  }
  
  return result;
}
```

### handleMcpError(error, functionName, params)

Handles errors specific to MCP function calls.

**Parameters:**
- `error`: The error object (Error)
- `functionName`: The MCP function that failed (string)
- `params`: The parameters passed to the function (object)

**Returns:**
- Error handling result with recovery options (object)

**Usage Example:**
```javascript
try {
  const result = await read_file({ path: filePath });
  return result;
} catch (error) {
  const result = handleMcpError(error, 'read_file', { path: filePath });
  return result.userMessage;
}
```

**Implementation:**
```javascript
function handleMcpError(error, functionName, params) {
  // Default result structure
  const result = {
    handled: false,
    userMessage: `Error in MCP function ${functionName}: ${error.message}`,
    recoveryOptions: [],
    alternativeApproach: null
  };
  
  // Handle based on function and error
  if (error.message.includes('limit') || error.message.includes('quota')) {
    result.handled = true;
    result.userMessage = `MCP limit exceeded for function ${functionName}`;
    
    // Suggest alternative approaches
    if (functionName === 'search_code' || functionName === 'search_files') {
      result.alternativeApproach = 'narrow_search';
      result.recoveryOptions.push('Narrow search scope', 'Reduce result count', 'Split into smaller searches');
    } else if (functionName === 'execute_command') {
      result.alternativeApproach = 'limit_output';
      result.recoveryOptions.push('Add output limiting to the command', 'Use a shorter time range');
    }
  } else if (error.message.includes('unavailable') || error.message.includes('not accessible')) {
    result.handled = true;
    result.userMessage = `MCP function ${functionName} is currently unavailable`;
    
    // Suggest manual alternatives
    if (functionName === 'read_file' || functionName === 'write_file') {
      result.alternativeApproach = 'manual_operation';
      result.recoveryOptions.push('Try manually checking the file', 'Check file permissions');
    } else if (functionName === 'execute_command') {
      result.alternativeApproach = 'manual_command';
      result.recoveryOptions.push('Try running the command manually in a terminal');
    }
  } else if (error.message.includes('invalid') || error.message.includes('malformed')) {
    result.handled = true;
    result.userMessage = `Invalid parameters for MCP function ${functionName}`;
    
    // Log the problematic parameters
    if (params) {
      result.userMessage += `\nParameters: ${JSON.stringify(params)}`;
    }
  }
  
  return result;
}
```

## Fallback Mechanisms

The error handler includes automatic fallback mechanisms to recover from common errors.

### attemptFallback(error, operation, params)

Attempts to recover from an error using appropriate fallback strategies.

**Parameters:**
- `error`: The error object (Error)
- `operation`: The operation that failed (string)
- `params`: The parameters used in the operation (object)

**Returns:**
- Fallback result with recovery information (object)

**Usage Example:**
```javascript
try {
  const content = await read_file({ path: filePath });
  return content;
} catch (error) {
  const fallback = await attemptFallback(error, 'read_file', { path: filePath });
  if (fallback.success) {
    return fallback.result;
  }
  throw new Error(fallback.message);
}
```

**Implementation:**
```javascript
async function attemptFallback(error, operation, params) {
  // Default result structure
  const fallbackResult = {
    success: false,
    message: `Fallback failed for operation ${operation}: ${error.message}`,
    result: null,
    actionsPerformed: []
  };
  
  // Handle file system operations
  if (['read_file', 'write_file', 'create_directory', 'list_directory', 'get_file_info'].includes(operation)) {
    // Get appropriate handler
    const fsError = handleFileSystemError(error, operation, params.path);
    
    if (fsError.handled) {
      // Try fallback based on recover strategy
      switch (fsError.recoverStrategy) {
        case 'create':
          // File doesn't exist but we can create a default version
          if (operation === 'read_file') {
            // For certain files, create a default template
            if (params.path.includes('session_summaries')) {
              const summaryTemplate = `# Session Summary\n\nSession started: ${new Date().toISOString()}\n\n## Activities\n\n- Session initialization\n`;
              
              try {
                // Ensure directory exists
                const dirPath = params.path.substring(0, params.path.lastIndexOf('\\'));
                await create_directory({ path: dirPath });
                
                // Create default file
                await write_file({ path: params.path, content: summaryTemplate });
                
                fallbackResult.success = true;
                fallbackResult.result = summaryTemplate;
                fallbackResult.message = `Created default template for: ${params.path}`;
                fallbackResult.actionsPerformed.push('create_directory', 'write_file');
              } catch (fallbackError) {
                fallbackResult.message = `Fallback failed: ${fallbackError.message}`;
              }
            }
          }
          break;
          
        case 'create_directory':
          // Directory doesn't exist but we can create it
          try {
            const dirPath = params.path.substring(0, params.path.lastIndexOf('\\'));
            await create_directory({ path: dirPath });
            
            // Try the original operation again
            if (operation === 'write_file') {
              await write_file(params);
              fallbackResult.success = true;
              fallbackResult.message = `Created directory and wrote file: ${params.path}`;
              fallbackResult.actionsPerformed.push('create_directory', 'write_file');
            }
          } catch (fallbackError) {
            fallbackResult.message = `Fallback failed: ${fallbackError.message}`;
          }
          break;
          
        case 'ignore':
          // Operation can be safely ignored
          fallbackResult.success = true;
          fallbackResult.result = fsError.fallbackContent;
          fallbackResult.message = fsError.userMessage;
          break;
          
        case 'check_parent':
          // Check if parent directory exists
          try {
            const dirPath = params.path.substring(0, params.path.lastIndexOf('\\'));
            await get_file_info({ path: dirPath });
            
            fallbackResult.success = true;
            fallbackResult.result = null;
            fallbackResult.message = `Parent directory exists, but file not found: ${params.path}`;
          } catch (fallbackError) {
            fallbackResult.message = `Parent directory not found: ${dirPath}`;
          }
          break;
      }
    }
  }
  
  // Handle command execution operations
  else if (operation === 'execute_command') {
    // Get appropriate handler
    const cmdError = handleCommandExecutionError(error, params.command);
    
    if (cmdError.handled) {
      // Try fallback based on recover strategy
      switch (cmdError.recoverStrategy) {
        case 'retry_with_timeout':
          // Try again with increased timeout
          try {
            const result = await execute_command({
              ...params,
              timeout_ms: (params.timeout_ms || 30000) * 2
            });
            
            fallbackResult.success = true;
            fallbackResult.result = result;
            fallbackResult.message = `Command succeeded with increased timeout: ${params.command}`;
            fallbackResult.actionsPerformed.push('retry_with_timeout');
          } catch (fallbackError) {
            fallbackResult.message = `Retry failed: ${fallbackError.message}`;
          }
          break;
          
        case 'expand_time_range':
          // For git commands, try with expanded time range
          if (params.command.startsWith('git log')) {
            try {
              // Modify time range
              const expandedCommand = params.command.replace(
                /(--since=")([^"]+)(")/,
                (match, p1, timeRange, p3) => {
                  // Double the time range
                  if (timeRange.includes('day')) {
                    const days = parseInt(timeRange) || 1;
                    return `${p1}${days * 2} days ago${p3}`;
                  }
                  return `${p1}7 days ago${p3}`;
                }
              );
              
              const result = await execute_command({ ...params, command: expandedCommand });
              
              fallbackResult.success = true;
              fallbackResult.result = result || cmdError.fallbackOutput;
              fallbackResult.message = `Command succeeded with expanded time range: ${expandedCommand}`;
              fallbackResult.actionsPerformed.push('expand_time_range');
            } catch (fallbackError) {
              fallbackResult.message = `Retry failed: ${fallbackError.message}`;
            }
          }
          break;
      }
    }
    
    // Return fallback output if available
    if (cmdError.fallbackOutput && !fallbackResult.success) {
      fallbackResult.success = true;
      fallbackResult.result = cmdError.fallbackOutput;
      fallbackResult.message = cmdError.userMessage;
    }
  }
  
  return fallbackResult;
}
```

## Error Logging and Reporting

### logError(error, context)

Logs error information for diagnostics and reporting.

**Parameters:**
- `error`: The error object (Error)
- `context`: Additional context about the error (object)

**Returns:**
- Logged error ID (string)

**Usage Example:**
```javascript
try {
  const result = await executeCommand(commandInfo, args);
  return result;
} catch (error) {
  const errorId = logError(error, { command: commandInfo.name, args: args });
  return `Error executing command (ID: ${errorId}). ${error.message}`;
}
```

**Implementation:**
```javascript
async function logError(error, context) {
  // Generate unique error ID
  const errorId = `ERR-${Date.now().toString(36)}-${Math.floor(Math.random() * 10000).toString(36)}`;
  
  // Format error for logging
  const errorEntry = {
    id: errorId,
    timestamp: new Date().toISOString(),
    message: error.message,
    stack: error.stack,
    context: context || {}
  };
  
  // Format as string for logging
  const errorLog = `## Error: ${errorId}
**Timestamp:** ${errorEntry.timestamp}
**Message:** ${errorEntry.message}
**Context:** ${JSON.stringify(errorEntry.context, null, 2)}
**Stack:** ${errorEntry.stack || 'No stack trace available'}
`;

  try {
    // Ensure log directory exists
    const logDir = "C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xmemory\\errors";
    await create_directory({ path: logDir });
    
    // Write to error log file
    const logPath = `${logDir}\\errors_${new Date().toISOString().split('T')[0].replace(/-/g, '')}.md`;
    
    // Check if log file exists
    let existingContent = '';
    try {
      existingContent = await read_file({ path: logPath });
    } catch (readError) {
      // File doesn't exist yet, start fresh
      existingContent = '# ScaffoldX Error Log\n\n';
    }
    
    // Append new error
    const updatedContent = existingContent + '\n' + errorLog + '\n---\n';
    await write_file({ path: logPath, content: updatedContent });
  } catch (logError) {
    // If logging itself fails, just return the ID
    console.error(`Failed to log error: ${logError.message}`);
  }
  
  return errorId;
}
```

## Integration with the Execution Adapter

The error handler works seamlessly with the other components of the execution adapter layer:

- The **MDC Parser** uses the error handler to manage parsing errors
- The **Path Resolution Adapter** benefits from fallbacks for invalid paths
- The **Function Call Translator** uses error handling for operation failures
- The **State Persistence** system recovers from state corruption or loss

Together, these components ensure robust command execution in the Claude environment while providing graceful recovery from errors.

## Usage in Command Execution

The error handler is designed to be integrated into the command execution flow:

```javascript
async function executeScaffoldXCommand(commandName, args) {
  try {
    // Parse command
    const commandInfo = await parseMdcCommand(`C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcore\\commands\\${commandName}.md`);
    if (!commandInfo) {
      throw new Error(`Command not found: ${commandName}`);
    }
    
    // Parse arguments
    const parsedArgs = parseCommandArgs(args, commandInfo);
    
    // Execute command
    return await executeCommand(commandInfo, parsedArgs);
  } catch (error) {
    // Handle different error types
    if (error.message.includes('not found')) {
      const parseResult = handleParsingError(error, `${commandName} ${args}`);
      return parseResult.userMessage;
    } else if (error.phase === 'execution') {
      // Log the error
      const errorId = await logError(error, { command: commandName, args: args });
      
      // Try fallback
      const fallback = await attemptFallback(error, error.operation, error.params);
      if (fallback.success) {
        return `Recovered from error: ${fallback.message}`;
      }
      
      return `Error executing command (ID: ${errorId}): ${error.message}`;
    }
    
    // Generic error
    return `Error: ${error.message}`;
  }
}
```

This error handling approach ensures that ScaffoldX commands execute as reliably as possible, with graceful fallbacks and clear error messages when issues occur.

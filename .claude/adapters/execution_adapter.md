---
title: ScaffoldX Execution Adapter for Claude
description: Main execution adapter that integrates all components for ScaffoldX in Claude
context_type: adapter
priority: high
last_updated: 2025-05-02
---

# ScaffoldX Execution Adapter for Claude

This document provides the core execution adapter that integrates all components for ScaffoldX in Claude. It serves as the central system for command execution, enabling ScaffoldX to work seamlessly in the Claude environment.

## Overview

The Execution Adapter is the cornerstone of the ScaffoldX Cursor-to-Claude port, providing:

1. **Unified Interface**: A consistent interface for command execution
2. **Component Integration**: Integration of all adapter components
3. **Command Flow**: Orchestration of the command execution process
4. **Seamless Execution**: Transparent execution of commands without requiring duplication
5. **Enhanced User Experience**: Improved interaction with additional context-awareness

This adapter allows ScaffoldX to maintain a single source of truth for commands while adapting to Claude's environment-specific execution requirements.

## Adapter Components

The Execution Adapter integrates several specialized components:

- **MDC Parser**: Parses original MDC command files
- **Path Resolver**: Converts relative paths to absolute paths
- **Function Call Translator**: Maps Cursor operations to Claude MCP functions
- **Error Handler**: Provides error handling and fallback mechanisms
- **State Persistence**: Maintains context between conversations

Together, these components form a comprehensive adapter layer that bridges the gap between Cursor commands and Claude execution.

## Execution Flow

The execution adapter implements a structured flow for command processing:

1. **Command Recognition**: Identifies commands in user input
2. **Command Loading**: Loads command definition from original MDC files
3. **Command Parsing**: Extracts parameters and arguments
4. **Path Resolution**: Resolves relative paths to absolute paths
5. **Function Translation**: Maps Cursor operations to Claude MCP functions
6. **Command Execution**: Executes the command with error handling
7. **Result Processing**: Processes and formats command output
8. **State Persistence**: Updates state for continuity

This flow ensures reliable and consistent command execution across the entire ScaffoldX framework.

## Core Execution Functions

### executeScaffoldXCommand(input)

Main entry point for executing ScaffoldX commands.

**Parameters:**
- `input`: User input containing the command and arguments (string)

**Returns:**
- Command execution result (string or object)

**Usage Example:**
```javascript
const result = await executeScaffoldXCommand('x-session-summarize --month 2025-05');
```

**Implementation:**
```javascript
async function executeScaffoldXCommand(input) {
  try {
    // Extract command name
    const commandName = extractCommandNameFromInput(input);
    if (!commandName) {
      throw new Error('Invalid command format. Commands must start with "x-".');
    }
    
    // Start session if not already started
    const session = await startSession();
    
    // Load command definition
    const commandInfo = await loadCommandDefinition(commandName);
    if (!commandInfo) {
      throw new Error(`Command not found: ${commandName}`);
    }
    
    // Parse command arguments
    const args = parseCommandArgs(input, commandInfo);
    
    // Log command execution start
    await logSessionActivity('command_execution_start', { 
      command: commandName, 
      args: args 
    });
    
    // Execute command
    const result = await executeCommandWithDefinition(commandInfo, args, session);
    
    // Record command execution
    await recordCommandExecution(commandName, args, result);
    
    // Log command execution end
    await logSessionActivity('command_execution_end', { 
      command: commandName, 
      success: true 
    });
    
    return result;
  } catch (error) {
    // Handle error based on type
    if (error.phase === 'parsing') {
      const parseError = handleParsingError(error, input);
      return parseError.userMessage;
    } else if (error.phase === 'execution') {
      // Try fallback if possible
      try {
        const fallback = await attemptFallback(error, error.operation, error.params);
        if (fallback.success) {
          return `Recovered from error: ${fallback.message}`;
        }
      } catch (fallbackError) {
        // Fallback itself failed, continue to error logging
      }
      
      // Log error
      const errorId = await logError(error, { command: input });
      
      return `Error executing command (ID: ${errorId}): ${error.message}`;
    }
    
    // Generic error handling
    console.error(`Error executing command: ${error.message}`);
    return `Error: ${error.message}`;
  }
}
```

### loadCommandDefinition(commandName)

Loads a command definition from the original MDC file.

**Parameters:**
- `commandName`: Name of the command to load (string)

**Returns:**
- Parsed command information (object) or null if not found

**Usage Example:**
```javascript
const commandInfo = await loadCommandDefinition('x-session-summarize');
```

**Implementation:**
```javascript
async function loadCommandDefinition(commandName) {
  if (!commandName) {
    return null;
  }
  
  try {
    // Check for command alias lookups in commands index
    const commandsIndexPath = "C:\\__dev\\_projects\\ScaffoldX\\.claude\\_x_commands.md";
    let primaryCommandName = commandName;
    
    try {
      const indexContent = await read_file({ path: commandsIndexPath });
      
      // Look for the command in the index
      const commandPattern = new RegExp(`## \`${commandName}\`.*?\\n- Process command definition: \\.scaffoldx\\/xcore\\/commands\\/([\\w-]+)\\.md`, 'i');
      const match = indexContent.match(commandPattern);
      
      if (match) {
        primaryCommandName = match[1];
      }
    } catch (indexError) {
      // Unable to check index, use the provided command name
      console.error(`Error loading commands index: ${indexError.message}`);
    }
    
    // Construct path to command definition
    const commandPath = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcore\\commands\\${primaryCommandName}.md`;
    
    // Check if file exists
    try {
      await get_file_info({ path: commandPath });
    } catch (error) {
      return null;
    }
    
    // Parse command definition
    const commandInfo = await parseMdcCommand(commandPath);
    
    // Resolve paths in command definition
    if (commandInfo) {
      return resolveCommandPaths(commandInfo);
    }
    
    return null;
  } catch (error) {
    console.error(`Error loading command definition: ${error.message}`);
    return null;
  }
}
```

### executeCommandWithDefinition(commandInfo, args, session)

Executes a command based on its definition, arguments, and session context.

**Parameters:**
- `commandInfo`: Parsed command information (object)
- `args`: Parsed command arguments (object)
- `session`: Current session context (object)

**Returns:**
- Command execution result (string or object)

**Usage Example:**
```javascript
const result = await executeCommandWithDefinition(commandInfo, args, session);
```

**Implementation:**
```javascript
async function executeCommandWithDefinition(commandInfo, args, session) {
  if (!commandInfo) {
    throw new Error('Command definition not provided');
  }
  
  try {
    // Determine command behavior based on definition
    const operationPlan = analyzeCommandBehavior(commandInfo, args);
    
    // Apply context from session
    if (session) {
      // Add current task context if needed
      if (operationPlan.requiresTaskContext && session.currentTask) {
        operationPlan.context.currentTask = session.currentTask;
      }
      
      // Add current role context if needed
      if (operationPlan.requiresRoleContext && session.currentRole) {
        operationPlan.context.currentRole = session.currentRole;
      }
    }
    
    // Execute operations in sequence
    const results = {};
    
    for (const operation of operationPlan.operations) {
      const result = await executeOperation(operation, operationPlan.context);
      results[operation.id] = result;
      
      // Update context with operation result
      operationPlan.context[operation.id] = result;
    }
    
    // Process results based on command output format
    return formatCommandResults(commandInfo, results, operationPlan.context);
  } catch (error) {
    // Add execution phase information for better error handling
    error.phase = 'execution';
    throw error;
  }
}
```

### analyzeCommandBehavior(commandInfo, args)

Analyzes a command's behavior to determine operations for execution.

**Parameters:**
- `commandInfo`: Parsed command information (object)
- `args`: Parsed command arguments (object)

**Returns:**
- Operation plan with ordered operations and context (object)

**Usage Example:**
```javascript
const operationPlan = analyzeCommandBehavior(commandInfo, args);
```

**Implementation:**
```javascript
function analyzeCommandBehavior(commandInfo, args) {
  // Initialize operation plan
  const operationPlan = {
    operations: [],
    context: {
      ...args,
      commandName: commandInfo.name
    },
    requiresTaskContext: false,
    requiresRoleContext: false
  };
  
  // Extract behavior description
  const behavior = commandInfo.behavior || '';
  
  // Check for task context requirement
  if (behavior.includes('current task') || behavior.includes('task-specific')) {
    operationPlan.requiresTaskContext = true;
  }
  
  // Check for role context requirement
  if (behavior.includes('role perspective') || behavior.includes('role-based')) {
    operationPlan.requiresRoleContext = true;
  }
  
  // Identify operations based on behavior patterns
  
  // File reading operations
  const readPatterns = behavior.match(/reads?\s+([^.]+)\.md/gi) || [];
  for (let i = 0; i < readPatterns.length; i++) {
    const match = readPatterns[i].match(/reads?\s+([^.]+)\.md/i);
    if (match) {
      const fileDesc = match[1].trim();
      const opId = `read_${fileDesc.replace(/\s+/g, '_')}`;
      
      // Determine file path based on description
      let filePath;
      if (fileDesc.includes('session summaries')) {
        filePath = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xmemory\\session_summaries\\${args.month || getCurrentMonth()}.md`;
      } else if (fileDesc.includes('task')) {
        filePath = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${args.taskId || operationPlan.context.currentTask}\\task-${args.taskId || operationPlan.context.currentTask}.md`;
      } else {
        // Generic file in context
        filePath = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcontext\\${fileDesc}.md`;
      }
      
      operationPlan.operations.push({
        id: opId,
        type: 'read_file',
        path: filePath,
        description: `Read ${fileDesc} file`
      });
    }
  }
  
  // Directory creation operations
  const dirPatterns = behavior.match(/creates?\s+directory\s+([^.]+)/gi) || [];
  for (let i = 0; i < dirPatterns.length; i++) {
    const match = dirPatterns[i].match(/creates?\s+directory\s+([^.]+)/i);
    if (match) {
      const dirDesc = match[1].trim();
      const opId = `create_${dirDesc.replace(/\s+/g, '_')}_directory`;
      
      // Determine directory path based on description
      let dirPath;
      if (dirDesc.includes('session summaries')) {
        dirPath = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xmemory\\session_summaries`;
      } else if (dirDesc.includes('task')) {
        dirPath = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${args.taskId || operationPlan.context.currentTask}`;
      } else {
        // Generic directory in context
        dirPath = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\${dirDesc}`;
      }
      
      operationPlan.operations.push({
        id: opId,
        type: 'create_directory',
        path: dirPath,
        description: `Create ${dirDesc} directory`
      });
    }
  }
  
  // File writing operations
  const writePatterns = behavior.match(/writes?\s+to\s+([^.]+)\.md/gi) || [];
  for (let i = 0; i < writePatterns.length; i++) {
    const match = writePatterns[i].match(/writes?\s+to\s+([^.]+)\.md/i);
    if (match) {
      const fileDesc = match[1].trim();
      const opId = `write_${fileDesc.replace(/\s+/g, '_')}`;
      
      // Determine file path based on description
      let filePath;
      if (fileDesc.includes('session summary')) {
        filePath = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xmemory\\session_summaries\\${args.month || getCurrentMonth()}.md`;
      } else if (fileDesc.includes('task')) {
        filePath = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${args.taskId || operationPlan.context.currentTask}\\task-${args.taskId || operationPlan.context.currentTask}.md`;
      } else {
        // Generic file in context
        filePath = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcontext\\${fileDesc}.md`;
      }
      
      operationPlan.operations.push({
        id: opId,
        type: 'write_file',
        path: filePath,
        contentFromArgs: args.content || null,
        description: `Write to ${fileDesc} file`
      });
    }
  }
  
  // Command execution operations
  const cmdPatterns = behavior.match(/executes?\s+command\s+['"]([^'"]+)['"]/gi) || [];
  for (let i = 0; i < cmdPatterns.length; i++) {
    const match = cmdPatterns[i].match(/executes?\s+command\s+['"]([^'"]+)['"]/i);
    if (match) {
      const cmdText = match[1].trim();
      const opId = `execute_${i + 1}`;
      
      operationPlan.operations.push({
        id: opId,
        type: 'execute_command',
        command: cmdText,
        description: `Execute command: ${cmdText}`
      });
    }
  }
  
  // Special command-specific operations
  if (commandInfo.name === 'x-session-summarize') {
    // Add operations specific to session summarize command
    const month = args.month || getCurrentMonth();
    const customText = args.custom || '';
    
    // Directory check and creation
    operationPlan.operations.unshift({
      id: 'check_summary_dir',
      type: 'create_directory',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xmemory\\session_summaries`,
      description: 'Ensure summary directory exists'
    });
    
    // Git log if not mentioned yet
    if (!cmdPatterns.length) {
      operationPlan.operations.push({
        id: 'get_git_log',
        type: 'execute_command',
        command: 'git log --since="1 day ago" --pretty=format:"%h - %s" --no-merges',
        description: 'Get recent git history'
      });
    }
    
    // Generate summary
    operationPlan.operations.push({
      id: 'generate_summary',
      type: 'generate_content',
      template: 'session_summary',
      inputs: {
        month: month,
        customText: customText,
        gitLogRef: 'get_git_log'
      },
      description: 'Generate session summary content'
    });
  }
  
  return operationPlan;
}
```

### executeOperation(operation, context)

Executes a single operation based on its type and parameters.

**Parameters:**
- `operation`: Operation to execute (object)
- `context`: Execution context with variables (object)

**Returns:**
- Operation result (string, object, or boolean)

**Usage Example:**
```javascript
const result = await executeOperation({
  id: 'read_prd',
  type: 'read_file',
  path: 'C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcontext\\01_prd.md'
}, context);
```

**Implementation:**
```javascript
async function executeOperation(operation, context) {
  if (!operation || !operation.type) {
    throw new Error('Invalid operation');
  }
  
  try {
    switch (operation.type) {
      case 'read_file':
        return await read_file({ path: operation.path });
      
      case 'write_file':
        // Determine content to write
        let content = '';
        
        if (operation.contentFromArgs) {
          // Content provided directly in args
          content = operation.contentFromArgs;
        } else if (operation.contentFromOperation) {
          // Content from previous operation
          content = context[operation.contentFromOperation];
        } else if (operation.contentTemplate) {
          // Content from template
          content = applyTemplate(operation.contentTemplate, context);
        }
        
        await write_file({ path: operation.path, content: content });
        return true;
      
      case 'create_directory':
        await create_directory({ path: operation.path });
        return true;
      
      case 'execute_command':
        return await execute_command({ command: operation.command });
      
      case 'generate_content':
        // Use template to generate content
        let templateContent = '';
        
        if (operation.template === 'session_summary') {
          const gitLog = context[operation.inputs.gitLogRef] || 'No recent git activity';
          const customText = operation.inputs.customText || '';
          
          templateContent = `# Session Summary: ${new Date().toISOString().split('T')[0]}

## Git Activity

${gitLog}

${customText ? `\n## Custom Notes\n\n${customText}\n` : ''}

## Key Insights

- Progress made on current tasks
- Documentation updated
- Core functionality improved

---

`;
        }
        
        return templateContent;
      
      default:
        throw new Error(`Unsupported operation type: ${operation.type}`);
    }
  } catch (error) {
    // Add operation context to error for better handling
    error.operation = operation.type;
    error.params = {
      path: operation.path,
      command: operation.command
    };
    throw error;
  }
}
```

### formatCommandResults(commandInfo, results, context)

Formats the results of command execution based on the command's expected output format.

**Parameters:**
- `commandInfo`: Parsed command information (object)
- `results`: Operation results keyed by operation ID (object)
- `context`: Execution context with variables (object)

**Returns:**
- Formatted command output (string)

**Usage Example:**
```javascript
const formattedResult = formatCommandResults(commandInfo, results, context);
```

**Implementation:**
```javascript
function formatCommandResults(commandInfo, results, context) {
  // Default to simple output format
  let output = `Command ${commandInfo.name} executed successfully.`;
  
  // Check for explicit examples in command definition for output format guidance
  if (commandInfo.examples && commandInfo.examples.length > 0) {
    // Use the first example as guidance
    const example = commandInfo.examples[0];
    
    if (example.output && example.output.includes('summary created in')) {
      // Command likely outputs path information
      const relevantResult = findRelevantResult(results, 'write_');
      if (relevantResult && relevantResult.path) {
        output = `Session summary created in ${relevantResult.path}`;
      }
    }
  }
  
  // Special case for specific commands
  if (commandInfo.name === 'x-session-summarize') {
    // Check if we have a generated summary
    if (results.generate_summary) {
      output = `Session summary created for ${context.month || getCurrentMonth()}:\n\n`;
      
      // Add verbose output if requested
      if (context.verbose) {
        output += results.generate_summary;
      } else {
        output += `Summary written to .scaffoldx/xmemory/session_summaries/${context.month || getCurrentMonth()}.md`;
      }
    }
  } else if (commandInfo.name === 'x-task-create') {
    output = `Task "${context.taskName || 'New task'}" created successfully.`;
    
    if (results.write_task) {
      output += `\nTask saved to ${results.write_task.path || '.scaffoldx/xtasks/'}`;
    }
  }
  
  return output;
}

// Helper function to find relevant result by key prefix
function findRelevantResult(results, prefix) {
  for (const [key, value] of Object.entries(results)) {
    if (key.startsWith(prefix)) {
      return {
        key: key,
        value: value,
        path: key.includes('write_') ? getPathFromContext(key, value) : null
      };
    }
  }
  return null;
}

// Helper to extract path from context based on operation key
function getPathFromContext(key, value) {
  if (typeof value === 'object' && value.path) {
    return value.path;
  }
  
  // Default path based on operation key
  if (key.includes('session_summary')) {
    return '.scaffoldx/xmemory/session_summaries/[month].md';
  } else if (key.includes('task')) {
    return '.scaffoldx/xtasks/[task-id]/task-[task-id].md';
  }
  
  return null;
}

// Helper to get current month in YYYY-MM format
function getCurrentMonth() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}
```

## Integration with Claude's System

The execution adapter integrates with Claude's main system through hooks in the master rule files.

### Command Recognition Hook

When a user inputs a command starting with `x-`, the system intercepts it:

```javascript
// In __X_main.md
// If prompt starts with x-, process as command
if (input.startsWith('x-')) {
  return await executeScaffoldXCommand(input);
}
```

### Session Management Hook

The system maintains session state across conversations:

```javascript
// In __X_main.md
// When conversation starts
async function onConversationStart() {
  const session = await startSession();
  return session;
}

// When conversation ends
async function onConversationEnd(session) {
  await endSession(session);
}
```

### Command Help Hook

The system provides intelligent help for commands:

```javascript
// In __X_main.md
// If prompt contains command help pattern
if (input.match(/^x-[\w-]+ --help$/) || input.match(/^help x-[\w-]+$/)) {
  const commandName = extractCommandNameFromInput(input);
  return await getCommandHelp(commandName);
}

async function getCommandHelp(commandName) {
  const commandInfo = await loadCommandDefinition(commandName);
  if (!commandInfo) {
    return `Command ${commandName} not found.`;
  }
  
  // Format help information from command definition
  let help = `# ${commandInfo.name}`;
  if (commandInfo.alias) {
    help += ` (alias: ${commandInfo.alias})`;
  }
  
  help += `\n\n${commandInfo.description}\n\n## Usage\n\n`;
  
  // Add parameters
  if (commandInfo.parameters && commandInfo.parameters.length > 0) {
    help += '### Parameters\n\n';
    for (const param of commandInfo.parameters) {
      help += `- \`${param.name}\`: ${param.description}\n`;
    }
    help += '\n';
  }
  
  // Add flags
  if (commandInfo.flags && commandInfo.flags.length > 0) {
    help += '### Flags\n\n';
    for (const flag of commandInfo.flags) {
      help += `- \`--${flag.name}`;
      if (flag.valueFormat) {
        help += ` ${flag.valueFormat}`;
      }
      help += `\`: ${flag.description}\n`;
    }
    help += '\n';
  }
  
  // Add examples
  if (commandInfo.examples && commandInfo.examples.length > 0) {
    help += '## Examples\n\n';
    for (let i = 0; i < commandInfo.examples.length; i++) {
      const example = commandInfo.examples[i];
      help += `### Example ${i + 1}:\n\n`;
      help += `\`${example.input}\`\n\n${example.output}\n\n`;
    }
  }
  
  return help;
}
```

## Command Execution Example

To illustrate the full flow of the execution adapter, here's an example of executing the `x-session-summarize` command:

```javascript
// User input
const input = 'x-session-summarize --month 2025-05 --custom "Implemented execution adapter for Claude"';

// 1. Command recognition
// Extract command name: 'x-session-summarize'

// 2. Load command definition from MDC file
// Parse the .scaffoldx/xcore/commands/x-session-summarize.md file

// 3. Parse command arguments
// Extract --month=2025-05 and --custom="Implemented execution adapter for Claude"

// 4. Analyze command behavior
// Determine required operations: create directory, read existing summary, generate content, write summary

// 5. Execute operations
// - Create directory: .scaffoldx/xmemory/session_summaries/
// - Get git activity
// - Generate summary content
// - Write summary to file

// 6. Format results
// Return success message with summary path

// Output
"Session summary created for 2025-05. Summary written to .scaffoldx/xmemory/session_summaries/2025-05.md"
```

## Benefits of the Execution Adapter

This execution adapter provides several advantages:

1. **Single Source of Truth**: Maintains original MDC files as the definitive command source
2. **Environment Adaptation**: Adapts execution to Claude's specific requirements
3. **Enhanced Reliability**: Provides robust error handling and fallback mechanisms
4. **Context Awareness**: Leverages session context for improved execution
5. **Seamless Experience**: Creates a consistent user experience across environments

By implementing this adapter layer, ScaffoldX can maintain its markdown-driven philosophy while taking advantage of Claude's unique capabilities.

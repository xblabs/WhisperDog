---
title: Central Command Processor for ScaffoldX Claude Adapter
description: Centralized functions for processing all command types through a single adapter
context_type: adapter
priority: high
last_updated: 2025-05-02
---

# Central Command Processor

This document contains the centralized functions for processing all ScaffoldX commands through a single adapter layer. It follows the correct pattern of maintaining the original MDC command files as the single source of truth while providing environment-specific execution logic for Claude.

## Core Command Processing

### processCommand(commandName, args)

Main entry point for processing any ScaffoldX command.

```javascript
async function processCommand(commandName, args) {
  try {
    // Normalize command name (remove 'x-' prefix if needed)
    const normalizedName = commandName.startsWith('x-') 
      ? commandName 
      : `x-${commandName}`;
    
    // Load original command definition
    const commandDef = await loadCommandDefinition(normalizedName);
    
    if (!commandDef) {
      return `Command not found: ${normalizedName}`;
    }
    
    // Parse command arguments
    const parsedArgs = parseCommandArgs(args, commandDef);
    
    // Check for help flag
    if (parsedArgs.help || parsedArgs.h) {
      return generateHelpText(commandDef);
    }
    
    // Analyze command behavior
    const operations = analyzeCommandBehavior(commandDef, parsedArgs);
    
    // Execute operations
    return await executeOperations(operations);
  } catch (error) {
    return `Error processing command: ${error.message}`;
  }
}
```

### loadCommandDefinition(commandName)

Loads and parses the original command definition from the MDC file.

```javascript
async function loadCommandDefinition(commandName) {
  try {
    // Construct path to the command definition
    const commandPath = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcore\\commands\\${commandName}.md`;
    
    // Check if file exists
    if (!await fileExists(commandPath)) {
      // Try searching for command with alias
      const aliasCommand = await findCommandByAlias(commandName);
      if (aliasCommand) {
        return await loadCommandDefinition(aliasCommand);
      }
      return null;
    }
    
    // Read command definition
    const content = await readFile(commandPath);
    
    // Parse frontmatter and content
    const { frontmatter, content: commandContent } = parseFrontmatter(content);
    
    // Extract key components
    const description = frontmatter.description || '';
    const version = frontmatter.version || '1.0.0';
    const priority = frontmatter.priority || 'medium';
    
    // Extract behavior section
    const behaviorMatch = commandContent.match(/\*\*Behavior\*\*:([\s\S]*?)(?:\n\n\*\*|\n\n##|$)/);
    const behavior = behaviorMatch ? behaviorMatch[1].trim() : '';
    
    // Extract parsing section
    const parsingMatch = commandContent.match(/\*\*Parsing\*\*:([\s\S]*?)(?:\n\n\*\*|\n\n##|$)/);
    const parsing = parsingMatch ? parsingMatch[1].trim() : '';
    
    // Extract examples
    const examplesMatch = commandContent.match(/\*\*Example\*\*:([\s\S]*?)(?:\n\n\*\*|\n\n##|$)/);
    const examples = examplesMatch ? examplesMatch[1].trim() : '';
    
    // Extract alias if present
    const aliasMatch = commandContent.match(/\(alias:?\s+[`"]?([^`".)]+)[`"]?\)/i);
    const alias = aliasMatch ? aliasMatch[1].trim() : null;
    
    return {
      name: commandName,
      description,
      version,
      priority,
      behavior,
      parsing,
      examples,
      alias,
      content: commandContent
    };
  } catch (error) {
    console.error(`Error loading command definition: ${error.message}`);
    return null;
  }
}
```

### findCommandByAlias(alias)

Finds a command by its alias.

```javascript
async function findCommandByAlias(alias) {
  try {
    // Normalize alias (remove x- prefix if present)
    const normalizedAlias = alias.startsWith('x-') ? alias.substring(2) : alias;
    
    // Load the command index file
    const indexPath = "C:\\__dev\\_projects\\ScaffoldX\\.claude\\_x_commands.md";
    const indexContent = await readFile(indexPath);
    
    // Search for alias pattern
    const aliasPattern = new RegExp(`## \`x-([\\w-]+)\`\\s*\\(alias:\\s*\`x-${normalizedAlias}\`|\\`${normalizedAlias}\\`\\)`, 'i');
    const match = indexContent.match(aliasPattern);
    
    if (match && match[1]) {
      return `x-${match[1]}`;
    }
    
    return null;
  } catch (error) {
    console.error(`Error finding command by alias: ${error.message}`);
    return null;
  }
}
```

### parseCommandArgs(args, commandDef)

Parses command arguments based on the command definition.

```javascript
function parseCommandArgs(args, commandDef) {
  // If args is already an object, just return it
  if (typeof args === 'object' && !Array.isArray(args)) {
    return args;
  }
  
  // If args is a string, parse it
  if (typeof args === 'string') {
    return parseCommandString(args, commandDef);
  }
  
  // Default to empty object if no args
  return {};
}

function parseCommandString(argsString, commandDef) {
  const result = {
    _: []
  };
  
  // Split string on spaces, but respect quotes
  const parts = argsString.match(/(?:[^\s"']+|"[^"]*"|'[^']*')+/g) || [];
  
  // Process each part
  let currentFlag = null;
  
  for (const part of parts) {
    // Check if it's a flag
    if (part.startsWith('--')) {
      const flagName = part.substring(2);
      
      // Check if it's a boolean flag
      if (flagName.includes('=')) {
        const [name, value] = flagName.split('=');
        result[name] = value.replace(/^["']|["']$/g, '');
      } else {
        result[flagName] = true;
        currentFlag = flagName;
      }
    } 
    // Check if it's a value for the previous flag
    else if (currentFlag) {
      result[currentFlag] = part.replace(/^["']|["']$/g, '');
      currentFlag = null;
    } 
    // Otherwise it's a positional argument
    else {
      result._.push(part.replace(/^["']|["']$/g, ''));
    }
  }
  
  return result;
}
```

### analyzeCommandBehavior(commandDef, args)

Analyzes the command behavior to determine operations.

```javascript
function analyzeCommandBehavior(commandDef, args) {
  const operations = [];
  const behavior = commandDef.behavior || '';
  
  // Check for command-specific behaviors using pattern matching
  
  // 1. File reading operations
  const readMatches = behavior.match(/reads?\s+([^.]+)\.[\w]+/gi) || [];
  for (const match of readMatches) {
    const fileDesc = match.replace(/reads?\s+|\.[\w]+/gi, '').trim();
    const operation = createReadOperation(fileDesc, args);
    if (operation) {
      operations.push(operation);
    }
  }
  
  // 2. File writing operations
  const writeMatches = behavior.match(/writes?\s+(?:to\s+)?([^.]+)\.[\w]+/gi) || [];
  for (const match of writeMatches) {
    const fileDesc = match.replace(/writes?\s+(?:to\s+)?|\.[\w]+/gi, '').trim();
    const operation = createWriteOperation(fileDesc, args, commandDef);
    if (operation) {
      operations.push(operation);
    }
  }
  
  // 3. Directory operations
  const dirMatches = behavior.match(/creates?\s+(?:a\s+)?directory\s+(.+?)(?:\.|;|\n|$)/gi) || [];
  for (const match of dirMatches) {
    const dirDesc = match.replace(/creates?\s+(?:a\s+)?directory\s+/gi, '').trim();
    const operation = createDirectoryOperation(dirDesc, args);
    if (operation) {
      operations.push(operation);
    }
  }
  
  // 4. Command execution operations
  const cmdMatches = behavior.match(/executes?\s+(?:the\s+)?command\s+["'](.+?)["']/gi) || [];
  for (const match of cmdMatches) {
    const cmdText = match.replace(/executes?\s+(?:the\s+)?command\s+["']/gi, '').replace(/["']$/, '').trim();
    operations.push({
      type: 'execute_command',
      command: cmdText,
      description: `Execute command: ${cmdText}`
    });
  }
  
  // Handle specific command patterns
  const commandHandlers = {
    'x-command-list': handleCommandListBehavior,
    'x-task-list': handleTaskListBehavior,
    'x-task-create': handleTaskCreateBehavior,
    'x-task-complete': handleTaskCompleteBehavior,
    'x-feedback-cycle': handleFeedbackCycleBehavior
    // Add more handlers as needed
  };
  
  const handler = commandHandlers[commandDef.name];
  if (handler) {
    const specialOperations = handler(args, behavior);
    operations.push(...specialOperations);
  }
  
  return operations;
}
```

### executeOperations(operations)

Executes operations determined from the command behavior.

```javascript
async function executeOperations(operations) {
  const results = {};
  
  for (let i = 0; i < operations.length; i++) {
    const operation = operations[i];
    const result = await executeOperation(operation);
    
    // Store result with operation ID or index
    const resultId = operation.id || `op_${i}`;
    results[resultId] = result;
    
    // Update context for later operations
    if (resultId && i < operations.length - 1) {
      for (let j = i + 1; j < operations.length; j++) {
        updateOperationContext(operations[j], resultId, result);
      }
    }
  }
  
  // Format the final output based on results
  return formatResults(operations, results);
}
```

### executeOperation(operation)

Executes a single operation.

```javascript
async function executeOperation(operation) {
  try {
    switch (operation.type) {
      case 'read_file':
        return await readFile(operation.path);
        
      case 'write_file':
        await writeFile(operation.path, operation.content);
        return { success: true, path: operation.path };
        
      case 'create_directory':
        await createDirectory(operation.path);
        return { success: true, path: operation.path };
        
      case 'execute_command':
        return await executeCommand(operation.command);
        
      case 'parse_json':
        return JSON.parse(operation.content);
        
      case 'generate_content':
        return generateContent(operation.template, operation.data);
        
      default:
        throw new Error(`Unknown operation type: ${operation.type}`);
    }
  } catch (error) {
    console.error(`Error executing operation: ${error.message}`);
    return { error: error.message };
  }
}
```

## Command-Specific Handlers

These handlers analyze behaviors for specific commands.

```javascript
function handleCommandListBehavior(args) {
  const operations = [];
  
  // Add operations for listing commands
  operations.push({
    id: 'list_core_commands',
    type: 'search_files',
    path: 'C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcore\\commands',
    pattern: 'x-*.md',
    description: 'List core commands'
  });
  
  // Check for custom commands
  operations.push({
    id: 'check_custom_commands',
    type: 'file_exists',
    path: 'C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcustom\\commands',
    description: 'Check for custom commands directory'
  });
  
  // Generate formatted output
  operations.push({
    id: 'format_output',
    type: 'generate_content',
    template: 'command_list',
    data: {
      detailed: args.detailed || false,
      category: args.category || null,
      format: args.format || 'markdown',
      search: args.search || null,
      commands: '${list_core_commands}',
      customCommands: '${check_custom_commands}'
    },
    description: 'Format command list output'
  });
  
  return operations;
}

function handleTaskListBehavior(args) {
  const operations = [];
  
  // Add operation to read tasks.json
  operations.push({
    id: 'read_tasks',
    type: 'read_file',
    path: 'C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\tasks.json',
    description: 'Read tasks.json'
  });
  
  // Add operation to parse tasks.json
  operations.push({
    id: 'parse_tasks',
    type: 'parse_json',
    content: '${read_tasks}',
    description: 'Parse tasks.json'
  });
  
  // Add operation to format task list
  operations.push({
    id: 'format_tasks',
    type: 'generate_content',
    template: 'task_list',
    data: {
      tasks: '${parse_tasks}',
      status: args.status || null,
      priority: args.priority || null,
      search: args.search || null
    },
    description: 'Format task list'
  });
  
  return operations;
}

// Add more command-specific handlers as needed
```

## Helper Functions

Utility functions to support the main processing.

```javascript
// Create a read operation based on file description
function createReadOperation(fileDesc, args) {
  let path;
  
  // Determine path based on file description
  if (fileDesc.includes('tasks.json')) {
    path = 'C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\tasks.json';
  } else if (fileDesc.includes('session')) {
    const month = args.month || getCurrentMonth();
    path = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xmemory\\session_summaries\\${month}.md`;
  } else if (fileDesc.includes('task')) {
    const taskId = args.taskId || args._[0];
    path = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${taskId}\\task-${taskId}.md`;
  } else {
    // Default to a file in xcontext
    path = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcontext\\${fileDesc}.md`;
  }
  
  return {
    type: 'read_file',
    path,
    description: `Read ${fileDesc} file`
  };
}

// Create a write operation based on file description
function createWriteOperation(fileDesc, args, commandDef) {
  let path;
  let content;
  
  // Determine path based on file description
  if (fileDesc.includes('tasks.json')) {
    path = 'C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\tasks.json';
    content = args.content || '';
  } else if (fileDesc.includes('session')) {
    const month = args.month || getCurrentMonth();
    path = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xmemory\\session_summaries\\${month}.md`;
    content = generateSessionSummary(args, month);
  } else if (fileDesc.includes('task')) {
    const taskId = args.taskId || args._[0];
    path = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${taskId}\\task-${taskId}.md`;
    content = args.content || '';
  } else {
    // Default to a file in xcontext
    path = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcontext\\${fileDesc}.md`;
    content = args.content || '';
  }
  
  return {
    type: 'write_file',
    path,
    content,
    description: `Write to ${fileDesc} file`
  };
}

// Create a directory operation based on directory description
function createDirectoryOperation(dirDesc, args) {
  let path;
  
  // Determine path based on directory description
  if (dirDesc.includes('session')) {
    path = 'C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xmemory\\session_summaries';
  } else if (dirDesc.includes('task')) {
    const taskId = args.taskId || args._[0];
    path = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${taskId}`;
  } else if (dirDesc.includes('feedback')) {
    path = 'C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xworkbench\\feedback_cycle';
  } else {
    // Default to a directory in scaffoldx
    path = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\${dirDesc}`;
  }
  
  return {
    type: 'create_directory',
    path,
    description: `Create ${dirDesc} directory`
  };
}

// Update operation context with results from previous operations
function updateOperationContext(operation, resultId, result) {
  // Check for string interpolation in various fields
  for (const key in operation) {
    if (typeof operation[key] === 'string' && operation[key].includes('${' + resultId + '}')) {
      operation[key] = operation[key].replace('${' + resultId + '}', JSON.stringify(result));
    } else if (key === 'data' && typeof operation[key] === 'object') {
      // Handle data object separately
      for (const dataKey in operation[key]) {
        if (typeof operation[key][dataKey] === 'string' && operation[key][dataKey].includes('${' + resultId + '}')) {
          operation[key][dataKey] = operation[key][dataKey].replace('${' + resultId + '}', JSON.stringify(result));
        }
      }
    }
  }
}

// Format results into a user-friendly output
function formatResults(operations, results) {
  // The last operation's result is typically the final output
  const lastOp = operations[operations.length - 1];
  const lastResult = results[lastOp.id || `op_${operations.length - 1}`];
  
  if (lastOp.type === 'generate_content') {
    // Return generated content directly
    return lastResult;
  } else if (typeof lastResult === 'string') {
    // Return string results directly
    return lastResult;
  } else if (lastResult && lastResult.error) {
    // Handle errors
    return `Error: ${lastResult.error}`;
  } else if (lastResult && lastResult.success) {
    // Handle success messages
    return `Operation completed successfully: ${lastOp.description}`;
  } else {
    // Default format
    return `Command executed successfully.`;
  }
}

// Get current month in YYYY-MM format
function getCurrentMonth() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

// Generate session summary
function generateSessionSummary(args, month) {
  const now = new Date().toISOString().split('T')[0];
  const customText = args.custom || '';
  
  return `# Session Summary: ${now}

## Summary
Session summary for ${month}

${customText ? `\n## Custom Notes\n\n${customText}\n` : ''}

## Key Insights
- Progress made on current tasks
- Documentation updated
- Core functionality improved

---
`;
}

// Generate help text
function generateHelpText(commandDef) {
  let help = `# ${commandDef.name}`;
  
  if (commandDef.alias) {
    help += ` (alias: ${commandDef.alias})`;
  }
  
  help += `\n\n${commandDef.description}\n\n`;
  
  if (commandDef.parsing) {
    help += `## Usage\n\n${commandDef.parsing}\n\n`;
  }
  
  if (commandDef.examples) {
    help += `## Examples\n\n${commandDef.examples}\n\n`;
  }
  
  return help;
}
```

## Integration with Master Rule Files

To integrate this central command processor with the master rule files:

```javascript
// In __X_main.md
// When a user inputs a command starting with x-
if (input.startsWith('x-')) {
  const commandName = input.split(' ')[0];
  const args = input.substring(commandName.length).trim();
  return await processCommand(commandName, args);
}
```

This centralized approach ensures we maintain a single adapter layer that interprets the original command definitions, avoiding the N+1 problem while providing environment-specific execution for Claude.

---
title: Enhanced Command Processor for ScaffoldX Claude Adapter
description: Comprehensive centralized processor for all ScaffoldX commands through a single adapter interface
context_type: adapter
priority: high
last_updated: 2025-05-02
---

# Enhanced Command Processor

This document contains the enhanced centralized processor for all ScaffoldX commands through a single adapter interface. It strictly follows the single-adapter pattern specified in `.claude/IMPLEMENTATION_PATTERN.md`, maintaining the original MDC command files as the single source of truth while providing environment-specific execution logic for Claude.

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
    const operationPlan = analyzeCommandBehavior(commandDef, parsedArgs);
    
    // Execute operations
    const results = await executeOperations(operationPlan.operations, operationPlan.context);
    
    // Format and return results
    return formatResults(commandDef, results, operationPlan.context);
  } catch (error) {
    console.error(`Error processing command: ${error.message}`);
    return `Error processing command: ${error.message}`;
  }
}
```

### loadCommandDefinition(commandName)

Loads and parses the original command definition from the MDC file.

```javascript
async function loadCommandDefinition(commandName) {
  try {
    // Normalize command name (remove 'x-' prefix if present for lookup)
    const searchName = commandName.startsWith('x-') ? commandName : `x-${commandName}`;
    
    // Construct path to the command definition
    const commandPath = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcore\\commands\\${searchName}.md`;
    
    // Check if file exists
    try {
      await get_file_info({ path: commandPath });
    } catch (error) {
      // Try searching for command with alias
      const aliasCommand = await findCommandByAlias(searchName);
      if (aliasCommand) {
        return await loadCommandDefinition(aliasCommand);
      }
      return null;
    }
    
    // Read command definition
    const content = await read_file({ path: commandPath });
    
    // Parse frontmatter and content
    const parsedCommand = parseMdcFile(content);
    
    return {
      name: searchName,
      path: commandPath,
      ...parsedCommand
    };
  } catch (error) {
    console.error(`Error loading command definition: ${error.message}`);
    return null;
  }
}
```

### parseMdcFile(content)

Parses an MDC file content into structured data.

```javascript
function parseMdcFile(content) {
  // Initialize result object
  const result = {
    frontmatter: {},
    title: '',
    description: '',
    parsing: '',
    behavior: '',
    examples: [],
    aliases: [],
    flags: []
  };
  
  // Extract frontmatter
  const frontmatterMatch = content.match(/^---\s*\n([\s\S]*?)\n---\s*\n/);
  if (frontmatterMatch) {
    const frontmatterBlock = frontmatterMatch[1];
    const frontmatterLines = frontmatterBlock.split('\n');
    
    for (const line of frontmatterLines) {
      if (line.trim() && line.includes(':')) {
        const [key, ...valueParts] = line.split(':');
        const value = valueParts.join(':').trim();
        result.frontmatter[key.trim()] = value;
      }
    }
    
    // Extract key properties from frontmatter
    result.description = result.frontmatter.description || '';
    result.version = result.frontmatter.version || '1.0.0';
    result.priority = result.frontmatter.priority || 'medium';
  }
  
  // Extract title (command name)
  const titleMatch = content.match(/^#\s+(.*?)(?:\s*\(.*\))?\s*$/m);
  if (titleMatch) {
    result.title = titleMatch[1].trim();
    
    // Check for aliases in title
    const aliasMatch = titleMatch[0].match(/\((?:alias(?:es)?:)?\s*([^)]+)\)/i);
    if (aliasMatch) {
      const aliases = aliasMatch[1].split(',').map(a => a.trim().replace(/[`"']/g, ''));
      result.aliases = aliases;
    }
  }
  
  // Extract parsing section
  const parsingMatch = content.match(/\*\*Parsing\*\*:([\s\S]*?)(?:\n\n\*\*|\n\n##|$)/);
  if (parsingMatch) {
    result.parsing = parsingMatch[1].trim();
    
    // Extract flags from parsing section
    const flagPattern = /--([\w-]+)(?:\s+(\S+))?\s*:\s*(.*?)(?:\n|$)/g;
    let flagMatch;
    
    while ((flagMatch = flagPattern.exec(parsingMatch[1])) !== null) {
      result.flags.push({
        name: flagMatch[1],
        valueFormat: flagMatch[2] || null,
        description: flagMatch[3].trim()
      });
    }
  }
  
  // Extract behavior section
  const behaviorMatch = content.match(/\*\*Behavior\*\*:([\s\S]*?)(?:\n\n\*\*|\n\n##|$)/);
  if (behaviorMatch) {
    result.behavior = behaviorMatch[1].trim();
  }
  
  // Extract examples
  const examplesSection = content.match(/\*\*Examples?\*\*:([\s\S]*?)(?:\n\n\*\*|\n\n##|$)/);
  if (examplesSection) {
    const examplesText = examplesSection[1].trim();
    
    // Extract individual examples
    const examples = examplesText.split(/\n\n(?=`)/);
    
    for (const example of examples) {
      const inputMatch = example.match(/`([^`]+)`/);
      if (inputMatch) {
        // Rest of the text after the input is the output/explanation
        const output = example.substring(inputMatch[0].length).trim();
        
        result.examples.push({
          input: inputMatch[1],
          output: output
        });
      }
    }
  }
  
  return result;
}
```

### findCommandByAlias(alias)

Searches for a command by its alias.

```javascript
async function findCommandByAlias(alias) {
  try {
    // Normalize alias (ensure x- prefix for searching)
    const searchAlias = alias.startsWith('x-') ? alias : `x-${alias}`;
    
    // List all command files
    const commandsDir = 'C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcore\\commands';
    const files = await list_directory({ path: commandsDir });
    
    // Filter for .md files
    const mdFiles = files.filter(file => file.includes('.md') && !file.includes('.example.md'));
    
    // Search each file for the alias
    for (const file of mdFiles) {
      const content = await read_file({ path: `${commandsDir}\\${file}` });
      
      // Parse command and check aliases
      const command = parseMdcFile(content);
      
      if (command.aliases.some(a => {
        const normalizedAlias = a.startsWith('x-') ? a : `x-${a}`;
        return normalizedAlias === searchAlias;
      })) {
        // Found matching alias
        return file.replace('.md', '');
      }
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
  return { _: [] };
}

function parseCommandString(argsString, commandDef) {
  const result = {
    _: []
  };
  
  // If empty string, return default
  if (!argsString.trim()) {
    return result;
  }
  
  // Split string on spaces, but respect quotes
  const parts = argsString.match(/(?:[^\s"']+|"[^"]*"|'[^']*')+/g) || [];
  
  // Process each part
  let currentFlag = null;
  
  for (const part of parts) {
    // Check if it's a flag
    if (part.startsWith('--')) {
      const flagName = part.substring(2);
      
      // Check if it's a flag with value (--flag=value)
      if (flagName.includes('=')) {
        const [name, value] = flagName.split('=');
        result[name] = value.replace(/^["']|["']$/g, '');
      } else {
        // Check if this flag should have a value based on command definition
        const flagDef = commandDef.flags.find(f => f.name === flagName);
        
        if (flagDef && flagDef.valueFormat) {
          // Flag expects a value
          currentFlag = flagName;
        } else {
          // Flag is boolean
          result[flagName] = true;
        }
      }
    } 
    // Check if it's a short flag (-h)
    else if (part.startsWith('-') && part.length === 2) {
      const shortFlag = part.substring(1);
      
      // Map short flags to their long versions
      const shortToLong = {
        h: 'help',
        v: 'verbose',
        f: 'force',
        d: 'debug'
      };
      
      if (shortToLong[shortFlag]) {
        result[shortToLong[shortFlag]] = true;
      } else {
        result[shortFlag] = true;
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
  
  // Extract task-specific parameters from first positional arg
  // Common pattern in ScaffoldX commands
  if (result._.length > 0 && commandDef.behavior.includes('task')) {
    // Check if it's a task-id parameter
    if (!result.taskId && !result.task && !result['task-id']) {
      result.taskId = result._[0];
    }
    
    // Check if it's a task name parameter
    if (!result.taskName && !result.name && commandDef.name === 'x-task-create') {
      result.taskName = result._[0];
    }
  }
  
  return result;
}
```

### analyzeCommandBehavior(commandDef, args)

Analyzes the command definition to extract operations.

```javascript
function analyzeCommandBehavior(commandDef, args) {
  // Initialize operation plan
  const operationPlan = {
    operations: [],
    context: {
      ...args,
      commandName: commandDef.name
    }
  };
  
  // Extract behavior description
  const behavior = commandDef.behavior || '';
  
  // Apply command type-specific analyzers
  // Identify command type based on name and behavior
  if (commandDef.name === 'x-task-create') {
    applyTaskCreateAnalyzer(operationPlan, behavior, args);
  } else if (commandDef.name === 'x-task-list') {
    applyTaskListAnalyzer(operationPlan, behavior, args);
  } else if (commandDef.name === 'x-task-complete') {
    applyTaskCompleteAnalyzer(operationPlan, behavior, args);
  } else if (commandDef.name === 'x-session-summarize') {
    applySessionSummarizeAnalyzer(operationPlan, behavior, args);
  } else if (commandDef.name === 'x-feedback-cycle') {
    applyFeedbackCycleAnalyzer(operationPlan, behavior, args);
  } else if (commandDef.name === 'x-sequential-thinking') {
    applySequentialThinkingAnalyzer(operationPlan, behavior, args);
  } else if (commandDef.name === 'x-analyze-complexity') {
    applyAnalyzeComplexityAnalyzer(operationPlan, behavior, args);
  } else if (commandDef.name === 'x-command-list') {
    applyCommandListAnalyzer(operationPlan, behavior, args);
  } else {
    // Generic analyzer for other commands
    applyGenericCommandAnalyzer(operationPlan, commandDef, behavior, args);
  }
  
  return operationPlan;
}
```

### Command-Specific Analyzers

These functions analyze behavior for specific commands.

```javascript
function applyGenericCommandAnalyzer(operationPlan, commandDef, behavior, args) {
  // Generic file operations through pattern matching
  
  // 1. File reading operations
  const readMatches = behavior.match(/reads?\s+([^.]+)\.[\w]+/gi) || [];
  for (const match of readMatches) {
    const fileDesc = match.replace(/reads?\s+|\.[\w]+/gi, '').trim();
    const operation = createReadOperation(fileDesc, args);
    if (operation) {
      operationPlan.operations.push(operation);
    }
  }
  
  // 2. File writing operations
  const writeMatches = behavior.match(/writes?\s+(?:to\s+)?([^.]+)\.[\w]+/gi) || [];
  for (const match of writeMatches) {
    const fileDesc = match.replace(/writes?\s+(?:to\s+)?|\.[\w]+/gi, '').trim();
    const operation = createWriteOperation(fileDesc, args, commandDef);
    if (operation) {
      operationPlan.operations.push(operation);
    }
  }
  
  // 3. Directory operations
  const dirMatches = behavior.match(/creates?\s+(?:a\s+)?directory\s+(.+?)(?:\.|;|\n|$)/gi) || [];
  for (const match of dirMatches) {
    const dirDesc = match.replace(/creates?\s+(?:a\s+)?directory\s+/gi, '').trim();
    const operation = createDirectoryOperation(dirDesc, args);
    if (operation) {
      operationPlan.operations.push(operation);
    }
  }
  
  // 4. Command execution operations
  const cmdMatches = behavior.match(/executes?\s+(?:the\s+)?command\s+["'](.+?)["']/gi) || [];
  for (const match of cmdMatches) {
    const cmdText = match.replace(/executes?\s+(?:the\s+)?command\s+["']/gi, '').replace(/["']$/, '').trim();
    operationPlan.operations.push({
      id: `execute_command_${operationPlan.operations.length + 1}`,
      type: 'execute_command',
      command: cmdText,
      description: `Execute command: ${cmdText}`
    });
  }
}

function applyTaskCreateAnalyzer(operationPlan, behavior, args) {
  // Get task name from args
  const taskName = args.taskName || args._[0] || 'new-task';
  
  // Generate task ID from name
  const taskId = generateTaskId(taskName);
  
  // Create task directory
  operationPlan.operations.push({
    id: 'create_task_directory',
    type: 'create_directory',
    path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${taskId}`,
    description: `Create directory for task ${taskId}`
  });
  
  // Check if tasks.json exists, create if not
  operationPlan.operations.push({
    id: 'check_tasks_json',
    type: 'file_exists',
    path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\tasks.json`,
    description: 'Check if tasks.json exists'
  });
  
  // Create task file with template
  operationPlan.operations.push({
    id: 'create_task_file',
    type: 'write_file',
    path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${taskId}\\task-${taskId}.md`,
    content: generateTaskContent(taskName, taskId, args),
    description: `Create task file for ${taskName}`
  });
  
  // Update tasks.json
  operationPlan.operations.push({
    id: 'update_tasks_json',
    type: 'update_tasks_json',
    taskInfo: {
      id: taskId,
      name: taskName,
      priority: args.priority || 'medium',
      status: 'todo',
      dueDate: args.due || null,
      parent: args.parent || null,
      created: new Date().toISOString().split('T')[0]
    },
    description: 'Update tasks.json with new task'
  });
  
  // Add context for result formatting
  operationPlan.context.taskId = taskId;
  operationPlan.context.taskName = taskName;
}

function applyTaskListAnalyzer(operationPlan, behavior, args) {
  // Add operation to read tasks.json
  operationPlan.operations.push({
    id: 'read_tasks_json',
    type: 'read_file',
    path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\tasks.json`,
    description: 'Read tasks.json file'
  });
  
  // Add operation to parse tasks.json
  operationPlan.operations.push({
    id: 'parse_tasks_json',
    type: 'parse_json',
    content: { refId: 'read_tasks_json' },
    description: 'Parse tasks.json content'
  });
  
  // Add operation to format task list
  operationPlan.operations.push({
    id: 'format_task_list',
    type: 'generate_content',
    template: 'task_list',
    data: {
      tasks: { refId: 'parse_tasks_json' },
      status: args.status || null,
      priority: args.priority || null,
      search: args.search || null,
      format: args.format || 'markdown'
    },
    description: 'Format task list output'
  });
}

function applyTaskCompleteAnalyzer(operationPlan, behavior, args) {
  // Get task ID from args
  const taskId = args.taskId || args._[0];
  
  if (!taskId) {
    throw new Error('Task ID is required for x-task-complete');
  }
  
  // Add operation to read task file
  operationPlan.operations.push({
    id: 'read_task_file',
    type: 'read_file',
    path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${taskId}\\task-${taskId}.md`,
    description: `Read task file for ${taskId}`
  });
  
  // Add operation to read tasks.json
  operationPlan.operations.push({
    id: 'read_tasks_json',
    type: 'read_file',
    path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\tasks.json`,
    description: 'Read tasks.json file'
  });
  
  // Add operation to parse tasks.json
  operationPlan.operations.push({
    id: 'parse_tasks_json',
    type: 'parse_json',
    content: { refId: 'read_tasks_json' },
    description: 'Parse tasks.json content'
  });
  
  // Add operation to update task status in tasks.json
  operationPlan.operations.push({
    id: 'update_task_status',
    type: 'update_task_status',
    taskId: taskId,
    status: 'completed',
    completedDate: new Date().toISOString().split('T')[0],
    tasksJson: { refId: 'parse_tasks_json' },
    description: `Update status of task ${taskId} to completed`
  });
  
  // Add operation to write updated tasks.json
  operationPlan.operations.push({
    id: 'write_tasks_json',
    type: 'write_file',
    path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\tasks.json`,
    content: { refId: 'update_task_status', contentType: 'json' },
    description: 'Write updated tasks.json file'
  });
  
  // Add operation to update task file
  operationPlan.operations.push({
    id: 'update_task_file',
    type: 'update_task_file',
    taskId: taskId,
    status: 'completed',
    completedDate: new Date().toISOString().split('T')[0],
    taskFile: { refId: 'read_task_file' },
    description: `Update task file for ${taskId}`
  });
  
  // Add operation to write updated task file
  operationPlan.operations.push({
    id: 'write_task_file',
    type: 'write_file',
    path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${taskId}\\task-${taskId}.md`,
    content: { refId: 'update_task_file' },
    description: `Write updated task file for ${taskId}`
  });
  
  // Add context for result formatting
  operationPlan.context.taskId = taskId;
}

function applySessionSummarizeAnalyzer(operationPlan, behavior, args) {
  // Get month from args or use current month
  const month = args.month || getCurrentMonth();
  
  // Ensure session summaries directory exists
  operationPlan.operations.push({
    id: 'create_summaries_directory',
    type: 'create_directory',
    path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xmemory\\session_summaries`,
    description: 'Create session summaries directory'
  });
  
  // Check if summary for month already exists
  operationPlan.operations.push({
    id: 'check_existing_summary',
    type: 'file_exists',
    path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xmemory\\session_summaries\\${month}.md`,
    description: `Check if summary for ${month} exists`
  });
  
  // Get git log for recent activity
  operationPlan.operations.push({
    id: 'get_git_log',
    type: 'execute_command',
    command: 'git log --since="1 day ago" --pretty=format:"%h - %s" --no-merges',
    description: 'Get recent git history'
  });
  
  // Generate session summary
  operationPlan.operations.push({
    id: 'generate_summary',
    type: 'generate_session_summary',
    month: month,
    gitLog: { refId: 'get_git_log' },
    customText: args.custom || '',
    existingSummary: { refId: 'check_existing_summary' },
    description: 'Generate session summary content'
  });
  
  // Write session summary
  operationPlan.operations.push({
    id: 'write_session_summary',
    type: 'write_file',
    path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xmemory\\session_summaries\\${month}.md`,
    content: { refId: 'generate_summary' },
    description: `Write session summary for ${month}`
  });
  
  // Add context for result formatting
  operationPlan.context.month = month;
}

function applyFeedbackCycleAnalyzer(operationPlan, behavior, args) {
  // Get prefix from args or use default
  const prefix = args.prefix || args._[0] || 'document';
  
  // Get stage from args or use default
  const stage = args.stage || '1';
  
  // Create feedback cycle directory
  operationPlan.operations.push({
    id: 'create_feedback_directory',
    type: 'create_directory',
    path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xworkbench\\feedback_cycle`,
    description: 'Create feedback cycle directory'
  });
  
  // Check stage to determine behavior
  if (stage === '1' || stage === 'draft') {
    // Stage 1: Initial draft
    
    // Create status file
    operationPlan.operations.push({
      id: 'create_status_file',
      type: 'write_file',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xworkbench\\feedback_cycle\\${prefix}_status.md`,
      content: generateStatusContent(prefix, 'draft', new Date().toISOString().split('T')[0]),
      description: `Create status file for ${prefix}`
    });
    
    // Create draft file
    operationPlan.operations.push({
      id: 'create_draft_file',
      type: 'write_file',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xworkbench\\feedback_cycle\\${prefix}_draft.md`,
      content: args.content || generateDraftTemplate(prefix),
      description: `Create draft file for ${prefix}`
    });
  } else if (stage === '2' || stage === 'refine') {
    // Stage 2: Refinement
    
    // Read draft file
    operationPlan.operations.push({
      id: 'read_draft_file',
      type: 'read_file',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xworkbench\\feedback_cycle\\${prefix}_draft.md`,
      description: `Read draft file for ${prefix}`
    });
    
    // Read status file
    operationPlan.operations.push({
      id: 'read_status_file',
      type: 'read_file',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xworkbench\\feedback_cycle\\${prefix}_status.md`,
      description: `Read status file for ${prefix}`
    });
    
    // Parse status file to get current refinement number
    operationPlan.operations.push({
      id: 'parse_status',
      type: 'parse_status',
      content: { refId: 'read_status_file' },
      description: 'Parse status file'
    });
    
    // Create refinement file
    operationPlan.operations.push({
      id: 'create_refinement_file',
      type: 'write_file',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xworkbench\\feedback_cycle\\${prefix}_refinement_{{refineNum}}.md`,
      content: args.content || { refId: 'read_draft_file', modifier: 'refine', feedback: args.feedback || '' },
      pathParams: { refId: 'parse_status' },
      description: `Create refinement file for ${prefix}`
    });
    
    // Update status file
    operationPlan.operations.push({
      id: 'update_status_file',
      type: 'write_file',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xworkbench\\feedback_cycle\\${prefix}_status.md`,
      content: { refId: 'parse_status', modifier: 'updateStatus', newStage: 'refinement' },
      description: `Update status file for ${prefix}`
    });
  } else if (stage === '3' || stage === 'finalize') {
    // Stage 3: Finalization
    
    // Read status file
    operationPlan.operations.push({
      id: 'read_status_file',
      type: 'read_file',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xworkbench\\feedback_cycle\\${prefix}_status.md`,
      description: `Read status file for ${prefix}`
    });
    
    // Parse status file to get latest refinement
    operationPlan.operations.push({
      id: 'parse_status',
      type: 'parse_status',
      content: { refId: 'read_status_file' },
      description: 'Parse status file'
    });
    
    // Read latest refinement file
    operationPlan.operations.push({
      id: 'read_latest_refinement',
      type: 'read_file',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xworkbench\\feedback_cycle\\${prefix}_refinement_{{latestRefine}}.md`,
      pathParams: { refId: 'parse_status' },
      description: 'Read latest refinement file'
    });
    
    // Create final file
    operationPlan.operations.push({
      id: 'create_final_file',
      type: 'write_file',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xworkbench\\feedback_cycle\\${prefix}_final.md`,
      content: { refId: 'read_latest_refinement' },
      description: `Create final file for ${prefix}`
    });
    
    // Determine destination based on prefix
    if (prefix.includes('prd')) {
      // PRD document
      operationPlan.operations.push({
        id: 'finalize_prd',
        type: 'write_file',
        path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcontext\\01_prd.md`,
        content: { refId: 'read_latest_refinement' },
        description: 'Finalize PRD document'
      });
    } else if (prefix.includes('implementation')) {
      // Implementation plan
      operationPlan.operations.push({
        id: 'finalize_implementation',
        type: 'write_file',
        path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcontext\\02_implementation_plan.md`,
        content: { refId: 'read_latest_refinement' },
        description: 'Finalize implementation plan'
      });
    } else if (prefix.includes('task') && args.taskId) {
      // Task document
      operationPlan.operations.push({
        id: 'finalize_task',
        type: 'write_file',
        path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${args.taskId}\\task-${args.taskId}.md`,
        content: { refId: 'read_latest_refinement' },
        description: `Finalize task document for ${args.taskId}`
      });
    }
    
    // Update status file
    operationPlan.operations.push({
      id: 'update_status_file',
      type: 'write_file',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xworkbench\\feedback_cycle\\${prefix}_status.md`,
      content: { refId: 'parse_status', modifier: 'updateStatus', newStage: 'finalized' },
      description: `Update status file for ${prefix}`
    });
  }
  
  // Add context for result formatting
  operationPlan.context.prefix = prefix;
  operationPlan.context.stage = stage;
}

function applySequentialThinkingAnalyzer(operationPlan, behavior, args) {
  // Get target task from args
  const targetTaskId = args.target || args._[0];
  
  if (!targetTaskId && !args.resume) {
    throw new Error('Target task ID is required for x-sequential-thinking');
  }
  
  if (args.resume) {
    // Resume an existing thinking session
    const sessionId = args.resume;
    
    // Read thinking session file
    operationPlan.operations.push({
      id: 'read_thinking_session',
      type: 'read_file',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\_thinking_sessions\\${sessionId}.md`,
      description: `Read thinking session ${sessionId}`
    });
    
    // Process thinking session
    operationPlan.operations.push({
      id: 'process_thinking_session',
      type: 'process_thinking_session',
      content: { refId: 'read_thinking_session' },
      createSubtasks: args.createSubtasks || false,
      description: 'Process thinking session'
    });
    
    // If creating subtasks
    if (args.createSubtasks) {
      // Get parent task ID
      operationPlan.operations.push({
        id: 'get_parent_task_id',
        type: 'extract_parent_task_id',
        content: { refId: 'read_thinking_session' },
        description: 'Extract parent task ID from thinking session'
      });
      
      // Create subtasks based on thinking
      operationPlan.operations.push({
        id: 'create_subtasks',
        type: 'create_subtasks',
        parentTaskId: { refId: 'get_parent_task_id' },
        thinking: { refId: 'process_thinking_session' },
        description: 'Create subtasks based on thinking'
      });
    }
    
    // Add context for result formatting
    operationPlan.context.sessionId = sessionId;
    operationPlan.context.createSubtasks = args.createSubtasks || false;
  } else {
    // Start a new thinking session
    
    // Create thinking sessions directory if it doesn't exist
    operationPlan.operations.push({
      id: 'create_thinking_directory',
      type: 'create_directory',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\_thinking_sessions`,
      description: 'Create thinking sessions directory'
    });
    
    // Read task file
    operationPlan.operations.push({
      id: 'read_task_file',
      type: 'read_file',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${targetTaskId}\\task-${targetTaskId}.md`,
      description: `Read task file for ${targetTaskId}`
    });
    
    // Generate session ID
    operationPlan.operations.push({
      id: 'generate_session_id',
      type: 'generate_session_id',
      taskId: targetTaskId,
      description: 'Generate thinking session ID'
    });
    
    // Analyze task complexity
    operationPlan.operations.push({
      id: 'analyze_task_complexity',
      type: 'analyze_task_complexity',
      taskContent: { refId: 'read_task_file' },
      description: 'Analyze task complexity'
    });
    
    // Generate sequential thinking
    operationPlan.operations.push({
      id: 'generate_sequential_thinking',
      type: 'generate_sequential_thinking',
      taskContent: { refId: 'read_task_file' },
      complexityAnalysis: { refId: 'analyze_task_complexity' },
      description: 'Generate sequential thinking'
    });
    
    // Save thinking session
    operationPlan.operations.push({
      id: 'save_thinking_session',
      type: 'write_file',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\_thinking_sessions\\{{sessionId}}.md`,
      content: { refId: 'generate_sequential_thinking' },
      pathParams: { refId: 'generate_session_id' },
      description: 'Save thinking session'
    });
    
    // If creating subtasks
    if (args.createSubtasks) {
      // Create subtasks based on thinking
      operationPlan.operations.push({
        id: 'create_subtasks',
        type: 'create_subtasks',
        parentTaskId: targetTaskId,
        thinking: { refId: 'generate_sequential_thinking' },
        description: 'Create subtasks based on thinking'
      });
    }
    
    // Add context for result formatting
    operationPlan.context.targetTaskId = targetTaskId;
    operationPlan.context.sessionId = { refId: 'generate_session_id' };
    operationPlan.context.createSubtasks = args.createSubtasks || false;
  }
}

function applyAnalyzeComplexityAnalyzer(operationPlan, behavior, args) {
  // Process different input types based on flags
  if (args.task || (args._ && args._.length > 0 && !args.file && !args.dir)) {
    // Task-based analysis
    const taskId = args.task || args._[0];
    applyTaskAnalysis(operationPlan, taskId, args);
  } else if (args.file) {
    // File-based analysis
    applyFileAnalysis(operationPlan, args.file, args);
  } else if (args.dir) {
    // Directory-based analysis
    applyDirectoryAnalysis(operationPlan, args.dir, args);
  } else if (args._ && args._.length > 0) {
    // Text-based analysis
    const inputText = args._.join(' ');
    applyTextAnalysis(operationPlan, inputText, args);
  } else {
    throw new Error('No input provided for complexity analysis. Please provide a task ID, file path, directory path, or text description.');
  }
  
  // Add AI autonomy assessment if requested
  if (args.aiAutonomy || args['ai-autonomy']) {
    addAutonomyAssessment(operationPlan, args);
  }
  
  // Add aggregate metrics if requested
  if (args.aggregate) {
    addAggregateMetrics(operationPlan, args);
  }
}

// Task-based analysis
function applyTaskAnalysis(operationPlan, taskId, args) {
  // Read task file
  operationPlan.operations.push({
    id: 'read_task_file',
    type: 'read_file',
    path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${taskId}\\task-${taskId}.md`,
    description: `Read task file for ${taskId}`
  });
  
  // Create analysis directory if it doesn't exist
  operationPlan.operations.push({
    id: 'create_task_analysis_directory',
    type: 'create_directory',
    path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${taskId}\\analysis`,
    description: `Create analysis directory for ${taskId}`
  });
  
  // Analyze task complexity
  operationPlan.operations.push({
    id: 'analyze_task_complexity',
    type: 'analyze_complexity',
    inputType: 'task',
    taskContent: { refId: 'read_task_file' },
    detailed: args.detailed || false,
    metrics: args.metrics || null,
    description: 'Analyze task complexity'
  });
  
  // Format analysis based on requested format
  operationPlan.operations.push({
    id: 'format_analysis',
    type: 'format_analysis',
    analysisData: { refId: 'analyze_task_complexity' },
    format: args.format || 'markdown',
    entityName: `Task: ${taskId}`,
    entityType: 'task',
    detailed: args.detailed || false,
    autonomyOnly: args.autonomyOnly || args['autonomy-only'] || false,
    description: 'Format analysis output'
  });
  
  // Save analysis if requested
  if (args.save) {
    const filename = (args.autonomyOnly || args['autonomy-only']) ? 'autonomy_analysis.md' : 'complexity_analysis.md';
    
    operationPlan.operations.push({
      id: 'save_analysis',
      type: 'write_file',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${taskId}\\analysis\\${filename}`,
      content: { refId: 'format_analysis' },
      description: `Save analysis to ${filename}`
    });
    
    // Update task file with complexity score
    operationPlan.operations.push({
      id: 'update_task_file',
      type: 'update_task_complexity',
      taskId: taskId,
      taskFile: { refId: 'read_task_file' },
      complexityAnalysis: { refId: 'analyze_task_complexity' },
      description: `Update task file with complexity score for ${taskId}`
    });
    
    // Write updated task file
    operationPlan.operations.push({
      id: 'write_task_file',
      type: 'write_file',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${taskId}\\task-${taskId}.md`,
      content: { refId: 'update_task_file' },
      description: `Write updated task file for ${taskId}`
    });
    
    // Read tasks.json
    operationPlan.operations.push({
      id: 'read_tasks_json',
      type: 'read_file',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\tasks.json`,
      description: 'Read tasks.json file'
    });
    
    // Parse tasks.json
    operationPlan.operations.push({
      id: 'parse_tasks_json',
      type: 'parse_json',
      content: { refId: 'read_tasks_json' },
      description: 'Parse tasks.json content'
    });
    
    // Update task complexity in tasks.json
    operationPlan.operations.push({
      id: 'update_tasks_json',
      type: 'update_task_complexity_json',
      taskId: taskId,
      tasksJson: { refId: 'parse_tasks_json' },
      complexityAnalysis: { refId: 'analyze_task_complexity' },
      description: `Update complexity score in tasks.json for ${taskId}`
    });
    
    // Write updated tasks.json
    operationPlan.operations.push({
      id: 'write_tasks_json',
      type: 'write_file',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\tasks.json`,
      content: { refId: 'update_tasks_json', contentType: 'json' },
      description: 'Write updated tasks.json file'
    });
  }
  
  // Add context for result formatting
  operationPlan.context.entityType = 'task';
  operationPlan.context.entityId = taskId;
  operationPlan.context.format = args.format || 'markdown';
  operationPlan.context.save = args.save || false;
}

// File-based analysis
function applyFileAnalysis(operationPlan, filePath, args) {
  // Normalize file path
  const normalizedPath = filePath.startsWith('C:\\') ? 
    filePath : `C:\\__dev\\_projects\\ScaffoldX\\${filePath}`;
  
  // Read file
  operationPlan.operations.push({
    id: 'read_file',
    type: 'read_file',
    path: normalizedPath,
    description: `Read file ${filePath}`
  });
  
  // Get file info to extract name
  operationPlan.operations.push({
    id: 'get_file_info',
    type: 'get_file_info',
    path: normalizedPath,
    description: `Get info for file ${filePath}`
  });
  
  // Analyze file complexity
  operationPlan.operations.push({
    id: 'analyze_file_complexity',
    type: 'analyze_complexity',
    inputType: 'file',
    fileContent: { refId: 'read_file' },
    fileInfo: { refId: 'get_file_info' },
    detailed: args.detailed || false,
    metrics: args.metrics || null,
    description: 'Analyze file complexity'
  });
  
  // Format analysis based on requested format
  operationPlan.operations.push({
    id: 'format_analysis',
    type: 'format_analysis',
    analysisData: { refId: 'analyze_file_complexity' },
    format: args.format || 'markdown',
    entityName: `File: ${filePath}`,
    entityType: 'file',
    detailed: args.detailed || false,
    autonomyOnly: args.autonomyOnly || args['autonomy-only'] || false,
    description: 'Format analysis output'
  });
  
  // Save analysis if requested
  if (args.save) {
    operationPlan.operations.push({
      id: 'save_analysis',
      type: 'write_file',
      path: `${normalizedPath}.complexity_analysis.md`,
      content: { refId: 'format_analysis' },
      description: `Save analysis for ${filePath}`
    });
  }
  
  // Add context for result formatting
  operationPlan.context.entityType = 'file';
  operationPlan.context.entityPath = filePath;
  operationPlan.context.format = args.format || 'markdown';
  operationPlan.context.save = args.save || false;
}

// Directory-based analysis
function applyDirectoryAnalysis(operationPlan, dirPath, args) {
  // Normalize directory path
  const normalizedPath = dirPath.startsWith('C:\\') ? 
    dirPath : `C:\\__dev\\_projects\\ScaffoldX\\${dirPath}`;
  
  // List directory contents
  operationPlan.operations.push({
    id: 'list_directory',
    type: 'list_directory',
    path: normalizedPath,
    description: `List contents of directory ${dirPath}`
  });
  
  // Filter for code files
  operationPlan.operations.push({
    id: 'filter_code_files',
    type: 'filter_files',
    files: { refId: 'list_directory' },
    pattern: '\\.(js|py|ts|tsx|jsx|java|c|cpp|h|cs|go|rb|php|html|css|scss|sass)$',
    description: 'Filter for code files'
  });
  
  // Analyze directory complexity
  operationPlan.operations.push({
    id: 'analyze_directory_complexity',
    type: 'analyze_complexity',
    inputType: 'directory',
    dirPath: normalizedPath,
    files: { refId: 'filter_code_files' },
    detailed: args.detailed || false,
    metrics: args.metrics || null,
    description: 'Analyze directory complexity'
  });
  
  // Format analysis based on requested format
  operationPlan.operations.push({
    id: 'format_analysis',
    type: 'format_analysis',
    analysisData: { refId: 'analyze_directory_complexity' },
    format: args.format || 'markdown',
    entityName: `Directory: ${dirPath}`,
    entityType: 'directory',
    detailed: args.detailed || false,
    autonomyOnly: args.autonomyOnly || args['autonomy-only'] || false,
    description: 'Format analysis output'
  });
  
  // Save analysis if requested
  if (args.save) {
    operationPlan.operations.push({
      id: 'save_analysis',
      type: 'write_file',
      path: `${normalizedPath}\\complexity_analysis.md`,
      content: { refId: 'format_analysis' },
      description: `Save analysis for ${dirPath}`
    });
  }
  
  // Add context for result formatting
  operationPlan.context.entityType = 'directory';
  operationPlan.context.entityPath = dirPath;
  operationPlan.context.format = args.format || 'markdown';
  operationPlan.context.save = args.save || false;
}

// Text-based analysis
function applyTextAnalysis(operationPlan, inputText, args) {
  // Analyze text complexity
  operationPlan.operations.push({
    id: 'analyze_text_complexity',
    type: 'analyze_complexity',
    inputType: 'text',
    textContent: inputText,
    detailed: args.detailed || false,
    metrics: args.metrics || null,
    description: 'Analyze text complexity'
  });
  
  // Format analysis based on requested format
  operationPlan.operations.push({
    id: 'format_analysis',
    type: 'format_analysis',
    analysisData: { refId: 'analyze_text_complexity' },
    format: args.format || 'markdown',
    entityName: 'Text Description',
    entityType: 'text',
    detailed: args.detailed || false,
    autonomyOnly: args.autonomyOnly || args['autonomy-only'] || false,
    description: 'Format analysis output'
  });
  
  // Save analysis if requested
  if (args.save) {
    // Create workbench directory if it doesn't exist
    operationPlan.operations.push({
      id: 'create_workbench_directory',
      type: 'create_directory',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xworkbench\\analysis`,
      description: 'Create workbench analysis directory'
    });
    
    // Save analysis
    operationPlan.operations.push({
      id: 'save_analysis',
      type: 'write_file',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xworkbench\\analysis\\text_complexity_analysis.md`,
      content: { refId: 'format_analysis' },
      description: 'Save text analysis'
    });
  }
  
  // Add context for result formatting
  operationPlan.context.entityType = 'text';
  operationPlan.context.inputText = inputText.substring(0, 50) + (inputText.length > 50 ? '...' : '');
  operationPlan.context.format = args.format || 'markdown';
  operationPlan.context.save = args.save || false;
}

// Add AI autonomy assessment
function addAutonomyAssessment(operationPlan, args) {
  operationPlan.operations.push({
    id: 'analyze_autonomy',
    type: 'analyze_autonomy',
    complexity: { refId: args.autonomyOnly || args['autonomy-only'] ? null : 'analyze_complexity' },
    entityType: operationPlan.context.entityType,
    entityContent: getEntityContentRef(operationPlan.context.entityType),
    metrics: args.metrics || null,
    detailed: args.detailed || false,
    description: 'Analyze AI autonomy potential'
  });
  
  // Update format analysis to include autonomy data
  const formatOpIndex = operationPlan.operations.findIndex(op => op.id === 'format_analysis');
  if (formatOpIndex >= 0) {
    operationPlan.operations[formatOpIndex].autonomyData = { refId: 'analyze_autonomy' };
    operationPlan.operations[formatOpIndex].includeAutonomy = true;
  }
  
  // Update context
  operationPlan.context.includeAutonomy = true;
  operationPlan.context.autonomyOnly = args.autonomyOnly || args['autonomy-only'] || false;
}

// Add aggregate metrics for directory analysis
function addAggregateMetrics(operationPlan, args) {
  if (operationPlan.context.entityType !== 'directory') {
    return; // Only applicable for directory analysis
  }
  
  operationPlan.operations.push({
    id: 'calculate_aggregate_metrics',
    type: 'calculate_aggregate_metrics',
    directoryAnalysis: { refId: 'analyze_directory_complexity' },
    autonomyData: operationPlan.context.includeAutonomy ? { refId: 'analyze_autonomy' } : null,
    description: 'Calculate aggregate metrics'
  });
  
  // Update format analysis to include aggregate data
  const formatOpIndex = operationPlan.operations.findIndex(op => op.id === 'format_analysis');
  if (formatOpIndex >= 0) {
    operationPlan.operations[formatOpIndex].aggregateData = { refId: 'calculate_aggregate_metrics' };
    operationPlan.operations[formatOpIndex].includeAggregate = true;
  }
  
  // Update context
  operationPlan.context.includeAggregate = true;
}

// Helper function to get the appropriate reference for entity content
function getEntityContentRef(entityType) {
  switch (entityType) {
    case 'task':
      return { refId: 'read_task_file' };
    case 'file':
      return { refId: 'read_file' };
    case 'directory':
      return { refId: 'filter_code_files' };
    case 'text':
      return null; // For text, the input is already in the initial operation
    default:
      return null;
  }
}

function applyCommandListAnalyzer(operationPlan, behavior, args) {
  // List core commands
  operationPlan.operations.push({
    id: 'list_core_commands',
    type: 'list_directory',
    path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcore\\commands`,
    description: 'List core commands'
  });
  
  // Filter for command files
  operationPlan.operations.push({
    id: 'filter_command_files',
    type: 'filter_files',
    files: { refId: 'list_core_commands' },
    pattern: 'x-*.md',
    exclude: '*.example.md',
    description: 'Filter for command files'
  });
  
  // Check for custom commands directory
  operationPlan.operations.push({
    id: 'check_custom_commands',
    type: 'directory_exists',
    path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcustom\\commands`,
    description: 'Check for custom commands directory'
  });
  
  // If custom commands directory exists, list custom commands
  operationPlan.operations.push({
    id: 'list_custom_commands',
    type: 'conditional_operation',
    condition: { refId: 'check_custom_commands' },
    operation: {
      type: 'list_directory',
      path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcustom\\commands`,
      description: 'List custom commands'
    },
    description: 'List custom commands if directory exists'
  });
  
  // Filter for custom command files
  operationPlan.operations.push({
    id: 'filter_custom_command_files',
    type: 'conditional_operation',
    condition: { refId: 'check_custom_commands' },
    operation: {
      type: 'filter_files',
      files: { refId: 'list_custom_commands' },
      pattern: 'x-*.md',
      exclude: '*.example.md',
      description: 'Filter for custom command files'
    },
    description: 'Filter for custom command files if directory exists'
  });
  
  // Load command details for each command
  operationPlan.operations.push({
    id: 'load_command_details',
    type: 'load_command_details',
    coreCommands: { refId: 'filter_command_files' },
    customCommands: { refId: 'filter_custom_command_files' },
    description: 'Load command details'
  });
  
  // Format command list output
  operationPlan.operations.push({
    id: 'format_command_list',
    type: 'format_command_list',
    commands: { refId: 'load_command_details' },
    format: args.format || 'markdown',
    category: args.category || null,
    search: args.search || null,
    detailed: args.detailed || false,
    description: 'Format command list output'
  });
  
  // Add context for result formatting
  operationPlan.context.format = args.format || 'markdown';
  operationPlan.context.detailed = args.detailed || false;
}
```

### Utility Functions

Helper functions for command processing.

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
    if (!taskId) {
      console.warn('Task ID not provided for task file read operation');
      return null;
    }
    path = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${taskId}\\task-${taskId}.md`;
  } else {
    // Default to a file in xcontext
    path = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcontext\\${fileDesc}.md`;
  }
  
  return {
    id: `read_${fileDesc.replace(/\s+/g, '_')}`,
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
    if (!taskId) {
      console.warn('Task ID not provided for task file write operation');
      return null;
    }
    path = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${taskId}\\task-${taskId}.md`;
    content = args.content || '';
  } else {
    // Default to a file in xcontext
    path = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcontext\\${fileDesc}.md`;
    content = args.content || '';
  }
  
  return {
    id: `write_${fileDesc.replace(/\s+/g, '_')}`,
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
    if (!taskId) {
      console.warn('Task ID not provided for task directory operation');
      return null;
    }
    path = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${taskId}`;
  } else if (dirDesc.includes('feedback')) {
    path = 'C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xworkbench\\feedback_cycle';
  } else {
    // Default to a directory in scaffoldx
    path = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\${dirDesc}`;
  }
  
  return {
    id: `create_${dirDesc.replace(/\s+/g, '_')}_directory`,
    type: 'create_directory',
    path,
    description: `Create ${dirDesc} directory`
  };
}

// Generate a task ID from a task name
function generateTaskId(taskName) {
  const normalized = taskName
    .toLowerCase()
    .replace(/[^\w\s-]/g, '')  // Remove special characters
    .replace(/\s+/g, '_');     // Replace spaces with underscores
  
  // Add timestamp to ensure uniqueness
  const timestamp = Date.now().toString().slice(-6);
  
  return `${normalized.slice(0, 20)}_${timestamp}`;
}

// Generate content for a new task file
function generateTaskContent(taskName, taskId, args) {
  const now = new Date().toISOString().split('T')[0];
  
  return `---
title: ${taskName}
description: ${args.description || `Task for ${taskName}`}
status: todo
priority: ${args.priority || 'medium'}
complexity: ${args.complexity || 'unknown'}
${args.due ? `due_date: ${args.due}` : ''}
${args.parent ? `parent_task: ${args.parent}` : ''}
created: ${now}
last_updated: ${now}
---

# Task: ${taskName}

## Description

${args.description || `Task for ${taskName}`}

## Requirements

${args.requirements || '- Requirement 1\n- Requirement 2'}

## Acceptance Criteria

${args.criteria || '- Criteria 1\n- Criteria 2'}

## Dependencies

${args.dependencies || 'None'}

## Notes

${args.notes || 'None'}

## Progress Tracking

- [ ] Task started
${args.subtasks ? args.subtasks.split(',').map(st => `- [ ] ${st.trim()}`).join('\n') : '- [ ] Subtask 1\n- [ ] Subtask 2'}
- [ ] Task completed
`;
}

// Generate content for a status file
function generateStatusContent(prefix, stage, date) {
  return `---
prefix: ${prefix}
stage: ${stage}
date_created: ${date}
last_updated: ${date}
refinement_count: 0
---

# Status: ${prefix}

Current stage: ${stage}
Last updated: ${date}
Refinement count: 0
`;
}

// Generate template for a draft document
function generateDraftTemplate(prefix) {
  const now = new Date().toISOString().split('T')[0];
  
  if (prefix.includes('prd')) {
    return `---
title: Project Requirements Document
description: PRD for the project
version: 1.0.0
status: draft
created: ${now}
last_updated: ${now}
---

# Project Requirements Document

## 1. Introduction

[Brief introduction to the project]

## 2. Project Goals

[Goals of the project]

## 3. Key Features

[Key features of the project]

## 4. User Stories

[User stories]

## 5. Functional Requirements

[Functional requirements]

## 6. Non-Functional Requirements

[Non-functional requirements]

## 7. Success Metrics

[Success metrics]
`;
  } else if (prefix.includes('implementation')) {
    return `---
title: Implementation Plan
description: Implementation plan for the project
version: 1.0.0
status: draft
created: ${now}
last_updated: ${now}
---

# Implementation Plan

## 1. Architecture Overview

[Brief overview of the architecture]

## 2. Technology Stack

[Technology stack to be used]

## 3. Implementation Steps

[Step-by-step implementation plan]

## 4. Timeline

[Project timeline]

## 5. Resources Required

[Resources required for implementation]

## 6. Risk Assessment

[Risk assessment and mitigation strategies]
`;
  } else {
    return `---
title: ${prefix}
description: Draft document
status: draft
created: ${now}
last_updated: ${now}
---

# ${prefix}

[Draft content]
`;
  }
}

// Generate session summary
function generateSessionSummary(args, month) {
  const now = new Date().toISOString().split('T')[0];
  const customText = args.custom || '';
  
  return `# Session Summary: ${now}

## Summary

Session summary for ${month}

${customText ? `\n## Custom Notes\n\n${customText}\n\n` : ''}

## Key Insights

- Progress made on current tasks
- Documentation updated
- Core functionality improved

---
`;
}

// Get current month in YYYY-MM format
function getCurrentMonth() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}
```

### Operation Execution

Functions for executing operations.

```javascript
async function executeOperations(operations, context) {
  const results = {};
  
  for (let i = 0; i < operations.length; i++) {
    const operation = operations[i];
    
    // Resolve references in operation parameters
    const resolvedOperation = resolveOperationReferences(operation, results);
    
    // Execute the operation
    try {
      const result = await executeOperation(resolvedOperation, context, results);
      results[operation.id] = result;
    } catch (error) {
      console.error(`Error executing operation ${operation.id}: ${error.message}`);
      results[operation.id] = { error: error.message };
      
      // Check if operation is critical
      if (!operation.optional) {
        throw new Error(`Failed to execute operation ${operation.id}: ${error.message}`);
      }
    }
  }
  
  return results;
}

function resolveOperationReferences(operation, results) {
  const resolved = { ...operation };
  
  // Handle references in properties
  for (const [key, value] of Object.entries(resolved)) {
    if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
      if (value.refId && results[value.refId]) {
        // Direct reference to a result
        resolved[key] = results[value.refId];
        
        // Apply modifier if specified
        if (value.modifier && typeof value.modifier === 'string') {
          resolved[key] = applyModifier(resolved[key], value.modifier, value);
        }
        
        // Handle content type conversion
        if (value.contentType === 'json' && typeof resolved[key] === 'object') {
          resolved[key] = JSON.stringify(resolved[key], null, 2);
        }
      } else if (key !== 'operation') {
        // Recursively resolve nested objects (except 'operation' which might be a template)
        resolved[key] = resolveOperationReferences(value, results);
      }
    } else if (key === 'path' && resolved.pathParams && resolved.pathParams.refId && results[resolved.pathParams.refId]) {
      // Handle path parameters
      const params = results[resolved.pathParams.refId];
      resolved.path = resolved.path.replace(/\{\{([^}]+)\}\}/g, (match, param) => {
        return params[param] || match;
      });
    }
  }
  
  return resolved;
}

function applyModifier(value, modifier, options) {
  switch (modifier) {
    case 'refine':
      // Refine content based on feedback
      return refineDraftWithFeedback(value, options.feedback || '');
    
    case 'updateStatus':
      // Update status file
      return updateStatusFile(value, options.newStage || 'draft');
    
    default:
      return value;
  }
}

function refineDraftWithFeedback(draft, feedback) {
  // Simple implementation - in practice, this would use LLM to refine the draft
  return `${draft}\n\n## Feedback Applied\n\n${feedback}`;
}

function updateStatusFile(status, newStage) {
  // Parse status file content
  const lines = status.split('\n');
  let updated = '';
  
  for (const line of lines) {
    if (line.startsWith('stage:')) {
      updated += `stage: ${newStage}\n`;
    } else if (line.startsWith('last_updated:')) {
      updated += `last_updated: ${new Date().toISOString().split('T')[0]}\n`;
    } else if (line.startsWith('refinement_count:') && newStage === 'refinement') {
      const count = parseInt(line.split(':')[1].trim()) + 1;
      updated += `refinement_count: ${count}\n`;
    } else if (line.startsWith('Current stage:')) {
      updated += `Current stage: ${newStage}\n`;
    } else if (line.startsWith('Last updated:')) {
      updated += `Last updated: ${new Date().toISOString().split('T')[0]}\n`;
    } else if (line.startsWith('Refinement count:') && newStage === 'refinement') {
      const count = parseInt(line.split(':')[1].trim()) + 1;
      updated += `Refinement count: ${count}\n`;
    } else {
      updated += `${line}\n`;
    }
  }
  
  return updated;
}

async function executeOperation(operation, context, results) {
  switch (operation.type) {
    case 'read_file':
      return await read_file({ path: operation.path });
    
    case 'write_file':
      await write_file({ path: operation.path, content: operation.content });
      return { success: true, path: operation.path };
    
    case 'create_directory':
      await create_directory({ path: operation.path });
      return { success: true, path: operation.path };
    
    case 'execute_command':
      return await execute_command({ command: operation.command });
    
    case 'parse_json':
      try {
        return JSON.parse(operation.content);
      } catch (error) {
        console.error(`Error parsing JSON: ${error.message}`);
        return { error: 'Invalid JSON format' };
      }
    
    case 'file_exists':
      try {
        await get_file_info({ path: operation.path });
        return { exists: true, path: operation.path };
      } catch (error) {
        return { exists: false, path: operation.path };
      }
    
    case 'directory_exists':
      try {
        const info = await get_file_info({ path: operation.path });
        return info.isDirectory;
      } catch (error) {
        return false;
      }
    
    case 'list_directory':
      return await list_directory({ path: operation.path });
    
    case 'get_file_info':
      return await get_file_info({ path: operation.path });
    
    case 'filter_files':
      const files = operation.files || [];
      const pattern = new RegExp(operation.pattern.replace(/\*/g, '.*'), 'i');
      const exclude = operation.exclude ? new RegExp(operation.exclude.replace(/\*/g, '.*'), 'i') : null;
      
      return files.filter(file => {
        return pattern.test(file) && (!exclude || !exclude.test(file));
      });
    
    case 'conditional_operation':
      const condition = operation.condition;
      if (condition === true || (typeof condition === 'object' && condition.exists)) {
        // Execute the nested operation
        return await executeOperation(operation.operation, context, results);
      }
      return null;
    
    case 'update_tasks_json':
      const tasksJson = operation.tasksJson || {};
      const taskInfo = operation.taskInfo || {};
      
      // Check if tasks property exists
      if (!tasksJson.tasks) {
        tasksJson.tasks = [];
      }
      
      // Check if task already exists
      const existingTaskIndex = tasksJson.tasks.findIndex(t => t.id === taskInfo.id);
      
      if (existingTaskIndex >= 0) {
        // Update existing task
        tasksJson.tasks[existingTaskIndex] = {
          ...tasksJson.tasks[existingTaskIndex],
          ...taskInfo
        };
      } else {
        // Add new task
        tasksJson.tasks.push(taskInfo);
      }
      
      return tasksJson;
    
    case 'update_task_status':
      const tasks = operation.tasksJson || {};
      const taskId = operation.taskId;
      const status = operation.status || 'completed';
      
      // Check if tasks property exists
      if (!tasks.tasks) {
        throw new Error('Invalid tasks.json format');
      }
      
      // Find task by ID
      const taskIndex = tasks.tasks.findIndex(t => t.id === taskId);
      
      if (taskIndex < 0) {
        throw new Error(`Task ${taskId} not found in tasks.json`);
      }
      
      // Update task status
      tasks.tasks[taskIndex].status = status;
      
      // Add completed date if completing the task
      if (status === 'completed') {
        tasks.tasks[taskIndex].completedDate = operation.completedDate || new Date().toISOString().split('T')[0];
      }
      
      return tasks;
    
    case 'update_task_file':
      const taskFile = operation.taskFile || '';
      const newStatus = operation.status || 'completed';
      
      // Parse task file
      const lines = taskFile.split('\n');
      let updated = '';
      let inFrontmatter = false;
      
      for (const line of lines) {
        if (line.trim() === '---') {
          inFrontmatter = !inFrontmatter;
          updated += `${line}\n`;
          continue;
        }
        
        if (inFrontmatter && line.startsWith('status:')) {
          updated += `status: ${newStatus}\n`;
        } else if (inFrontmatter && newStatus === 'completed' && line.startsWith('completed:')) {
          updated += `completed: ${operation.completedDate || new Date().toISOString().split('T')[0]}\n`;
        } else if (inFrontmatter && line.startsWith('last_updated:')) {
          updated += `last_updated: ${new Date().toISOString().split('T')[0]}\n`;
        } else {
          updated += `${line}\n`;
        }
      }
      
      // Update progress tracking
      if (newStatus === 'completed') {
        updated = updated.replace(/- \[ \] Task completed/g, '- [x] Task completed');
      }
      
      return updated;
    
    case 'parse_status':
      const statusFile = operation.content || '';
      const statusData = {};
      
      // Parse frontmatter
      const frontmatterMatch = statusFile.match(/^---\s*\n([\s\S]*?)\n---\s*\n/);
      if (frontmatterMatch) {
        const frontmatterBlock = frontmatterMatch[1];
        const frontmatterLines = frontmatterBlock.split('\n');
        
        for (const line of frontmatterLines) {
          if (line.trim() && line.includes(':')) {
            const [key, ...valueParts] = line.split(':');
            const value = valueParts.join(':').trim();
            statusData[key.trim()] = value;
          }
        }
      }
      
      // Parse other fields
      const refinementCountMatch = statusFile.match(/Refinement count:\s*(\d+)/);
      if (refinementCountMatch) {
        statusData.refineNum = parseInt(refinementCountMatch[1]) + 1;
        statusData.latestRefine = parseInt(refinementCountMatch[1]);
      } else {
        statusData.refineNum = 1;
        statusData.latestRefine = 0;
      }
      
      return statusData;
    
    case 'generate_session_summary':
      const existingSummary = operation.existingSummary?.exists ? 
        await read_file({ path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xmemory\\session_summaries\\${operation.month}.md` }) : null;
      
      const now = new Date().toISOString().split('T')[0];
      const customText = operation.customText || '';
      const gitLog = operation.gitLog || 'No recent git activity';
      
      // Use existing summary if it exists
      if (existingSummary) {
        // Simple update - in practice, this would use LLM to update the summary
        return `${existingSummary.trim()}\n\n## Session: ${now}\n\n${customText ? `${customText}\n\n` : ''}## Git Activity\n\n${gitLog}\n\n---\n`;
      }
      
      // Generate new summary
      return `# Session Summary: ${now}

## Summary

Session summary for ${operation.month}

${customText ? `\n## Custom Notes\n\n${customText}\n\n` : ''}

## Git Activity

${gitLog}

## Key Insights

- Progress made on current tasks
- Documentation updated
- Core functionality improved

---
`;
    
    case 'generate_session_id':
      return {
        sessionId: `${operation.taskId}_${Date.now().toString().slice(-6)}`
      };
    
    case 'analyze_task_complexity':
      // Simple implementation - in practice, this would use LLM to analyze complexity
      const taskContent = operation.taskContent || '';
      const complexity = calculateComplexity(taskContent);
      
      return {
        complexityScore: complexity.score,
        complexityLevel: complexity.level,
        factors: complexity.factors,
        analysis: complexity.analysis
      };
    
    case 'generate_sequential_thinking':
      // Simple implementation - in practice, this would use LLM to generate thinking
      const thinking = generateThinking(operation.taskContent, operation.complexityAnalysis);
      
      return {
        taskId: extractTaskId(operation.taskContent),
        thinking: thinking.steps,
        componentCount: thinking.componentCount,
        subtasks: thinking.subtasks
      };
    
    case 'process_thinking_session':
      // Simple implementation - in practice, this would use LLM to process the session
      const thinkingSession = operation.content || '';
      return processThinkingSession(thinkingSession);
    
    case 'extract_parent_task_id':
      // Extract parent task ID from thinking session
      const session = operation.content || '';
      const taskIdMatch = session.match(/taskId:\s*(\w+)/);
      return taskIdMatch ? taskIdMatch[1] : null;
    
    case 'create_subtasks':
      // Simple implementation - in practice, this would create actual subtasks
      const parentTaskId = operation.parentTaskId;
      const thinking = operation.thinking || {};
      
      return {
        parentTaskId,
        subtasksCreated: thinking.subtasks ? thinking.subtasks.length : 0,
        subtasks: thinking.subtasks || []
      };
    
    case 'load_command_details':
      const coreCommands = operation.coreCommands || [];
      const customCommands = operation.customCommands || [];
      const commandDetails = [];
      
      // Load core command details
      for (const file of coreCommands) {
        try {
          const content = await read_file({ path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcore\\commands\\${file}` });
          const command = parseMdcFile(content);
          commandDetails.push({
            name: file.replace('.md', ''),
            description: command.description,
            priority: command.frontmatter.priority || 'medium',
            category: command.frontmatter.category || 'core',
            aliases: command.aliases,
            source: 'core'
          });
        } catch (error) {
          console.error(`Error loading command details for ${file}: ${error.message}`);
        }
      }
      
      // Load custom command details
      for (const file of customCommands || []) {
        try {
          const content = await read_file({ path: `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcustom\\commands\\${file}` });
          const command = parseMdcFile(content);
          commandDetails.push({
            name: file.replace('.md', ''),
            description: command.description,
            priority: command.frontmatter.priority || 'medium',
            category: command.frontmatter.category || 'custom',
            aliases: command.aliases,
            source: 'custom'
          });
        } catch (error) {
          console.error(`Error loading command details for ${file}: ${error.message}`);
        }
      }
      
      return commandDetails;
    
    case 'format_command_list':
      const commands = operation.commands || [];
      const format = operation.format || 'markdown';
      const category = operation.category;
      const search = operation.search;
      const detailed = operation.detailed || false;
      
      // Filter commands
      const filteredCommands = commands.filter(cmd => {
        return (!category || cmd.category === category) && 
               (!search || cmd.name.includes(search) || cmd.description.includes(search));
      });
      
      // Sort commands
      filteredCommands.sort((a, b) => {
        // Sort by source (core first, then custom)
        if (a.source !== b.source) {
          return a.source === 'core' ? -1 : 1;
        }
        
        // Then sort by name
        return a.name.localeCompare(b.name);
      });
      
      // Format output
      if (format === 'json') {
        return JSON.stringify(filteredCommands, null, 2);
      } else {
        // Default to markdown
        let output = '# ScaffoldX Commands\n\n';
        
        if (category) {
          output += `## Category: ${category}\n\n`;
        } else {
          output += '## All Commands\n\n';
        }
        
        if (search) {
          output += `Filtered by search: "${search}"\n\n`;
        }
        
        if (filteredCommands.length === 0) {
          output += 'No matching commands found.\n';
        } else {
          if (detailed) {
            // Detailed listing
            for (const cmd of filteredCommands) {
              output += `### ${cmd.name}\n\n`;
              output += `${cmd.description}\n\n`;
              
              if (cmd.aliases && cmd.aliases.length > 0) {
                output += `**Aliases**: ${cmd.aliases.join(', ')}\n\n`;
              }
              
              output += `**Priority**: ${cmd.priority}\n`;
              output += `**Category**: ${cmd.category}\n`;
              output += `**Source**: ${cmd.source}\n\n`;
              output += '---\n\n';
            }
          } else {
            // Simple listing
            for (const cmd of filteredCommands) {
              output += `- **${cmd.name}**: ${cmd.description}\n`;
            }
          }
        }
        
        return output;
      }
    
    case 'analyze_complexity':
      // Enhanced complexity analysis for different input types
      const inputType = operation.inputType || 'task';
      
      switch (inputType) {
        case 'task':
          return analyzeTaskComplexity(operation.taskContent || '', operation.detailed || false, operation.metrics || null);
        
        case 'file':
          return analyzeFileComplexity(operation.fileContent || '', operation.fileInfo || {}, operation.detailed || false, operation.metrics || null);
        
        case 'directory':
          return analyzeDirectoryComplexity(operation.dirPath || '', operation.files || [], operation.detailed || false, operation.metrics || null);
        
        case 'text':
          return analyzeTextComplexity(operation.textContent || '', operation.detailed || false, operation.metrics || null);
        
        default:
          throw new Error(`Unsupported input type for complexity analysis: ${inputType}`);
      }
    
    case 'analyze_autonomy':
      // AI autonomy analysis
      const entityType = operation.entityType || 'task';
      const entityContent = operation.entityContent || '';
      const complexityData = operation.complexity || null;
      
      return analyzeAutonomy(entityType, entityContent, complexityData, operation.detailed || false, operation.metrics || null);
    
    case 'format_analysis':
      // Format analysis results
      const analysisData = operation.analysisData || {};
      const outputFormat = operation.format || 'markdown';
      const entityName = operation.entityName || 'Unknown';
      const entityType = operation.entityType || 'unknown';
      const autonomyData = operation.autonomyData || null;
      const aggregateData = operation.aggregateData || null;
      const includeAutonomy = operation.includeAutonomy || false;
      const includeAggregate = operation.includeAggregate || false;
      const autonomyOnly = operation.autonomyOnly || false;
      
      return formatAnalysisOutput(
        analysisData,
        outputFormat,
        entityName,
        entityType,
        autonomyData,
        aggregateData,
        includeAutonomy,
        includeAggregate,
        autonomyOnly,
        operation.detailed || false
      );
    
    case 'calculate_aggregate_metrics':
      // Calculate aggregate metrics for a directory
      const directoryAnalysis = operation.directoryAnalysis || {};
      const directoryAutonomyData = operation.autonomyData || null;
      
      return calculateAggregateMetrics(directoryAnalysis, directoryAutonomyData);
    
    case 'update_task_complexity':
      const taskFileContent = operation.taskFile || '';
      const complexityAnalysis = operation.complexityAnalysis || {};
      
      // Parse task file
      const taskLines = taskFileContent.split('\n');
      let updatedTask = '';
      let inTaskFrontmatter = false;
      
      for (const line of taskLines) {
        if (line.trim() === '---') {
          inTaskFrontmatter = !inTaskFrontmatter;
          updatedTask += `${line}\n`;
          continue;
        }
        
        if (inTaskFrontmatter && line.startsWith('complexity:')) {
          updatedTask += `complexity: ${complexityAnalysis.complexityLevel || 'medium'}\n`;
        } else if (inTaskFrontmatter && line.startsWith('complexity_score:')) {
          updatedTask += `complexity_score: ${complexityAnalysis.complexityScore || 5}\n`;
        } else if (inTaskFrontmatter && line.startsWith('last_updated:')) {
          updatedTask += `last_updated: ${new Date().toISOString().split('T')[0]}\n`;
        } else {
          updatedTask += `${line}\n`;
        }
      }
      
      return updatedTask;
    
    case 'update_task_complexity_json':
      const tasksJsonContent = operation.tasksJson || {};
      const taskComplexityAnalysis = operation.complexityAnalysis || {};
      const targetTaskId = operation.taskId;
      
      // Check if tasks property exists
      if (!tasksJsonContent.tasks) {
        throw new Error('Invalid tasks.json format');
      }
      
      // Find task by ID
      const targetTaskIndex = tasksJsonContent.tasks.findIndex(t => t.id === targetTaskId);
      
      if (targetTaskIndex < 0) {
        throw new Error(`Task ${targetTaskId} not found in tasks.json`);
      }
      
      // Update task complexity
      tasksJsonContent.tasks[targetTaskIndex].complexity = taskComplexityAnalysis.complexityLevel || 'medium';
      tasksJsonContent.tasks[targetTaskIndex].complexityScore = taskComplexityAnalysis.complexityScore || 5;
      
      if (taskComplexityAnalysis.autonomyScore) {
        tasksJsonContent.tasks[targetTaskIndex].autonomyScore = taskComplexityAnalysis.autonomyScore;
        tasksJsonContent.tasks[targetTaskIndex].autonomyLevel = taskComplexityAnalysis.autonomyLevel || 'medium';
      }
      
      return tasksJsonContent;
    
    default:
      throw new Error(`Unsupported operation type: ${operation.type}`);
  }
}
```

### Helper Functions for Complexity and Thinking

```javascript
function calculateComplexity(taskContent) {
  // Default values
  let score = 50;
  let level = 'medium';
  const factors = [];
  
  // Task length factor
  const contentLength = taskContent.length;
  if (contentLength > 5000) {
    score += 20;
    factors.push('Large task description (+20)');
  } else if (contentLength > 2000) {
    score += 10;
    factors.push('Medium task description (+10)');
  }
  
  // Requirements count factor
  const requirementsMatch = taskContent.match(/## Requirements([\s\S]*?)(?=##|$)/);
  if (requirementsMatch) {
    const reqCount = (requirementsMatch[1].match(/- /g) || []).length;
    if (reqCount > 10) {
      score += 20;
      factors.push('High requirement count (+20)');
    } else if (reqCount > 5) {
      score += 10;
      factors.push('Medium requirement count (+10)');
    }
  }
  
  // Dependencies factor
  if (taskContent.includes('Dependencies') && !taskContent.includes('Dependencies\n\nNone')) {
    score += 15;
    factors.push('Has dependencies (+15)');
  }
  
  // Technical keywords
  const technicalKeywords = ['integration', 'api', 'database', 'authentication', 'complex', 'difficult', 'challenging'];
  for (const keyword of technicalKeywords) {
    if (taskContent.toLowerCase().includes(keyword)) {
      score += 5;
      factors.push(`Contains technical keyword "${keyword}" (+5)`);
    }
  }
  
  // Generate complexity level
  if (score >= 80) {
    level = 'high';
  } else if (score >= 50) {
    level = 'medium';
  } else {
    level = 'low';
  }
  
  // Normalize score to 1-10 scale for standardized presentation
  const normalizedScore = Math.max(1, Math.min(10, Math.round(score / 10)));
  
  return {
    score: normalizedScore,
    level,
    factors,
    analysis: `Task complexity analysis resulted in a score of ${normalizedScore}/10 (${level} complexity). Factors include: ${factors.join(', ')}.`
  };
}

// Enhanced complexity analysis functions for different input types

// Task-based complexity analysis
function analyzeTaskComplexity(taskContent, detailed, metrics) {
  // Start with basic complexity calculation
  const complexityResult = calculateComplexity(taskContent);
  
  // Extract task metadata
  const metadataMatch = taskContent.match(/^---\s*\n([\s\S]*?)\n---\s*\n/);
  const metadata = {};
  
  if (metadataMatch) {
    const metadataBlock = metadataMatch[1];
    const metadataLines = metadataBlock.split('\n');
    
    for (const line of metadataLines) {
      if (line.trim() && line.includes(':')) {
        const [key, ...valueParts] = line.split(':');
        const value = valueParts.join(':').trim();
        metadata[key.trim()] = value;
      }
    }
  }
  
  // Extract task title
  const titleMatch = taskContent.match(/# Task:?\s*(.+?)(?:\n|$)/i) || 
                     taskContent.match(/# (.+?)(?:\n|$)/);
  const taskTitle = titleMatch ? titleMatch[1].trim() : 'Unknown Task';
  
  // Extract task description
  const descriptionMatch = taskContent.match(/## Description\s+([\s\S]*?)(?=##|$)/);
  const description = descriptionMatch ? descriptionMatch[1].trim() : '';
  
  // Extract requirements
  const requirementsMatch = taskContent.match(/## Requirements\s+([\s\S]*?)(?=##|$)/);
  const requirements = requirementsMatch ? requirementsMatch[1].trim() : '';
  
  // Do detailed breakdown of complexity factors by component
  const breakdown = {
    conceptual: calculateConceptualComplexity(taskContent),
    implementation: calculateImplementationComplexity(taskContent),
    domainKnowledge: calculateDomainKnowledgeComplexity(taskContent),
    dependencies: calculateDependenciesComplexity(taskContent),
    risks: calculateRisksComplexity(taskContent)
  };
  
  // Enhanced analysis for detailed mode
  let detailedAnalysis = '';
  if (detailed) {
    detailedAnalysis = `
The task "${taskTitle}" has been analyzed in detail:

1. Conceptual Complexity (${breakdown.conceptual.score}/10): ${breakdown.conceptual.description}
2. Implementation Complexity (${breakdown.implementation.score}/10): ${breakdown.implementation.description}
3. Domain Knowledge Requirements (${breakdown.domainKnowledge.score}/10): ${breakdown.domainKnowledge.description}
4. Dependencies (${breakdown.dependencies.score}/10): ${breakdown.dependencies.description}
5. Risk Factors (${breakdown.risks.score}/10): ${breakdown.risks.description}

${generateBreakdownRecommendations(complexityResult.level, breakdown)}
`;
  }
  
  // Estimate effort in hours based on complexity
  const effort = estimateEffort(complexityResult.score);
  
  // Determine if the task should be broken down
  const shouldBreakDown = complexityResult.score >= 7; // Tasks with complexity 7+ should be broken down
  
  // Suggest subtasks if breakdown is recommended
  let suggestedSubtasks = [];
  if (shouldBreakDown) {
    suggestedSubtasks = generateSubtaskSuggestions(taskContent, breakdown);
  }
  
  return {
    complexityScore: complexityResult.score,
    complexityLevel: complexityResult.level,
    taskTitle: taskTitle,
    description: description,
    breakdown: breakdown,
    factors: complexityResult.factors,
    shouldBreakDown: shouldBreakDown,
    suggestedSubtasks: suggestedSubtasks,
    estimatedEffort: effort,
    storyPoints: convertToStoryPoints(complexityResult.score),
    detailedAnalysis: detailed ? detailedAnalysis : complexityResult.analysis,
    now: new Date().toISOString().split('T')[0]
  };
}

// File-based complexity analysis
function analyzeFileComplexity(fileContent, fileInfo, detailed, metrics) {
  // Basic complexity calculation based on code characteristics
  const complexity = calculateCodeComplexity(fileContent);
  
  // Extract file name and extension
  const fileName = fileInfo.name || 'Unknown';
  const fileExt = getFileExtension(fileName);
  const language = getLanguageFromExtension(fileExt);
  
  // Calculate different aspects of code complexity
  const breakdown = {
    cyclomaticComplexity: calculateCyclomaticComplexity(fileContent, language),
    codeSize: calculateCodeSize(fileContent),
    functionCount: countFunctions(fileContent, language),
    dependencyCount: countDependencies(fileContent, language),
    commentRatio: calculateCommentRatio(fileContent, language)
  };
  
  // Identify complex functions or code sections
  const complexSections = identifyComplexSections(fileContent, language);
  
  // Enhanced analysis for detailed mode
  let detailedAnalysis = '';
  if (detailed) {
    detailedAnalysis = `
File "${fileName}" has been analyzed in detail:

1. Language: ${language || 'Unknown'}
2. Code Size: ${breakdown.codeSize.description}
3. Cyclomatic Complexity: ${breakdown.cyclomaticComplexity.description}
4. Function Count: ${breakdown.functionCount.count} functions identified
5. Dependencies: ${breakdown.dependencyCount.count} dependencies identified
6. Comment Ratio: ${breakdown.commentRatio.ratio}% (${breakdown.commentRatio.assessment})

${complexSections.length > 0 ? '## Complex Sections Identified\n\n' + complexSections.map(s => `- Line ${s.lineRange}: ${s.description} (complexity: ${s.complexity}/10)`).join('\n') : 'No particularly complex sections identified.'}

${generateCodeRecommendations(complexity.level, breakdown)}
`;
  }
  
  return {
    complexityScore: complexity.score,
    complexityLevel: complexity.level,
    fileName: fileName,
    language: language,
    breakdown: breakdown,
    factors: complexity.factors,
    complexSections: complexSections,
    refactoringNeeded: complexity.score >= 7,
    estimatedRefactoringEffort: estimateEffort(complexity.score),
    detailedAnalysis: detailed ? detailedAnalysis : complexity.analysis,
    now: new Date().toISOString().split('T')[0]
  };
}

// Directory-based complexity analysis
function analyzeDirectoryComplexity(dirPath, files, detailed, metrics) {
  // Calculate overall metrics based on files
  const totalFiles = files.length;
  
  // Group files by language/type
  const filesByType = groupFilesByType(files);
  
  // Calculate complexity score based on directory characteristics
  const complexity = calculateDirectoryComplexity(totalFiles, filesByType);
  
  // Identify most complex files (would normally analyze each file)
  const complexFiles = identifyComplexFiles(files);
  
  // Identify architectural patterns in the codebase
  const architecturalPatterns = identifyArchitecturalPatterns(files);
  
  // Identify integration points
  const integrationPoints = identifyIntegrationPoints(files);
  
  // Identify risk areas
  const riskAreas = identifyRiskAreas(files, filesByType, complexity);
  
  // Enhanced analysis for detailed mode
  let detailedAnalysis = '';
  if (detailed) {
    detailedAnalysis = `
Directory analysis of ${files.length} files:

1. File Distribution: ${Object.entries(filesByType).map(([type, count]) => `${type}: ${count}`).join(', ')}
2. Overall Structure: ${assessDirectoryStructure(files)}
3. Complexity Score: ${complexity.score}/10 (${complexity.level} complexity)

${complexFiles.length > 0 ? '## Most Complex Files\n\n' + complexFiles.map(f => `- ${f.filename}: ${f.reason} (complexity: ${f.score}/10)`).join('\n') : 'No particularly complex files identified.'}

${architecturalPatterns.length > 0 ? '## Architectural Patterns\n\n' + architecturalPatterns.map(p => `- ${p}`).join('\n') : 'No clear architectural patterns identified.'}

${riskAreas.length > 0 ? '## Risk Areas\n\n' + riskAreas.map(r => `- ${r}`).join('\n') : 'No significant risk areas identified.'}

${integrationPoints.length > 0 ? '## Integration Points\n\n' + integrationPoints.map(i => `- ${i}`).join('\n') : 'No clear integration points identified.'}

${generateDirectoryRecommendations(complexity.level, filesByType)}
`;
  }
  
  return {
    complexityScore: complexity.score,
    complexityLevel: complexity.level,
    totalFiles: totalFiles,
    filesByType: filesByType,
    factors: complexity.factors,
    complexFiles: complexFiles,
    architecturalPatterns: architecturalPatterns,
    integrationPoints: integrationPoints,
    riskAreas: riskAreas,
    estimatedUnderstandingEffort: estimateEffort(complexity.score * 1.5), // Understanding a codebase takes more effort
    refactoringRecommended: complexity.score >= 8,
    detailedAnalysis: detailed ? detailedAnalysis : complexity.analysis,
    now: new Date().toISOString().split('T')[0]
  };
}

// Text-based complexity analysis
function analyzeTextComplexity(textContent, detailed, metrics) {
  // Basic complexity calculation
  const complexityResult = calculateComplexity(textContent);
  
  // Extract key phrases and concepts
  const keyPhrases = extractKeyPhrases(textContent);
  
  // Calculate text-specific metrics
  const breakdown = {
    conceptual: calculateConceptualComplexity(textContent),
    implementation: calculateImplementationComplexity(textContent),
    domainKnowledge: calculateDomainKnowledgeComplexity(textContent),
    risks: calculateRisksComplexity(textContent)
  };
  
  // Enhanced analysis for detailed mode
  let detailedAnalysis = '';
  if (detailed) {
    detailedAnalysis = `
The provided text description has been analyzed in detail:

1. Conceptual Complexity (${breakdown.conceptual.score}/10): ${breakdown.conceptual.description}
2. Implementation Complexity (${breakdown.implementation.score}/10): ${breakdown.implementation.description}
3. Domain Knowledge Requirements (${breakdown.domainKnowledge.score}/10): ${breakdown.domainKnowledge.description}
4. Risk Factors (${breakdown.risks.score}/10): ${breakdown.risks.description}

${keyPhrases.length > 0 ? '## Key Phrases Identified\n\n' + keyPhrases.map(p => `- ${p}`).join('\n') : 'No key phrases identified.'}

${generateTextRecommendations(complexityResult.level, breakdown)}
`;
  }
  
  // Determine if the task should be broken down
  const shouldBreakDown = complexityResult.score >= 7;
  
  // Suggest subtasks if breakdown is recommended
  let suggestedSubtasks = [];
  if (shouldBreakDown) {
    suggestedSubtasks = generateSubtaskSuggestions(textContent, breakdown);
  }
  
  return {
    complexityScore: complexityResult.score,
    complexityLevel: complexityResult.level,
    breakdown: breakdown,
    factors: complexityResult.factors,
    keyPhrases: keyPhrases,
    shouldBreakDown: shouldBreakDown,
    suggestedSubtasks: suggestedSubtasks,
    estimatedEffort: estimateEffort(complexityResult.score),
    storyPoints: convertToStoryPoints(complexityResult.score),
    detailedAnalysis: detailed ? detailedAnalysis : complexityResult.analysis,
    now: new Date().toISOString().split('T')[0]
  };
}

// AI autonomy analysis
function analyzeAutonomy(entityType, entityContent, complexityData, detailed, metrics) {
  // Define autonomy criteria
  const criteria = {
    clarity: assessClarity(entityContent),
    scope: assessScope(entityContent),
    technicalComplexity: assessTechnicalComplexity(entityContent, complexityData),
    integration: assessIntegration(entityContent),
    externalDependencies: assessExternalDependencies(entityContent),
    humanJudgment: assessHumanJudgment(entityContent),
    testability: assessTestability(entityContent)
  };
  
  // Calculate overall autonomy score (1-5 scale, where 1 is highest autonomy)
  const criteriaScores = Object.values(criteria).map(c => c.score);
  const averageScore = criteriaScores.reduce((sum, score) => sum + score, 0) / criteriaScores.length;
  const autonomyScore = Math.round(averageScore);
  
  // Determine autonomy level
  const autonomyLevel = getAutonomyLevel(autonomyScore);
  
  // Identify human intervention points
  const interventionPoints = identifyInterventionPoints(criteria);
  
  // Generate autonomy improvement suggestions
  const improvementSuggestions = generateAutonomyImprovements(criteria);
  
  // Estimate AI implementation time
  const implementationTime = estimateAutonomyImplementationTime(autonomyScore);
  
  // Calculate story points
  const storyPoints = autonomyToStoryPoints(autonomyScore);
  
  // Calculate estimated human involvement time
  const humanInvolvementTime = estimateHumanInvolvementTime(autonomyScore);
  
  // Enhanced analysis for detailed mode
  let detailedAnalysis = '';
  if (detailed) {
    detailedAnalysis = `
AI Autonomy assessment for ${entityType}:

1. Clarity & Specificity (${criteria.clarity.score}/5): ${criteria.clarity.description}
2. Scope Definition (${criteria.scope.score}/5): ${criteria.scope.description}
3. Technical Complexity (${criteria.technicalComplexity.score}/5): ${criteria.technicalComplexity.description}
4. Integration Needs (${criteria.integration.score}/5): ${criteria.integration.description}
5. External Dependencies (${criteria.externalDependencies.score}/5): ${criteria.externalDependencies.description}
6. Human Judgment Requirements (${criteria.humanJudgment.score}/5): ${criteria.humanJudgment.description}
7. Testability (${criteria.testability.score}/5): ${criteria.testability.description}

Overall Autonomy Score: ${autonomyScore}/5 (${autonomyLevel})

${interventionPoints.length > 0 ? '## Human Intervention Points\n\n' + interventionPoints.map(p => `- ${p}`).join('\n') : 'No specific human intervention points identified.'}

${improvementSuggestions.length > 0 ? '## Autonomy Improvement Suggestions\n\n' + improvementSuggestions.map(s => `- ${s}`).join('\n') : 'No specific improvement suggestions identified.'}

Estimated AI Implementation Time: ${implementationTime}
Estimated Human Involvement: ${humanInvolvementTime}
Story Points: ${storyPoints}
`;
  }
  
  return {
    autonomyScore: autonomyScore,
    autonomyLevel: autonomyLevel,
    criteria: criteria,
    interventionPoints: interventionPoints,
    improvementSuggestions: improvementSuggestions,
    implementationTime: implementationTime,
    humanInvolvementTime: humanInvolvementTime,
    storyPoints: storyPoints,
    successLikelihood: getSuccessLikelihood(autonomyScore),
    detailedAnalysis: detailed ? detailedAnalysis : `AI Autonomy Score: ${autonomyScore}/5 (${autonomyLevel}). ${getAutonomyDescription(autonomyScore)}`,
    now: new Date().toISOString().split('T')[0]
  };
}

// Calculate aggregate metrics for directory analysis
function calculateAggregateMetrics(directoryAnalysis, autonomyData) {
  // Basic aggregate metrics
  const aggregateMetrics = {
    averageComplexity: calculateAverageComplexity(directoryAnalysis),
    complexityDistribution: calculateComplexityDistribution(directoryAnalysis),
    totalStoryPoints: calculateTotalStoryPoints(directoryAnalysis)
  };
  
  // Add autonomy metrics if available
  if (autonomyData) {
    aggregateMetrics.averageAutonomy = calculateAverageAutonomy(autonomyData);
    aggregateMetrics.autonomyDistribution = calculateAutonomyDistribution(autonomyData);
    aggregateMetrics.totalHumanInvolvement = calculateTotalHumanInvolvement(autonomyData);
    aggregateMetrics.aiImplementationTimeRange = calculateAIImplementationTimeRange(autonomyData);
  }
  
  // Generate implementation timeline
  aggregateMetrics.implementationTimeline = generateImplementationTimeline(aggregateMetrics);
  
  // Generate aggregate recommendations
  aggregateMetrics.recommendations = generateAggregateRecommendations(aggregateMetrics);
  
  return aggregateMetrics;
}

// Format analysis output
function formatAnalysisOutput(
  analysisData,
  outputFormat,
  entityName,
  entityType,
  autonomyData,
  aggregateData,
  includeAutonomy,
  includeAggregate,
  autonomyOnly,
  detailed
) {
  // Format based on output format
  if (outputFormat === 'json') {
    // Prepare JSON output
    const result = {
      entityName: entityName,
      entityType: entityType,
      date: analysisData.now,
      complexity: !autonomyOnly ? {
        score: analysisData.complexityScore,
        level: analysisData.complexityLevel,
        factors: analysisData.factors,
        breakdown: analysisData.breakdown,
        estimatedEffort: analysisData.estimatedEffort,
        storyPoints: analysisData.storyPoints,
        analysis: analysisData.detailedAnalysis
      } : null,
      autonomy: includeAutonomy && autonomyData ? {
        score: autonomyData.autonomyScore,
        level: autonomyData.autonomyLevel,
        criteria: autonomyData.criteria,
        implementationTime: autonomyData.implementationTime,
        humanInvolvementTime: autonomyData.humanInvolvementTime,
        storyPoints: autonomyData.storyPoints,
        successLikelihood: autonomyData.successLikelihood,
        interventionPoints: autonomyData.interventionPoints,
        improvementSuggestions: autonomyData.improvementSuggestions,
        analysis: autonomyData.detailedAnalysis
      } : null,
      aggregate: includeAggregate && aggregateData ? aggregateData : null
    };
    
    // Return formatted JSON
    return JSON.stringify(result, null, 2);
  } else {
    // Default to markdown format
    return formatAnalysisAsMarkdown(
      analysisData,
      entityName,
      entityType,
      autonomyData,
      aggregateData,
      includeAutonomy,
      includeAggregate,
      autonomyOnly,
      detailed
    );
  }
}

// Format analysis as markdown
function formatAnalysisAsMarkdown(
  analysisData,
  entityName,
  entityType,
  autonomyData,
  aggregateData,
  includeAutonomy,
  includeAggregate,
  autonomyOnly,
  detailed
) {
  const now = analysisData.now || new Date().toISOString().split('T')[0];
  
  // Determine title based on analysis type
  let title = 'Complexity Analysis';
  if (autonomyOnly) {
    title = 'AI Autonomy Assessment';
  } else if (includeAutonomy) {
    title = 'Complexity and AI Autonomy Analysis';
  }
  
  // Build frontmatter
  let markdown = `---
title: ${title} for ${entityName}
description: ${autonomyOnly ? 'AI autonomy assessment' : includeAutonomy ? 'Detailed complexity analysis with AI autonomy assessment' : 'Detailed complexity analysis'}
created: ${now}
last_updated: ${now}
entity_type: ${entityType}
${!autonomyOnly ? `complexity_score: ${analysisData.complexityScore}
complexity_level: ${analysisData.complexityLevel}` : ''}
${includeAutonomy && autonomyData ? `autonomy_score: ${autonomyData.autonomyScore}
autonomy_level: ${autonomyData.autonomyLevel}` : ''}
---

# ${title}: ${entityName}

`;

  // Add summary section
  markdown += `## Summary

`;

  if (!autonomyOnly) {
    markdown += `This ${entityType} has been analyzed and determined to have a **${analysisData.complexityLevel}** complexity level with a score of **${analysisData.complexityScore}/10**.

`;
  }

  if (includeAutonomy && autonomyData) {
    markdown += `The AI autonomy assessment resulted in a score of **${autonomyData.autonomyScore}/5** (${autonomyData.autonomyLevel}), indicating ${getAutonomyDescription(autonomyData.autonomyScore)}

`;
  }

  // Add complexity assessment section if not autonomy only
  if (!autonomyOnly) {
    markdown += `## Complexity Assessment

### Overall Scores

| Metric | Score | Description |
|--------|-------|-------------|
| Complexity | ${analysisData.complexityScore}/10 | ${analysisData.complexityLevel} |
${includeAutonomy && autonomyData ? `| AI Autonomy | ${autonomyData.autonomyScore}/5 | ${autonomyData.autonomyLevel} |` : ''}

`;

    // Add complexity breakdown based on entity type
    markdown += `### Complexity Breakdown

`;

    if (entityType === 'task' || entityType === 'text') {
      markdown += `| Factor | Score | Description |
|--------|-------|-------------|
| Conceptual Complexity | ${analysisData.breakdown.conceptual.score}/10 | ${analysisData.breakdown.conceptual.description} |
| Implementation Complexity | ${analysisData.breakdown.implementation.score}/10 | ${analysisData.breakdown.implementation.description} |
| Domain Knowledge | ${analysisData.breakdown.domainKnowledge.score}/10 | ${analysisData.breakdown.domainKnowledge.description} |
${analysisData.breakdown.dependencies ? `| Dependencies | ${analysisData.breakdown.dependencies.score}/10 | ${analysisData.breakdown.dependencies.description} |` : ''}
| Risk Factors | ${analysisData.breakdown.risks.score}/10 | ${analysisData.breakdown.risks.description} |

`;
    } else if (entityType === 'file') {
      markdown += `| Factor | Score | Description |
|--------|-------|-------------|
| Code Size | ${analysisData.breakdown.codeSize.score}/10 | ${analysisData.breakdown.codeSize.description} |
| Cyclomatic Complexity | ${analysisData.breakdown.cyclomaticComplexity.score}/10 | ${analysisData.breakdown.cyclomaticComplexity.description} |
| Function Count | ${analysisData.breakdown.functionCount.score}/10 | ${analysisData.breakdown.functionCount.count} functions identified |
| Dependency Count | ${analysisData.breakdown.dependencyCount.score}/10 | ${analysisData.breakdown.dependencyCount.count} dependencies |
| Comment Ratio | ${analysisData.breakdown.commentRatio.score}/10 | ${analysisData.breakdown.commentRatio.ratio}% (${analysisData.breakdown.commentRatio.assessment}) |

`;

      // Add complex sections if available
      if (analysisData.complexSections && analysisData.complexSections.length > 0) {
        markdown += `### Complex Sections

| Location | Complexity | Description |
|----------|------------|-------------|
${analysisData.complexSections.map(section => `| Line ${section.lineRange} | ${section.complexity}/10 | ${section.description} |`).join('\n')}

`;
      }
    } else if (entityType === 'directory') {
      markdown += `| Factor | Value |
|--------|-------|
| Total Files | ${analysisData.totalFiles} |
| File Distribution | ${Object.entries(analysisData.filesByType).map(([type, count]) => `${type}: ${count}`).join(', ')} |

`;

      // Add complex files if available
      if (analysisData.complexFiles && analysisData.complexFiles.length > 0) {
        markdown += `### Most Complex Files

| File | Complexity | Reason |
|------|------------|--------|
${analysisData.complexFiles.map(file => `| ${file.filename} | ${file.score}/10 | ${file.reason} |`).join('\n')}

`;
      }

      // Add architectural patterns if available
      if (analysisData.architecturalPatterns && analysisData.architecturalPatterns.length > 0) {
        markdown += `### Architectural Patterns

${analysisData.architecturalPatterns.map(pattern => `- ${pattern}`).join('\n')}

`;
      }

      // Add risk areas if available
      if (analysisData.riskAreas && analysisData.riskAreas.length > 0) {
        markdown += `### Risk Areas

${analysisData.riskAreas.map(risk => `- ${risk}`).join('\n')}

`;
      }

      // Add integration points if available
      if (analysisData.integrationPoints && analysisData.integrationPoints.length > 0) {
        markdown += `### Integration Points

${analysisData.integrationPoints.map(point => `- ${point}`).join('\n')}

`;
      }
    }

    // Add effort estimates
    markdown += `### Effort Estimates

- **Estimated Implementation Time**: ${analysisData.estimatedEffort}
- **Story Points**: ${analysisData.storyPoints}
${entityType === 'task' || entityType === 'text' ? `- **Recommended Approach**: ${analysisData.shouldBreakDown ? 'Break down into subtasks' : 'Implement as a single unit'}` : ''}
${entityType === 'file' ? `- **Refactoring Needed**: ${analysisData.refactoringNeeded ? 'Yes' : 'No'}` : ''}
${entityType === 'directory' ? `- **Understanding Effort**: ${analysisData.estimatedUnderstandingEffort}` : ''}

`;
  }

  // Add AI autonomy assessment section if included
  if (includeAutonomy && autonomyData) {
    markdown += `## AI Autonomy Assessment

### Autonomy Criteria

| Criterion | Score | Description |
|-----------|-------|-------------|
| Clarity & Specificity | ${autonomyData.criteria.clarity.score}/5 | ${autonomyData.criteria.clarity.description} |
| Scope Definition | ${autonomyData.criteria.scope.score}/5 | ${autonomyData.criteria.scope.description} |
| Technical Complexity | ${autonomyData.criteria.technicalComplexity.score}/5 | ${autonomyData.criteria.technicalComplexity.description} |
| Integration Needs | ${autonomyData.criteria.integration.score}/5 | ${autonomyData.criteria.integration.description} |
| External Dependencies | ${autonomyData.criteria.externalDependencies.score}/5 | ${autonomyData.criteria.externalDependencies.description} |
| Human Judgment Needs | ${autonomyData.criteria.humanJudgment.score}/5 | ${autonomyData.criteria.humanJudgment.description} |
| Testability | ${autonomyData.criteria.testability.score}/5 | ${autonomyData.criteria.testability.description} |

### Human Intervention Points

${autonomyData.interventionPoints.map(point => `- ${point}`).join('\n')}

### Suggested Improvements for Autonomy

${autonomyData.improvementSuggestions.map(suggestion => `- ${suggestion}`).join('\n')}

### Implementation Estimates

- **Estimated AI Implementation Time**: ${autonomyData.implementationTime}
- **Required Human Involvement**: ${autonomyData.humanInvolvementTime}
- **Success Likelihood**: ${autonomyData.successLikelihood}

`;
  }

  // Add aggregate metrics section if included
  if (includeAggregate && aggregateData) {
    markdown += `## Aggregate Metrics

### Overview Statistics

| Metric | Value |
|--------|-------|
| Average Complexity | ${aggregateData.averageComplexity.toFixed(1)}/10 |
| Total Story Points | ${aggregateData.totalStoryPoints} |
${includeAutonomy ? `| Average Autonomy | ${aggregateData.averageAutonomy.toFixed(1)}/5 |` : ''}
${includeAutonomy ? `| Total Human Involvement | ${aggregateData.totalHumanInvolvement} |` : ''}

### Distribution

${Object.entries(aggregateData.complexityDistribution).map(([level, count]) => `- **${level}** complexity: ${count} components (${Math.round(count / Object.values(aggregateData.complexityDistribution).reduce((sum, val) => sum + val, 0) * 100)}%)`).join('\n')}

${includeAutonomy ? `\n### Autonomy Distribution\n\n${Object.entries(aggregateData.autonomyDistribution).map(([level, count]) => `- **${level}** autonomy: ${count} components (${Math.round(count / Object.values(aggregateData.autonomyDistribution).reduce((sum, val) => sum + val, 0) * 100)}%)`).join('\n')}` : ''}

### Implementation Timeline

${aggregateData.implementationTimeline}

### Recommendations

${aggregateData.recommendations.map(rec => `- ${rec}`).join('\n')}

`;
  }

  // Add detailed analysis section
  if (detailed) {
    markdown += `## Detailed Analysis

${!autonomyOnly ? analysisData.detailedAnalysis : ''}
${includeAutonomy && autonomyData ? (autonomyOnly ? autonomyData.detailedAnalysis : `\n### Autonomy Analysis\n\n${autonomyData.detailedAnalysis}`) : ''}

`;
  }

  // Add recommendations
  if (!autonomyOnly && (entityType === 'task' || entityType === 'text') && analysisData.shouldBreakDown && analysisData.suggestedSubtasks && analysisData.suggestedSubtasks.length > 0) {
    markdown += `## Recommendations

### Task Breakdown Recommendation

This ${entityType} should be broken down into the following subtasks:

${analysisData.suggestedSubtasks.map((subtask, index) => `${index + 1}. ${subtask}`).join('\n')}

`;
  }

  // Add footer
  markdown += `---

*Analysis generated using ScaffoldX's x-analyze-complexity command on ${now}*`;

  return markdown;
}

// Helper functions for complexity analysis

function generateComplexityAnalysis(taskContent, complexityResult) {
  const now = new Date().toISOString().split('T')[0];
  
  return `---
title: Complexity Analysis
description: Automated complexity analysis of the task
created: ${now}
last_updated: ${now}
complexity_score: ${complexityResult.score}
complexity_level: ${complexityResult.level}
---

# Complexity Analysis

## Overview

This task has been analyzed and determined to have a **${complexityResult.level}** complexity level with a score of **${complexityResult.score}**.

## Complexity Factors

${complexityResult.factors.map(factor => `- ${factor}`).join('\n')}

## Analysis

${complexityResult.analysis}

## Recommendations

${getRecommendations(complexityResult.level)}

## Task Breakdown

${getTaskBreakdown(complexityResult.level, taskContent)}
`;
}

function getRecommendations(complexityLevel) {
  switch (complexityLevel) {
    case 'high':
      return `Based on the high complexity of this task, it is recommended to:
- Break down this task into smaller subtasks
- Allocate additional time for implementation
- Consider creating a detailed technical design document
- Implement with thorough testing at each stage
- Consider peer review for critical components`;
    
    case 'medium':
      return `Based on the medium complexity of this task, it is recommended to:
- Consider breaking the task into logical components
- Implement with regular testing
- Document key implementation decisions
- Review completed work against requirements`;
    
    case 'low':
      return `Based on the low complexity of this task, it is recommended to:
- Implement directly without additional breakdown
- Use standard testing procedures
- Document any unexpected challenges encountered`;
    
    default:
      return 'No specific recommendations based on complexity.';
  }
}

function getTaskBreakdown(complexityLevel, taskContent) {
  switch (complexityLevel) {
    case 'high':
      return `This task should be divided into smaller subtasks:
1. Research and planning phase
2. Initial implementation of core components
3. Implementation of secondary features
4. Integration with existing systems
5. Testing and refinement
6. Documentation and finalization`;
    
    case 'medium':
      return `This task can be organized into stages:
1. Initial implementation
2. Testing and refinement
3. Documentation and integration`;
    
    case 'low':
      return `This task can be implemented directly without further breakdown.`;
    
    default:
      return 'No breakdown provided.';
  }
}

function extractTaskId(taskContent) {
  // Extract task ID from task content
  const filenameMatch = taskContent.match(/task-([a-zA-Z0-9_-]+)\.md/);
  if (filenameMatch) {
    return filenameMatch[1];
  }
  
  // Fallback to extract from frontmatter
  const idMatch = taskContent.match(/id:\s*([a-zA-Z0-9_-]+)/);
  if (idMatch) {
    return idMatch[1];
  }
  
  // Last resort: generate a random ID
  return `task_${Date.now().toString().slice(-6)}`;
}

function generateThinking(taskContent, complexityAnalysis) {
  // Extract task name
  const titleMatch = taskContent.match(/# Task: (.*)/);
  const taskName = titleMatch ? titleMatch[1] : 'Unknown Task';
  
  // Extract description
  const descriptionMatch = taskContent.match(/## Description\s+([\s\S]*?)(?=##|$)/);
  const description = descriptionMatch ? descriptionMatch[1].trim() : '';
  
  // Extract requirements
  const requirementsMatch = taskContent.match(/## Requirements\s+([\s\S]*?)(?=##|$)/);
  const requirements = requirementsMatch ? requirementsMatch[1].trim() : '';
  
  // Generate thinking steps based on complexity
  const steps = [];
  const subtasks = [];
  let componentCount = 0;
  
  // Initial step
  steps.push({
    step: 1,
    thinking: `Analyzing task "${taskName}" with ${complexityAnalysis.complexityLevel} complexity (score: ${complexityAnalysis.score}).\n\nTask description: ${description}\n\nKey requirements:\n${requirements}`
  });
  
  // Component identification
  if (complexityAnalysis.complexityLevel === 'high' || complexityAnalysis.complexityLevel === 'medium') {
    // Identify components based on requirements
    const reqLines = requirements.split('\n');
    const components = [];
    
    for (const line of reqLines) {
      if (line.trim().startsWith('-') && line.length > 5) {
        components.push(line.trim().substring(2));
      }
    }
    
    componentCount = components.length;
    
    steps.push({
      step: 2,
      thinking: `Identified ${componentCount} main components from the requirements:\n${components.map((c, i) => `${i + 1}. ${c}`).join('\n')}`
    });
    
    // Generate subtasks based on components
    for (let i = 0; i < componentCount; i++) {
      const subtaskName = `Implement ${components[i]}`;
      subtasks.push({
        name: subtaskName,
        description: `Implementation of component: ${components[i]}`,
        priority: 'medium',
        order: i + 1
      });
    }
    
    // Add integration step if high complexity
    if (complexityAnalysis.complexityLevel === 'high') {
      subtasks.push({
        name: 'Integrate components',
        description: 'Integration of all implemented components',
        priority: 'high',
        order: componentCount + 1
      });
      
      subtasks.push({
        name: 'Testing and documentation',
        description: 'Final testing and documentation',
        priority: 'medium',
        order: componentCount + 2
      });
    }
  } else {
    // For low complexity, just add a simple analysis
    steps.push({
      step: 2,
      thinking: 'This task has low complexity and can be implemented directly without further breakdown.'
    });
  }
  
  // Final step
  steps.push({
    step: 3,
    thinking: `Conclusion: ${subtasks.length > 0 ? `This task should be broken down into ${subtasks.length} subtasks.` : 'This task can be implemented directly.'}`
  });
  
  return {
    steps,
    componentCount,
    subtasks
  };
}

function processThinkingSession(thinkingSession) {
  // Extract thinking steps
  const stepsMatch = thinkingSession.match(/step: (\d+)[\s\S]*?thinking: ([\s\S]*?)(?=step:|$)/g);
  
  if (!stepsMatch) {
    return {
      error: 'No thinking steps found in session'
    };
  }
  
  const steps = stepsMatch.map(match => {
    const stepMatch = match.match(/step: (\d+)/);
    const thinkingMatch = match.match(/thinking: ([\s\S]*?)(?=step:|$)/);
    
    return {
      step: stepMatch ? parseInt(stepMatch[1]) : 0,
      thinking: thinkingMatch ? thinkingMatch[1].trim() : ''
    };
  });
  
  // Extract subtasks if mentioned
  const subtasks = [];
  const subtaskPattern = /subtask(?:es)?\s*:?\s*\n([\s\S]*?)(?=\n\n|$)/;
  const subtaskMatch = thinkingSession.match(subtaskPattern);
  
  if (subtaskMatch) {
    const subtaskText = subtaskMatch[1];
    const subtaskLines = subtaskText.split('\n');
    
    for (const line of subtaskLines) {
      if (line.trim().startsWith('-') || line.trim().match(/^\d+\./)) {
        const taskName = line.replace(/^[-\d.\s]+/, '').trim();
        if (taskName) {
          subtasks.push({
            name: taskName,
            description: `Subtask: ${taskName}`,
            priority: 'medium',
            order: subtasks.length + 1
          });
        }
      }
    }
  }
  
  return {
    steps,
    subtasks,
    error: subtasks.length === 0 && steps.length > 0 ? 'No subtasks identified in thinking session' : null
  };
}
```

### Result Formatting

```javascript
function formatResults(commandDef, results, context) {
  // Try to find the final result
  const finalResultId = findFinalResultId(results);
  const finalResult = finalResultId ? results[finalResultId] : null;
  
  // Handle specific command formats
  if (commandDef.name === 'x-command-list') {
    // Return the formatted command list
    return results.format_command_list || 'No commands found.';
  } else if (commandDef.name === 'x-task-list') {
    // Return the formatted task list
    return results.format_task_list || 'No tasks found.';
  } else if (commandDef.name === 'x-session-summarize') {
    // Return success message for session summary
    return `Session summary created for ${context.month}. Summary written to .scaffoldx/xmemory/session_summaries/${context.month}.md`;
  } else if (commandDef.name === 'x-task-create') {
    // Return success message for task creation
    return `Task "${context.taskName}" created successfully with ID: ${context.taskId}`;
  } else if (commandDef.name === 'x-task-complete') {
    // Return success message for task completion
    return `Task ${context.taskId} marked as completed.`;
  } else if (commandDef.name === 'x-feedback-cycle') {
    // Return stage-specific message for feedback cycle
    if (context.stage === '1' || context.stage === 'draft') {
      return `Created draft for ${context.prefix}. Use 'x-feedback-cycle ${context.prefix} --stage 2 --feedback "Your feedback"' to refine.`;
    } else if (context.stage === '2' || context.stage === 'refine') {
      return `Created refinement for ${context.prefix}. Use 'x-feedback-cycle ${context.prefix} --stage 3' to finalize.`;
    } else if (context.stage === '3' || context.stage === 'finalize') {
      return `Finalized ${context.prefix}. Document is now ready for use.`;
    }
  } else if (commandDef.name === 'x-sequential-thinking') {
    // Return message for sequential thinking
    if (context.createSubtasks) {
      return `Sequential thinking completed for task ${context.targetTaskId || 'from session ' + context.sessionId}. Created ${results.create_subtasks?.subtasksCreated || 0} subtasks.`;
    } else {
      return `Sequential thinking completed for task ${context.targetTaskId || 'from session ' + context.sessionId}. Use '--create-subtasks' flag to automatically create subtasks.`;
    }
  } else if (commandDef.name === 'x-analyze-complexity') {
    // Return message for complexity analysis
    return `Complexity analysis completed for task ${context.targetTaskId}. Complexity level: ${results.analyze_complexity?.complexityLevel || 'unknown'} (score: ${results.analyze_complexity?.complexityScore || 0}).`;
  }
  
  // Default output
  if (finalResult && typeof finalResult === 'string') {
    return finalResult;
  } else if (finalResult && typeof finalResult === 'object' && !Array.isArray(finalResult)) {
    if (finalResult.success) {
      return `Command ${commandDef.name} executed successfully.`;
    } else if (finalResult.error) {
      return `Error: ${finalResult.error}`;
    }
  }
  
  return `Command ${commandDef.name} executed.`;
}

function findFinalResultId(results) {
  // Helper to find the likely final result ID
  const resultIds = Object.keys(results);
  
  if (resultIds.length === 0) {
    return null;
  }
  
  // Priority order for result types
  const priorityPatterns = [
    /^format_/,
    /^generate_/,
    /^save_/,
    /^write_/,
    /^update_/,
    /^create_/
  ];
  
  // Check for each pattern
  for (const pattern of priorityPatterns) {
    const match = resultIds.find(id => pattern.test(id));
    if (match) {
      return match;
    }
  }
  
  // Default to the last result
  return resultIds[resultIds.length - 1];
}

// Generate help text
function generateHelpText(commandDef) {
  let help = `# ${commandDef.name}`;
  
  if (commandDef.aliases && commandDef.aliases.length > 0) {
    help += ` (alias: ${commandDef.aliases.join(', ')})`;
  }
  
  help += `\n\n${commandDef.description}\n\n`;
  
  if (commandDef.parsing) {
    help += `## Usage\n\n${commandDef.parsing}\n\n`;
  }
  
  if (commandDef.flags && commandDef.flags.length > 0) {
    help += `## Options\n\n`;
    
    for (const flag of commandDef.flags) {
      help += `- \`--${flag.name}\`${flag.valueFormat ? ` <${flag.valueFormat}>` : ''}: ${flag.description}\n`;
    }
    
    help += '\n';
  }
  
  if (commandDef.examples && commandDef.examples.length > 0) {
    help += `## Examples\n\n`;
    
    for (let i = 0; i < commandDef.examples.length; i++) {
      const example = commandDef.examples[i];
      help += `### Example ${i + 1}:\n\n\`${example.input}\`\n\n${example.output}\n\n`;
    }
  }
  
  return help;
}
```

## Integration with Claude's System

```javascript
// Main entry point function for Claude's system
async function processScaffoldXCommand(input) {
  try {
    // Extract command name and arguments
    const parts = input.split(' ');
    const commandName = parts[0];
    const args = parts.slice(1).join(' ');
    
    // Process the command
    return await processCommand(commandName, args);
  } catch (error) {
    console.error(`Error processing ScaffoldX command: ${error.message}`);
    return `Error processing command: ${error.message}`;
  }
}
```

This enhanced command processor provides a comprehensive, centralized approach to processing all ScaffoldX commands through a single adapter interface. It strictly follows the single-adapter pattern, maintaining the original MDC command files as the single source of truth.

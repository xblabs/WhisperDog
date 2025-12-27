---
title: ScaffoldX MDC Parser for Claude
description: Parser for original MDC command files with Claude-compatible functionality
context_type: adapter
priority: high
last_updated: 2025-05-02
---

# ScaffoldX MDC Parser for Claude

This utility provides a standardized approach for parsing the original Markdown Context (MDC) command files from Cursor and adapting them for Claude's MCP capabilities. It enables seamless interpretation of commands without requiring duplicate definitions.

## Overview

The MDC Parser is designed to:

1. Read command definition files from `.scaffoldx/xcore/commands/`
2. Parse their structure and behavior
3. Extract parameters, flags, and execution logic
4. Generate Claude-compatible execution paths

## Command Structure Parsing

MDC command files in ScaffoldX follow a consistent structure that this parser handles:

1. **YAML Frontmatter**: Metadata about the command
2. **Command Definition**: Title, description, and usage information
3. **Parameter Definitions**: Expected parameters and their formats
4. **Behavior Definition**: Expected command behavior
5. **Examples**: Usage examples and expected outputs

The parser extracts these components to guide execution.

## Command Parsing Functions

### parseMdcCommand(commandPath)

Parses a command definition file and extracts its components.

**Parameters:**
- `commandPath`: Absolute path to the command definition file (string)

**Returns:**
- Command object with parsed components

**Usage Example:**
```javascript
const commandInfo = await parseMdcCommand("C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcore\\commands\\x-session-summarize.md");
```

**Implementation:**
```javascript
async function parseMdcCommand(commandPath) {
  try {
    // Read the command file
    const content = await read_file({ path: normalizePath(commandPath) });
    
    // Parse command components
    const command = {
      name: "",
      alias: "",
      description: "",
      parameters: [],
      flags: [],
      behavior: "",
      examples: []
    };
    
    // Extract command name and alias
    const titleMatch = content.match(/## (x-[\w-]+)(?:\s+\(alias:\s+(x-[\w-]+)\))?/);
    if (titleMatch) {
      command.name = titleMatch[1];
      command.alias = titleMatch[2] || "";
    }
    
    // Extract description
    const descriptionMatch = content.match(/\*\*Description\*\*: (.*?)(?=\n\n|\*\*)/s);
    if (descriptionMatch) {
      command.description = descriptionMatch[1].trim();
    }
    
    // Extract parameters and flags
    const parsingSection = content.match(/\*\*Parsing\*\*:(.*?)(?=\n\n\*\*)/s);
    if (parsingSection) {
      const paramLines = parsingSection[1].split('\n').filter(line => line.trim().startsWith('-'));
      
      for (const line of paramLines) {
        if (line.includes('--')) {
          // This is a flag
          const flagMatch = line.match(/`--(\w+)(?:\s+(\[.*?\]|\<.*?\>))?`/);
          if (flagMatch) {
            command.flags.push({
              name: flagMatch[1],
              valueFormat: flagMatch[2] || null,
              description: line.split(':')[1]?.trim() || ""
            });
          }
        } else {
          // This is a positional parameter
          const paramMatch = line.match(/`([\w-]+)`/);
          if (paramMatch) {
            command.parameters.push({
              name: paramMatch[1],
              description: line.split(':')[1]?.trim() || ""
            });
          }
        }
      }
    }
    
    // Extract behavior
    const behaviorMatch = content.match(/\*\*Behavior\*\*:(.*?)(?=\n\n\*\*)/s);
    if (behaviorMatch) {
      command.behavior = behaviorMatch[1].trim();
    }
    
    // Extract examples
    const examplesSection = content.match(/\*\*Example \d+\*\*:(.*?)(?=\n\n\*\*Example|\n\n-|\n\n$)/sg);
    if (examplesSection) {
      for (const example of examplesSection) {
        const inputMatch = example.match(/Input: `(.*?)`/);
        const outputMatch = example.match(/Output: (.*?)(?=\n\n|$)/s);
        
        if (inputMatch) {
          command.examples.push({
            input: inputMatch[1],
            output: outputMatch ? outputMatch[1].trim() : ""
          });
        }
      }
    }
    
    // Parse frontmatter for additional metadata
    const frontmatterMatch = content.match(/---\n(.*?)\n---/s);
    if (frontmatterMatch) {
      const frontmatterLines = frontmatterMatch[1].split('\n');
      command.metadata = {};
      
      for (const line of frontmatterLines) {
        const parts = line.split(':');
        if (parts.length >= 2) {
          const key = parts[0].trim();
          const value = parts.slice(1).join(':').trim();
          command.metadata[key] = value;
        }
      }
    }
    
    return command;
  } catch (error) {
    console.error(`Error parsing MDC command ${commandPath}: ${error.message}`);
    return null;
  }
}
```

### parseCommandArgs(input, commandInfo)

Parses command arguments from user input based on the command's definition.

**Parameters:**
- `input`: User input containing the command and arguments (string)
- `commandInfo`: Parsed command information from `parseMdcCommand` (object)

**Returns:**
- Object with parsed arguments and flags

**Usage Example:**
```javascript
const args = parseCommandArgs("x-session-summarize --month 2025-04 --custom \"Project kickoff meeting\"", commandInfo);
```

**Implementation:**
```javascript
function parseCommandArgs(input, commandInfo) {
  const result = {
    command: commandInfo.name,
    params: [],
    flags: {}
  };
  
  // Remove the command name from the input
  let argsText = input.replace(new RegExp(`^${commandInfo.name}|^${commandInfo.alias}`, 'i'), '').trim();
  
  // Handle quoted arguments
  const quotedArgs = [];
  const quotedRegex = /"([^"]*)"|'([^']*)'/g;
  let match;
  
  while ((match = quotedRegex.exec(argsText)) !== null) {
    const value = match[1] || match[2];
    quotedArgs.push(value);
    // Replace the quoted section with a placeholder
    argsText = argsText.replace(match[0], `__QUOTED_ARG_${quotedArgs.length - 1}__`);
  }
  
  // Split the remaining args by spaces
  const argParts = argsText.split(/\s+/).filter(part => part.trim().length > 0);
  
  // Process each argument
  let currentParam = 0;
  
  for (let i = 0; i < argParts.length; i++) {
    let part = argParts[i];
    
    // Replace quoted arg placeholders with their values
    if (part.startsWith('__QUOTED_ARG_')) {
      const index = parseInt(part.replace('__QUOTED_ARG_', '').replace('__', ''));
      part = quotedArgs[index];
    }
    
    // Handle flags
    if (part.startsWith('--')) {
      const flagName = part.substring(2);
      
      // Check if this is a boolean flag or has a value
      const flagDef = commandInfo.flags.find(f => f.name === flagName);
      
      if (flagDef && flagDef.valueFormat) {
        // This flag expects a value
        if (i + 1 < argParts.length) {
          let value = argParts[i + 1];
          
          // Replace quoted arg placeholders with their values
          if (value.startsWith('__QUOTED_ARG_')) {
            const index = parseInt(value.replace('__QUOTED_ARG_', '').replace('__', ''));
            value = quotedArgs[index];
          }
          
          result.flags[flagName] = value;
          i++; // Skip the next part since we consumed it as the flag value
        }
      } else {
        // Boolean flag
        result.flags[flagName] = true;
      }
    } else {
      // Positional parameter
      if (currentParam < commandInfo.parameters.length) {
        result.params.push(part);
        currentParam++;
      }
    }
  }
  
  return result;
}
```

### extractCommandNameFromInput(input)

Extracts the command name from user input.

**Parameters:**
- `input`: User input text (string)

**Returns:**
- Command name if found, or null

**Usage Example:**
```javascript
const commandName = extractCommandNameFromInput("x-session-summarize --month 2025-04");
```

**Implementation:**
```javascript
function extractCommandNameFromInput(input) {
  const match = input.match(/^(x-[\w-]+)/);
  return match ? match[1] : null;
}
```

## Natural Language Command Parsing

Claude can extract command parameters from natural language input, providing flexibility beyond strict syntax.

### parseNaturalLanguageCommand(input, commandInfo)

Parses command parameters from natural language descriptions.

**Parameters:**
- `input`: User's natural language input (string)
- `commandInfo`: Parsed command information (object)

**Returns:**
- Object with parsed arguments and flags

**Usage Example:**
```javascript
const args = parseNaturalLanguageCommand(
  "Generate a session summary for April 2025 and include a note about the project kickoff", 
  commandInfo
);
```

**Implementation:**
```javascript
function parseNaturalLanguageCommand(input, commandInfo) {
  const result = {
    command: commandInfo.name,
    params: [],
    flags: {}
  };
  
  // Extract month information
  const monthMatch = input.match(/for\s+(January|February|March|April|May|June|July|August|September|October|November|December)\s+(\d{4})/i);
  if (monthMatch) {
    const month = monthMatch[1];
    const year = monthMatch[2];
    
    // Map month name to number
    const monthNames = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
    const monthIndex = monthNames.findIndex(m => m.toLowerCase() === month.toLowerCase());
    
    if (monthIndex !== -1) {
      const monthNum = String(monthIndex + 1).padStart(2, '0');
      result.flags['month'] = `${year}-${monthNum}`;
    }
  }
  
  // Extract custom notes
  const customNoteMatches = [
    input.match(/include (?:a )?note (?:about|saying) ['""]?(.*?)['""]?(?:\.|\n|$)/i),
    input.match(/with (?:a )?(?:custom )?note ['""]?(.*?)['""]?(?:\.|\n|$)/i),
    input.match(/add (?:the )?(?:custom )?(?:text|note) ['""]?(.*?)['""]?(?:\.|\n|$)/i)
  ];
  
  for (const match of customNoteMatches) {
    if (match) {
      result.flags['custom'] = match[1].trim();
      break;
    }
  }
  
  // Check for append mode
  if (input.match(/append|add to existing|add to current/i)) {
    result.flags['append'] = true;
  }
  
  return result;
}
```

## Command Registry

The command registry tracks all available commands and their aliases.

### buildCommandRegistry()

Builds a registry of all commands and their information.

**Parameters:**
- None

**Returns:**
- Object mapping command names to their information

**Usage Example:**
```javascript
const commandRegistry = await buildCommandRegistry();
```

**Implementation:**
```javascript
async function buildCommandRegistry() {
  const registry = {};
  
  try {
    // Get all command files
    const commandsDir = "C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcore\\commands";
    const files = await list_directory({ path: commandsDir });
    
    const commandFiles = files
      .filter(file => file.type === 'file' && file.name.startsWith('x-') && file.name.endsWith('.md'))
      .map(file => file.name);
    
    // Parse each command
    for (const fileName of commandFiles) {
      const commandPath = `${commandsDir}\\${fileName}`;
      const commandInfo = await parseMdcCommand(commandPath);
      
      if (commandInfo) {
        registry[commandInfo.name] = commandInfo;
        
        // Add alias if present
        if (commandInfo.alias) {
          registry[commandInfo.alias] = commandInfo;
        }
      }
    }
    
    return registry;
  } catch (error) {
    console.error(`Error building command registry: ${error.message}`);
    return {};
  }
}
```

## Usage Flow

The typical flow for using the MDC parser is:

1. Extract the command name from user input
2. Load the command's details from the registry (or parse directly)
3. Parse the command arguments using either strict syntax or natural language
4. Execute the command based on the parsed information

```javascript
async function processCommand(input) {
  // Extract command name
  const commandName = extractCommandNameFromInput(input);
  if (!commandName) {
    return "Not a valid command. Commands must start with 'x-'.";
  }
  
  // Load command registry if not already loaded
  const registry = await buildCommandRegistry();
  
  // Get command info
  const commandInfo = registry[commandName];
  if (!commandInfo) {
    return `Command ${commandName} not found in the registry.`;
  }
  
  // Parse arguments (try both methods)
  let args = parseCommandArgs(input, commandInfo);
  
  // If strict parsing didn't capture much, try natural language parsing
  if (Object.keys(args.flags).length === 0 && args.params.length === 0) {
    const nlArgs = parseNaturalLanguageCommand(input, commandInfo);
    
    // If natural language parsing found more, use it
    if (Object.keys(nlArgs.flags).length > 0 || nlArgs.params.length > 0) {
      args = nlArgs;
    }
  }
  
  // Execute the command with the parsed arguments
  return await executeCommand(commandInfo, args);
}
```

## Integration with Command Execution

This MDC parser is designed to work seamlessly with the other components of the execution adapter layer:

- The **Path Resolution Adapter** converts relative paths in command definitions to absolute paths
- The **Function Call Translator** maps Cursor operations to Claude MCP functions
- The **Error Handling** system provides graceful fallbacks when commands encounter issues
- The **State Persistence** system maintains context between conversations

Together, these components allow ScaffoldX to maintain a single source of truth for commands while adapting to each environment's specific requirements.

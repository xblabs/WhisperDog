---
title: ScaffoldX State Persistence for Claude
description: System for maintaining context and state between conversations
context_type: adapter
priority: high
last_updated: 2025-05-02
---

# ScaffoldX State Persistence for Claude

This utility provides a comprehensive approach for maintaining context and state across multiple conversations in Claude. It enables seamless continuity of command execution and workflow progress between sessions.

## Overview

The State Persistence system is a crucial component of the ScaffoldX Cursor-to-Claude port, serving these key functions:

1. **Session State Storage**: Preserves important state variables between conversations
2. **Context Transition**: Ensures smooth continuation of workflows
3. **Activity Tracking**: Maintains a record of recent activities and outcomes
4. **Preference Persistence**: Stores user preferences and settings
5. **Command History**: Tracks recent command executions for reference

This approach allows the ScaffoldX framework to maintain continuity even with Claude's conversational model where direct state persistence isn't built in.

## State Categories

The system manages several categories of state information:

### 1. Session State

- **Current Task**: Currently active task identifier
- **Current Role**: Active role (e.g., strategic, technical, implementation)
- **Active Project**: Current project context
- **Current Phase**: Current workflow phase (e.g., planning, implementation, review)

### 2. Command State

- **Recent Commands**: List of recently executed commands with parameters
- **Command Output**: Results of significant command executions
- **Pending Operations**: Operations that require multiple steps to complete

### 3. Workflow State

- **Feedback Cycles**: Active feedback cycles and their current state
- **Review Status**: Status of document reviews in progress
- **Draft Versions**: Version tracking for documents being edited

### 4. User Preferences

- **Output Format**: Preferred output formats for command results
- **Detail Level**: Preferred verbosity level for responses
- **Working Directories**: Frequently used or preferred directories

## State Persistence Functions

### saveState(stateData, category)

Saves state data to a file for persistence between conversations.

**Parameters:**
- `stateData`: Object containing state data to save
- `category`: Category name for the state data (string)

**Returns:**
- Success status (boolean)

**Usage Example:**
```javascript
const taskState = {
  currentTask: 'implement_authentication',
  lastUpdate: new Date().toISOString(),
  completedSubtasks: ['define_authentication_flow', 'setup_database_schema']
};

const success = await saveState(taskState, 'task');
```

**Implementation:**
```javascript
async function saveState(stateData, category) {
  if (!stateData || !category) {
    return false;
  }
  
  try {
    // Ensure state directory exists
    const stateDir = "C:\\__dev\\_projects\\ScaffoldX\\.claude\\state";
    await create_directory({ path: stateDir });
    
    // Define file path for the state category
    const statePath = `${stateDir}\\${category}_state.json`;
    
    // Add timestamp to state data
    const stateWithTimestamp = {
      ...stateData,
      _lastUpdated: new Date().toISOString(),
      _category: category
    };
    
    // Convert to JSON
    const stateJson = JSON.stringify(stateWithTimestamp, null, 2);
    
    // Save to file
    await write_file({ path: statePath, content: stateJson });
    
    return true;
  } catch (error) {
    console.error(`Error saving state for category ${category}: ${error.message}`);
    return false;
  }
}
```

### loadState(category)

Loads state data from a file.

**Parameters:**
- `category`: Category name for the state data (string)

**Returns:**
- Loaded state data (object) or null if not found

**Usage Example:**
```javascript
const taskState = await loadState('task');
if (taskState) {
  console.log(`Current task: ${taskState.currentTask}`);
}
```

**Implementation:**
```javascript
async function loadState(category) {
  if (!category) {
    return null;
  }
  
  try {
    // Define file path for the state category
    const stateDir = "C:\\__dev\\_projects\\ScaffoldX\\.claude\\state";
    const statePath = `${stateDir}\\${category}_state.json`;
    
    // Check if file exists
    try {
      await get_file_info({ path: statePath });
    } catch (error) {
      // File doesn't exist
      return null;
    }
    
    // Read file
    const stateJson = await read_file({ path: statePath });
    
    // Parse JSON
    return JSON.parse(stateJson);
  } catch (error) {
    console.error(`Error loading state for category ${category}: ${error.message}`);
    return null;
  }
}
```

### updateState(updates, category)

Updates specific fields in state data without replacing the entire state.

**Parameters:**
- `updates`: Object containing fields to update
- `category`: Category name for the state data (string)

**Returns:**
- Updated state data (object) or null if failed

**Usage Example:**
```javascript
const updates = {
  currentTask: 'implement_login_form',
  lastAction: 'created login component'
};

const updatedState = await updateState(updates, 'task');
```

**Implementation:**
```javascript
async function updateState(updates, category) {
  if (!updates || !category) {
    return null;
  }
  
  try {
    // Load existing state
    const existingState = await loadState(category) || {};
    
    // Merge with updates
    const updatedState = {
      ...existingState,
      ...updates,
      _lastUpdated: new Date().toISOString()
    };
    
    // Save updated state
    const success = await saveState(updatedState, category);
    
    return success ? updatedState : null;
  } catch (error) {
    console.error(`Error updating state for category ${category}: ${error.message}`);
    return null;
  }
}
```

### clearState(category)

Clears state data for a specific category.

**Parameters:**
- `category`: Category name for the state data (string)

**Returns:**
- Success status (boolean)

**Usage Example:**
```javascript
const success = await clearState('task');
if (success) {
  console.log('Task state cleared');
}
```

**Implementation:**
```javascript
async function clearState(category) {
  if (!category) {
    return false;
  }
  
  try {
    // Define file path for the state category
    const stateDir = "C:\\__dev\\_projects\\ScaffoldX\\.claude\\state";
    const statePath = `${stateDir}\\${category}_state.json`;
    
    // Create empty state
    const emptyState = {
      _cleared: new Date().toISOString(),
      _category: category
    };
    
    // Save empty state
    await write_file({ path: statePath, content: JSON.stringify(emptyState, null, 2) });
    
    return true;
  } catch (error) {
    console.error(`Error clearing state for category ${category}: ${error.message}`);
    return false;
  }
}
```

## Session Context Management

### startSession()

Initializes a new session context and loads relevant state.

**Parameters:**
- None

**Returns:**
- Session context object (object)

**Usage Example:**
```javascript
const session = await startSession();
console.log(`Current task: ${session.currentTask}`);
```

**Implementation:**
```javascript
async function startSession() {
  try {
    // Initialize session context
    const session = {
      id: `session-${Date.now().toString(36)}`,
      startTime: new Date().toISOString(),
      currentTask: null,
      currentRole: null,
      commandHistory: [],
      workflowState: {},
      initialized: false
    };
    
    // Load task state
    const taskState = await loadState('task');
    if (taskState) {
      session.currentTask = taskState.currentTask;
    }
    
    // Load role state
    const roleState = await loadState('role');
    if (roleState) {
      session.currentRole = roleState.currentRole;
    }
    
    // Load workflow state
    const workflowState = await loadState('workflow');
    if (workflowState) {
      session.workflowState = workflowState;
    }
    
    // Load command history
    const commandState = await loadState('command');
    if (commandState && commandState.history) {
      session.commandHistory = commandState.history;
    }
    
    // Log session start
    await logSessionActivity('start', session);
    
    // Mark as initialized
    session.initialized = true;
    
    return session;
  } catch (error) {
    console.error(`Error starting session: ${error.message}`);
    
    // Return minimal session in case of error
    return {
      id: `session-${Date.now().toString(36)}`,
      startTime: new Date().toISOString(),
      initialized: false,
      error: error.message
    };
  }
}
```

### endSession(session)

Finishes a session and saves its state.

**Parameters:**
- `session`: Session context object (object)

**Returns:**
- Success status (boolean)

**Usage Example:**
```javascript
const session = await startSession();
// Perform operations...
const success = await endSession(session);
```

**Implementation:**
```javascript
async function endSession(session) {
  if (!session) {
    return false;
  }
  
  try {
    // Update session end time
    session.endTime = new Date().toISOString();
    
    // Save various state categories
    if (session.currentTask) {
      await updateState({ currentTask: session.currentTask }, 'task');
    }
    
    if (session.currentRole) {
      await updateState({ currentRole: session.currentRole }, 'role');
    }
    
    if (session.commandHistory && session.commandHistory.length > 0) {
      // Keep only the most recent commands (limit to 20)
      const recentCommands = session.commandHistory.slice(-20);
      await updateState({ history: recentCommands }, 'command');
    }
    
    if (session.workflowState && Object.keys(session.workflowState).length > 0) {
      await updateState(session.workflowState, 'workflow');
    }
    
    // Log session end
    await logSessionActivity('end', session);
    
    return true;
  } catch (error) {
    console.error(`Error ending session: ${error.message}`);
    return false;
  }
}
```

### logSessionActivity(activityType, context)

Logs session activity for tracking and debugging.

**Parameters:**
- `activityType`: Type of activity being logged (string)
- `context`: Additional context about the activity (object)

**Returns:**
- Success status (boolean)

**Usage Example:**
```javascript
const context = {
  command: 'x-task-create',
  args: { taskName: 'Implement login form', priority: 'high' }
};

await logSessionActivity('command_execution', context);
```

**Implementation:**
```javascript
async function logSessionActivity(activityType, context) {
  if (!activityType) {
    return false;
  }
  
  try {
    // Ensure log directory exists
    const logDir = "C:\\__dev\\_projects\\ScaffoldX\\.claude\\logs";
    await create_directory({ path: logDir });
    
    // Define log file path (daily log)
    const today = new Date().toISOString().split('T')[0];
    const logPath = `${logDir}\\${today}_activity.md`;
    
    // Format activity log
    const timestamp = new Date().toISOString();
    const logEntry = `### ${timestamp} - ${activityType}\n\n${JSON.stringify(context, null, 2)}\n\n---\n\n`;
    
    // Check if log file exists
    let existingContent = '';
    try {
      existingContent = await read_file({ path: logPath });
    } catch (error) {
      // File doesn't exist yet, create header
      existingContent = `# ScaffoldX Session Activity Log - ${today}\n\n`;
    }
    
    // Append log entry
    const updatedContent = existingContent + logEntry;
    await write_file({ path: logPath, content: updatedContent });
    
    return true;
  } catch (error) {
    console.error(`Error logging session activity: ${error.message}`);
    return false;
  }
}
```

## Command History Management

### recordCommandExecution(command, args, result)

Records a command execution for history tracking.

**Parameters:**
- `command`: Command name (string)
- `args`: Command arguments (object)
- `result`: Command execution result (string or object)

**Returns:**
- Success status (boolean)

**Usage Example:**
```javascript
const result = await executeCommand('x-task-create', { taskName: 'Implement login form' });
await recordCommandExecution('x-task-create', { taskName: 'Implement login form' }, result);
```

**Implementation:**
```javascript
async function recordCommandExecution(command, args, result) {
  if (!command) {
    return false;
  }
  
  try {
    // Load existing command history
    const commandState = await loadState('command') || { history: [] };
    
    // Create command record
    const commandRecord = {
      command: command,
      timestamp: new Date().toISOString(),
      args: args || {},
      resultSummary: typeof result === 'string' ? result.substring(0, 100) : 'Complex result',
      status: result ? 'success' : 'failure'
    };
    
    // Add to history
    if (!commandState.history) {
      commandState.history = [];
    }
    
    commandState.history.push(commandRecord);
    
    // Keep only the most recent commands (limit to 20)
    if (commandState.history.length > 20) {
      commandState.history = commandState.history.slice(-20);
    }
    
    // Save updated history
    await saveState(commandState, 'command');
    
    return true;
  } catch (error) {
    console.error(`Error recording command execution: ${error.message}`);
    return false;
  }
}
```

### getRecentCommands(limit = 10)

Retrieves recent command executions from history.

**Parameters:**
- `limit`: Maximum number of commands to retrieve (number, default: 10)

**Returns:**
- Array of recent command records (array)

**Usage Example:**
```javascript
const recentCommands = await getRecentCommands(5);
console.log('Recently executed commands:');
recentCommands.forEach(cmd => {
  console.log(`${cmd.timestamp} - ${cmd.command}`);
});
```

**Implementation:**
```javascript
async function getRecentCommands(limit = 10) {
  try {
    // Load command history
    const commandState = await loadState('command');
    
    // Return empty array if no history
    if (!commandState || !commandState.history) {
      return [];
    }
    
    // Get the most recent commands
    return commandState.history.slice(-limit).reverse();
  } catch (error) {
    console.error(`Error getting recent commands: ${error.message}`);
    return [];
  }
}
```

## Task Context Management

### setCurrentTask(taskId)

Sets the current active task for context tracking.

**Parameters:**
- `taskId`: Task identifier (string)

**Returns:**
- Success status (boolean)

**Usage Example:**
```javascript
const success = await setCurrentTask('implement_authentication');
if (success) {
  console.log('Current task set to: implement_authentication');
}
```

**Implementation:**
```javascript
async function setCurrentTask(taskId) {
  if (!taskId) {
    return false;
  }
  
  try {
    // Load task details to validate task exists
    const taskPath = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${taskId}\\task-${taskId}.md`;
    
    try {
      await get_file_info({ path: taskPath });
    } catch (error) {
      console.error(`Task not found: ${taskId}`);
      return false;
    }
    
    // Update task state
    await updateState({ 
      currentTask: taskId,
      taskSwitchTime: new Date().toISOString()
    }, 'task');
    
    // Log task switch
    await logSessionActivity('task_switch', { taskId: taskId });
    
    return true;
  } catch (error) {
    console.error(`Error setting current task: ${error.message}`);
    return false;
  }
}
```

### getCurrentTask()

Gets the current active task and its context.

**Parameters:**
- None

**Returns:**
- Current task object (object) or null if not set

**Usage Example:**
```javascript
const task = await getCurrentTask();
if (task) {
  console.log(`Current task: ${task.id} - ${task.name}`);
}
```

**Implementation:**
```javascript
async function getCurrentTask() {
  try {
    // Load task state
    const taskState = await loadState('task');
    
    // Return null if no current task
    if (!taskState || !taskState.currentTask) {
      return null;
    }
    
    const taskId = taskState.currentTask;
    
    // Load task details
    const taskPath = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${taskId}\\task-${taskId}.md`;
    
    try {
      const taskContent = await read_file({ path: taskPath });
      
      // Extract task metadata using a simple parser
      // This is a simplified approach and would be more robust in practice
      const nameMatch = taskContent.match(/# Task: ([^\n]+)/);
      const priorityMatch = taskContent.match(/Priority: ([^\n]+)/);
      const statusMatch = taskContent.match(/Status: ([^\n]+)/);
      
      return {
        id: taskId,
        name: nameMatch ? nameMatch[1].trim() : taskId,
        priority: priorityMatch ? priorityMatch[1].trim() : 'medium',
        status: statusMatch ? statusMatch[1].trim() : 'in_progress',
        path: taskPath,
        lastSwitchTime: taskState.taskSwitchTime
      };
    } catch (error) {
      console.error(`Error loading task details: ${error.message}`);
      
      // Return minimal task info
      return {
        id: taskId,
        path: taskPath,
        lastSwitchTime: taskState.taskSwitchTime,
        error: 'Failed to load task details'
      };
    }
  } catch (error) {
    console.error(`Error getting current task: ${error.message}`);
    return null;
  }
}
```

## Role Context Management

### setCurrentRole(role)

Sets the current active role for context tracking.

**Parameters:**
- `role`: Role identifier (string)

**Returns:**
- Success status (boolean)

**Usage Example:**
```javascript
const success = await setCurrentRole('technical');
if (success) {
  console.log('Current role set to: technical');
}
```

**Implementation:**
```javascript
async function setCurrentRole(role) {
  if (!role) {
    return false;
  }
  
  // Validate role
  const validRoles = ['strategic', 'technical', 'implementation', 'none'];
  if (!validRoles.includes(role.toLowerCase())) {
    console.error(`Invalid role: ${role}`);
    return false;
  }
  
  try {
    // Update role state
    await updateState({ 
      currentRole: role.toLowerCase(),
      roleSwitchTime: new Date().toISOString()
    }, 'role');
    
    // Log role switch
    await logSessionActivity('role_switch', { role: role.toLowerCase() });
    
    return true;
  } catch (error) {
    console.error(`Error setting current role: ${error.message}`);
    return false;
  }
}
```

### getCurrentRole()

Gets the current active role.

**Parameters:**
- None

**Returns:**
- Current role object (object) or null if not set

**Usage Example:**
```javascript
const role = await getCurrentRole();
if (role) {
  console.log(`Current role: ${role.name}`);
}
```

**Implementation:**
```javascript
async function getCurrentRole() {
  try {
    // Load role state
    const roleState = await loadState('role');
    
    // Return null if no current role
    if (!roleState || !roleState.currentRole) {
      return null;
    }
    
    const roleName = roleState.currentRole;
    
    // Return role context
    return {
      name: roleName,
      description: getRoleDescription(roleName),
      lastSwitchTime: roleState.roleSwitchTime
    };
  } catch (error) {
    console.error(`Error getting current role: ${error.message}`);
    return null;
  }
}

// Helper function to get role description
function getRoleDescription(roleName) {
  const roleDescriptions = {
    strategic: 'Focuses on project goals, business value, and high-level planning.',
    technical: 'Focuses on architecture, technical design, and technology choices.',
    implementation: 'Focuses on code implementation, testing, and deployment.',
    none: 'No specific role perspective active.'
  };
  
  return roleDescriptions[roleName.toLowerCase()] || 'Unknown role';
}
```

## Workflow State Management

### saveWorkflowState(workflow, state)

Saves the state of a specific workflow.

**Parameters:**
- `workflow`: Workflow identifier (string)
- `state`: Workflow state data (object)

**Returns:**
- Success status (boolean)

**Usage Example:**
```javascript
const feedbackState = {
  phase: 'refinement',
  iterations: 2,
  lastFeedback: 'Improve error handling',
  documentPath: '.scaffoldx/xcontext/01_prd.md'
};

const success = await saveWorkflowState('feedback_cycle', feedbackState);
```

**Implementation:**
```javascript
async function saveWorkflowState(workflow, state) {
  if (!workflow || !state) {
    return false;
  }
  
  try {
    // Load existing workflow state
    const workflowState = await loadState('workflow') || {};
    
    // Update with new state for the specific workflow
    workflowState[workflow] = {
      ...state,
      lastUpdated: new Date().toISOString()
    };
    
    // Save updated workflow state
    await saveState(workflowState, 'workflow');
    
    // Log workflow state update
    await logSessionActivity('workflow_update', { 
      workflow: workflow, 
      state: { 
        ...state,
        lastUpdated: new Date().toISOString() 
      } 
    });
    
    return true;
  } catch (error) {
    console.error(`Error saving workflow state: ${error.message}`);
    return false;
  }
}
```

### getWorkflowState(workflow)

Gets the state of a specific workflow.

**Parameters:**
- `workflow`: Workflow identifier (string)

**Returns:**
- Workflow state data (object) or null if not found

**Usage Example:**
```javascript
const feedbackState = await getWorkflowState('feedback_cycle');
if (feedbackState) {
  console.log(`Current feedback phase: ${feedbackState.phase}`);
}
```

**Implementation:**
```javascript
async function getWorkflowState(workflow) {
  if (!workflow) {
    return null;
  }
  
  try {
    // Load workflow state
    const workflowState = await loadState('workflow');
    
    // Return null if no workflow state or specific workflow not found
    if (!workflowState || !workflowState[workflow]) {
      return null;
    }
    
    return workflowState[workflow];
  } catch (error) {
    console.error(`Error getting workflow state: ${error.message}`);
    return null;
  }
}
```

### clearWorkflowState(workflow)

Clears the state of a specific workflow.

**Parameters:**
- `workflow`: Workflow identifier (string)

**Returns:**
- Success status (boolean)

**Usage Example:**
```javascript
const success = await clearWorkflowState('feedback_cycle');
if (success) {
  console.log('Feedback cycle workflow state cleared');
}
```

**Implementation:**
```javascript
async function clearWorkflowState(workflow) {
  if (!workflow) {
    return false;
  }
  
  try {
    // Load existing workflow state
    const workflowState = await loadState('workflow') || {};
    
    // Remove the specific workflow
    if (workflowState[workflow]) {
      delete workflowState[workflow];
      
      // Save updated workflow state
      await saveState(workflowState, 'workflow');
      
      // Log workflow state clear
      await logSessionActivity('workflow_clear', { workflow: workflow });
      
      return true;
    }
    
    return false;
  } catch (error) {
    console.error(`Error clearing workflow state: ${error.message}`);
    return false;
  }
}
```

## Integration with the Execution Adapter

The state persistence system works seamlessly with the other components of the execution adapter layer:

- The **MDC Parser** leverages session context for command parsing
- The **Path Resolution Adapter** benefits from context-aware path resolution
- The **Function Call Translator** uses workflow state for operation execution
- The **Error Handling** system recovers workflow state after errors

Together, these components enable continuous workflow execution in the Claude environment without losing context between conversations.

## Usage in Conversation Lifecycle

The state persistence system is designed to be integrated into the conversation lifecycle:

```javascript
// At the beginning of a conversation
async function initializeConversation() {
  // Start a new session and load context
  const session = await startSession();
  
  // Get current task context
  const currentTask = await getCurrentTask();
  
  // Get current role context
  const currentRole = await getCurrentRole();
  
  // Get active workflows
  const feedbackState = await getWorkflowState('feedback_cycle');
  
  // Return context for the conversation
  return {
    session: session,
    currentTask: currentTask,
    currentRole: currentRole,
    activeWorkflows: {
      feedbackCycle: feedbackState
    }
  };
}

// During command execution
async function executeCommandWithState(command, args) {
  // Execute the command
  const result = await executeCommand(command, args);
  
  // Record the execution in history
  await recordCommandExecution(command, args, result);
  
  // Update workflow state if needed
  if (command === 'x-feedback-cycle') {
    const phase = args.phase || 'start';
    
    // Update feedback cycle workflow state
    await saveWorkflowState('feedback_cycle', {
      phase: phase,
      documentType: args.type,
      documentPath: args.path,
      iteration: args.iteration || 1
    });
  }
  
  return result;
}

// At the end of a conversation
async function finalizeConversation(session) {
  // End the session and save context
  await endSession(session);
}
```

This approach to state persistence ensures that ScaffoldX can maintain conversational continuity and workflow state across multiple interactions with Claude.

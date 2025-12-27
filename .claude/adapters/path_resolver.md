---
title: ScaffoldX Path Resolution Adapter for Claude
description: Utility for converting relative paths to absolute paths for Claude MCP compatibility
context_type: adapter
priority: high
last_updated: 2025-05-02
---

# ScaffoldX Path Resolution Adapter for Claude

This utility provides a standardized approach for resolving file paths in ScaffoldX, ensuring compatibility with Claude's MCP capabilities. It converts relative paths to absolute paths and handles various path formats consistently.

## Overview

The Path Resolution Adapter solves a key challenge in porting ScaffoldX from Cursor to Claude: path handling. While Cursor can work with relative paths, Claude's MCP requires absolute paths for file operations. This adapter:

1. Detects different path formats (relative, absolute, tilde-prefixed)
2. Converts paths to standardized absolute paths
3. Handles path normalization and validation
4. Provides utilities for common path operations

## Core Path Resolution Functions

### resolvePath(path, basePath = null)

Resolves a path to an absolute path, using various resolution strategies.

**Parameters:**
- `path`: Path to resolve (string)
- `basePath`: Optional base path for relative paths (string, default: project root)

**Returns:**
- Resolved absolute path (string)

**Usage Example:**
```javascript
const absolutePath = resolvePath(".scaffoldx/xcontext/01_prd.md");
// Returns: "C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcontext\\01_prd.md"
```

**Implementation:**
```javascript
function resolvePath(path, basePath = null) {
  // Handle null, undefined, or empty paths
  if (!path) {
    return null;
  }
  
  // Normalize path separators to backslashes for Windows
  let normalizedPath = path.replace(/\//g, '\\');
  
  // If already an absolute path, just normalize it
  if (isAbsolutePath(normalizedPath)) {
    return normalizePath(normalizedPath);
  }
  
  // Set default base path if not provided
  if (!basePath) {
    basePath = "C:\\__dev\\_projects\\ScaffoldX";
  }
  
  // Handle paths starting with .scaffoldx, which are relative to project root
  if (normalizedPath.startsWith('.scaffoldx\\') || normalizedPath.startsWith('scaffoldx\\')) {
    return joinPaths(basePath, normalizedPath);
  }
  
  // Handle paths starting with .claude, which are relative to project root
  if (normalizedPath.startsWith('.claude\\') || normalizedPath.startsWith('claude\\')) {
    return joinPaths(basePath, normalizedPath);
  }
  
  // Handle other relative paths (starting with ./ or ../)
  if (normalizedPath.startsWith('.\\') || normalizedPath.startsWith('..\\')) {
    return joinPaths(basePath, normalizedPath);
  }
  
  // Handle tilde paths (~/...)
  if (normalizedPath.startsWith('~\\')) {
    // Replace ~ with user home directory for Windows
    return joinPaths("C:\\Users\\username", normalizedPath.substring(2));
  }
  
  // For paths that don't match any of the above patterns,
  // assume they're relative to the project root
  return joinPaths(basePath, normalizedPath);
}
```

### isAbsolutePath(path)

Checks if a path is absolute.

**Parameters:**
- `path`: Path to check (string)

**Returns:**
- Boolean indicating if the path is absolute

**Usage Example:**
```javascript
const isAbsolute = isAbsolutePath("C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx");
// Returns: true
```

**Implementation:**
```javascript
function isAbsolutePath(path) {
  // Check for Windows style absolute paths (C:\, D:\, etc.)
  if (/^[A-Za-z]:\\/.test(path)) {
    return true;
  }
  
  // Check for UNC paths (\\server\share)
  if (/^\\\\[^\\]+\\[^\\]+/.test(path)) {
    return true;
  }
  
  return false;
}
```

### normalizePath(path)

Normalizes a path by removing duplicate separators and resolving dot segments.

**Parameters:**
- `path`: Path to normalize (string)

**Returns:**
- Normalized path (string)

**Usage Example:**
```javascript
const normalized = normalizePath("C:\\__dev\\_projects\\ScaffoldX\\.\\xcontext\\..\\xcontext\\01_prd.md");
// Returns: "C:\\__dev\\_projects\\ScaffoldX\\xcontext\\01_prd.md"
```

**Implementation:**
```javascript
function normalizePath(path) {
  if (!path) {
    return path;
  }
  
  // Replace forward slashes with backslashes
  let normalized = path.replace(/\//g, '\\');
  
  // Remove duplicate backslashes (except at the start which could be a network path)
  normalized = normalized.replace(/([^\\])\\\\+/g, '$1\\');
  
  // Handle leading double backslashes for UNC paths
  if (normalized.startsWith('\\\\')) {
    normalized = '\\\\' + normalized.substring(2).replace(/\\\\+/g, '\\');
  }
  
  // Resolve dot segments (., ..)
  const parts = normalized.split('\\');
  const resolvedParts = [];
  
  for (const part of parts) {
    if (part === '.') {
      // Skip this segment
      continue;
    } else if (part === '..') {
      // Go up one directory (if possible)
      if (resolvedParts.length > 0 && resolvedParts[resolvedParts.length - 1] !== '..') {
        resolvedParts.pop();
      } else {
        // We're already at the root or this is a relative path that goes above the root
        resolvedParts.push('..');
      }
    } else if (part !== '') {
      // Regular path segment
      resolvedParts.push(part);
    }
  }
  
  // Reconstruct the path
  let result = resolvedParts.join('\\');
  
  // Ensure drive letter is capitalized for Windows paths
  if (/^[a-z]:/i.test(result)) {
    result = result.charAt(0).toUpperCase() + result.substring(1);
  }
  
  return result;
}
```

### joinPaths(...paths)

Joins path segments into a complete path.

**Parameters:**
- `...paths`: Path segments (rest parameter)

**Returns:**
- Joined and normalized path (string)

**Usage Example:**
```javascript
const fullPath = joinPaths("C:\\__dev\\_projects\\ScaffoldX", ".scaffoldx", "xcontext", "01_prd.md");
// Returns: "C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcontext\\01_prd.md"
```

**Implementation:**
```javascript
function joinPaths(...paths) {
  if (paths.length === 0) {
    return '';
  }
  
  // Filter out empty segments
  const filteredPaths = paths.filter(path => path && path.length > 0);
  
  if (filteredPaths.length === 0) {
    return '';
  }
  
  // Join the paths with backslashes
  const joined = filteredPaths.join('\\');
  
  // Normalize the resulting path
  return normalizePath(joined);
}
```

### getDirectoryPath(filePath)

Gets the directory part of a file path.

**Parameters:**
- `filePath`: Path to a file (string)

**Returns:**
- Directory path (string)

**Usage Example:**
```javascript
const dirPath = getDirectoryPath("C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcontext\\01_prd.md");
// Returns: "C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcontext"
```

**Implementation:**
```javascript
function getDirectoryPath(filePath) {
  if (!filePath) {
    return null;
  }
  
  const normalized = normalizePath(filePath);
  const lastSeparatorIndex = normalized.lastIndexOf('\\');
  
  if (lastSeparatorIndex === -1) {
    return normalized; // No separator, return as is
  }
  
  return normalized.substring(0, lastSeparatorIndex);
}
```

### getFileName(filePath)

Gets the file name from a path.

**Parameters:**
- `filePath`: Path to a file (string)

**Returns:**
- File name (string)

**Usage Example:**
```javascript
const fileName = getFileName("C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xcontext\\01_prd.md");
// Returns: "01_prd.md"
```

**Implementation:**
```javascript
function getFileName(filePath) {
  if (!filePath) {
    return null;
  }
  
  const normalized = normalizePath(filePath);
  const lastSeparatorIndex = normalized.lastIndexOf('\\');
  
  if (lastSeparatorIndex === -1) {
    return normalized; // No separator, return as is
  }
  
  return normalized.substring(lastSeparatorIndex + 1);
}
```

## Path Resolution in Markdown Content

The adapter also provides utilities for resolving paths in markdown content, which is essential for adapting command documentation.

### resolvePathsInMarkdown(content, basePath = null)

Resolves relative paths in markdown content to absolute paths.

**Parameters:**
- `content`: Markdown content (string)
- `basePath`: Optional base path for relative paths (string, default: project root)

**Returns:**
- Markdown content with resolved paths (string)

**Usage Example:**
```javascript
const resolvedContent = resolvePathsInMarkdown(commandDefinition, "C:\\__dev\\_projects\\ScaffoldX");
```

**Implementation:**
```javascript
function resolvePathsInMarkdown(content, basePath = null) {
  if (!content) {
    return content;
  }
  
  // Regular expression to match markdown links and image references
  const linkRegex = /\[([^\]]+)\]\(([^)]+)\)/g;
  
  // Replace links with resolved paths
  const resolvedContent = content.replace(linkRegex, (match, text, path) => {
    // Don't resolve URLs or anchor links
    if (path.startsWith('http') || path.startsWith('#')) {
      return match;
    }
    
    const resolvedPath = resolvePath(path, basePath);
    return `[${text}](${resolvedPath})`;
  });
  
  // Regular expression to match file references in code blocks
  const codeRegex = /```(?:javascript|js).*?(?:require|import|read).*?['"](.+?)['"]/gs;
  
  // Replace file references in code blocks with resolved paths
  const resolvedCode = resolvedContent.replace(codeRegex, (match, path) => {
    // Don't resolve node module paths
    if (!path.startsWith('.') && !path.startsWith('/')) {
      return match;
    }
    
    const resolvedPath = resolvePath(path, basePath);
    return match.replace(path, resolvedPath);
  });
  
  return resolvedCode;
}
```

### resolveScaffoldxPaths(content)

Specifically resolves ScaffoldX-specific path patterns in text content.

**Parameters:**
- `content`: Text content (string)

**Returns:**
- Content with resolved ScaffoldX paths (string)

**Usage Example:**
```javascript
const resolvedBehavior = resolveScaffoldxPaths(commandInfo.behavior);
```

**Implementation:**
```javascript
function resolveScaffoldxPaths(content) {
  if (!content) {
    return content;
  }
  
  // Regular expression to match common ScaffoldX path patterns
  const scaffoldxPathRegex = /\.scaffoldx\/([a-zA-Z_]+)\/([^.\s,):'"]+)/g;
  
  // Replace with absolute paths
  return content.replace(scaffoldxPathRegex, (match, folder, rest) => {
    const absolutePath = resolvePath(match);
    return absolutePath;
  });
}
```

## Path Resolution in Command Execution

When executing commands, the adapter ensures all file operations use properly resolved paths.

### resolveCommandPaths(commandInfo)

Resolves paths in a parsed command's information.

**Parameters:**
- `commandInfo`: Parsed command information object

**Returns:**
- Command information with resolved paths

**Usage Example:**
```javascript
const resolvedCommand = resolveCommandPaths(commandInfo);
```

**Implementation:**
```javascript
function resolveCommandPaths(commandInfo) {
  if (!commandInfo) {
    return commandInfo;
  }
  
  // Create a deep copy to avoid modifying the original
  const resolved = JSON.parse(JSON.stringify(commandInfo));
  
  // Resolve paths in behavior description
  if (resolved.behavior) {
    resolved.behavior = resolveScaffoldxPaths(resolved.behavior);
  }
  
  // Resolve paths in examples
  if (resolved.examples && Array.isArray(resolved.examples)) {
    for (let i = 0; i < resolved.examples.length; i++) {
      if (resolved.examples[i].output) {
        resolved.examples[i].output = resolveScaffoldxPaths(resolved.examples[i].output);
      }
    }
  }
  
  return resolved;
}
```

## Integration with the Execution Adapter

This path resolution adapter works hand-in-hand with the other components of the execution adapter layer:

- The **MDC Parser** passes extracted command information to be path-resolved
- The **Function Call Translator** uses resolved paths when mapping to MCP functions
- The **Error Handling** system benefits from consistent path formats
- The **State Persistence** system stores paths in a consistent format

Together, these components enable seamless execution of commands in the Claude environment while maintaining compatibility with the original ScaffoldX command definitions.

## Important Notes

1. **Windows Path Format**: This adapter standardizes on Windows backslash path format for compatibility with the project structure.

2. **Project Root**: The default base path is set to `C:\\__dev\\_projects\\ScaffoldX`, which is the project root for this implementation.

3. **Path Validation**: While the adapter focuses on path resolution, actual file existence should be verified before operations.

4. **Forward Slash Normalization**: Forward slashes in paths are automatically converted to backslashes for consistency.

5. **Absolute Path Preservation**: Already absolute paths are normalized but otherwise preserved.

## Implementation Considerations

When implementing file operations in Claude with MCP, always use the path resolution adapter to ensure consistent path handling:

```javascript
// Example: Reading a file from a ScaffoldX path
const filePath = resolvePath(".scaffoldx/xcontext/01_prd.md");
const content = await read_file({ path: filePath });

// Example: Writing to a file with a joined path
const outputDir = resolvePath(".scaffoldx/xmemory/session_summaries");
const fileName = `${getCurrentMonth()}.md`;
const outputPath = joinPaths(outputDir, fileName);
await write_file({ path: outputPath, content: summaryContent });
```

This consistent approach to path handling will ensure reliable file operations across the ScaffoldX framework in Claude.

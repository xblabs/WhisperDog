---
title: Analyze Complexity Command Handler for Claude
description: Handler for the x-analyze-complexity command in the centralized adapter pattern
context_type: adapter_handler
priority: high
last_updated: 2025-05-02
---

# Analyze Complexity Command Handler

This handler implements the `x-analyze-complexity` command for the centralized adapter pattern in Claude. It analyzes the complexity and AI autonomy potential of tasks and code.

## Command Processing

```javascript
/**
 * Process the x-analyze-complexity command
 * @param {Object} args - Command arguments
 * @returns {Promise<string>} - Command output
 */
async function processAnalyzeComplexityCommand(args) {
  try {
    // Initialize result
    let result = {
      taskDescription: '',
      taskId: null,
      filePath: null,
      dirPath: null,
      analysisType: 'complexity', // Default to complexity analysis
      format: 'markdown', // Default to markdown output
      saveResult: false,
      detailed: false,
      metrics: null,
      aiAutonomy: false,
      autonomyOnly: false,
      aggregate: false
    };
    
    // Process arguments
    if (args._ && args._[0] && !args.task && !args.file && !args.dir) {
      // If first argument isn't a flag, it's the task description
      result.taskDescription = args._[0];
    }
    
    // Process flags
    if (args.task) {
      result.taskId = args.task;
    }
    
    if (args.file) {
      result.filePath = args.file;
    }
    
    if (args.dir) {
      result.dirPath = args.dir;
    }
    
    if (args.save) {
      result.saveResult = true;
    }
    
    if (args.format) {
      result.format = args.format.toLowerCase();
      if (!['json', 'markdown'].includes(result.format)) {
        return `Error: Invalid format '${args.format}'. Supported formats are 'json' and 'markdown'.`;
      }
    }
    
    if (args.detailed) {
      result.detailed = true;
    }
    
    if (args.metrics) {
      result.metrics = args.metrics;
    }
    
    if (args['ai-autonomy']) {
      result.aiAutonomy = true;
    }
    
    if (args['autonomy-only']) {
      result.autonomyOnly = true;
      result.aiAutonomy = true;
      result.analysisType = 'autonomy';
    }
    
    if (args.aggregate) {
      result.aggregate = true;
    }
    
    // Validate arguments
    if (!result.taskDescription && !result.taskId && !result.filePath && !result.dirPath) {
      return "Error: You must provide a task description, task ID, file path, or directory path.";
    }
    
    // Perform the analysis based on the arguments
    const analysis = await performComplexityAnalysis(result);
    
    // Format the output
    return formatAnalysisOutput(analysis, result);
  } catch (error) {
    return `Error analyzing complexity: ${error.message}`;
  }
}

/**
 * Perform complexity analysis based on input type
 * @param {Object} options - Analysis options
 * @returns {Promise<Object>} - Analysis result
 */
async function performComplexityAnalysis(options) {
  // This would normally call the Node.js implementation
  // For Claude, we'll use our own implementation
  
  if (options.taskId) {
    // Analyze a task by ID
    return analyzeTaskById(options.taskId, options);
  } else if (options.filePath) {
    // Analyze a file
    return analyzeFile(options.filePath, options);
  } else if (options.dirPath) {
    // Analyze a directory
    return analyzeDirectory(options.dirPath, options);
  } else {
    // Analyze a task description
    return analyzeTaskDescription(options.taskDescription, options);
  }
}

/**
 * Analyze a task by ID
 * @param {string} taskId - Task ID
 * @param {Object} options - Analysis options
 * @returns {Promise<Object>} - Analysis result
 */
async function analyzeTaskById(taskId, options) {
  try {
    // Get the task file path
    const tasksJsonPath = "C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\tasks.json";
    
    // Check if tasks.json exists
    if (!await fileExists(tasksJsonPath)) {
      throw new Error(`tasks.json not found at ${tasksJsonPath}`);
    }
    
    // Read and parse tasks.json
    const tasksContent = await readFile(tasksJsonPath);
    const tasksData = JSON.parse(tasksContent);
    
    // Find the task
    const task = tasksData.find(t => t.id === taskId);
    if (!task) {
      throw new Error(`Task with ID "${taskId}" not found`);
    }
    
    // Get the task file path
    const taskFilePath = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${taskId}\\task-${taskId}.md`;
    
    // Check if task file exists
    if (!await fileExists(taskFilePath)) {
      throw new Error(`Task file not found at ${taskFilePath}`);
    }
    
    // Read the task file
    const taskContent = await readFile(taskFilePath);
    
    // Extract the task description
    const taskDescription = extractTaskDescription(taskContent);
    
    // Analyze the task description
    const analysis = await analyzeTaskDescription(taskDescription, options);
    
    // Add task metadata
    analysis.taskId = taskId;
    analysis.taskName = task.name;
    analysis.taskPath = taskFilePath;
    
    return analysis;
  } catch (error) {
    throw new Error(`Error analyzing task: ${error.message}`);
  }
}

/**
 * Extract task description from task content
 * @param {string} taskContent - Task file content
 * @returns {string} - Task description
 */
function extractTaskDescription(taskContent) {
  // Try to extract the description section
  const descriptionMatch = taskContent.match(/## Description\s*\n([\s\S]*?)(?:\n##|$)/);
  if (descriptionMatch && descriptionMatch[1].trim()) {
    return descriptionMatch[1].trim();
  }
  
  // If no description section, try to extract goal section
  const goalMatch = taskContent.match(/## Goal\s*\n([\s\S]*?)(?:\n##|$)/);
  if (goalMatch && goalMatch[1].trim()) {
    return goalMatch[1].trim();
  }
  
  // If neither, try to extract the title
  const titleMatch = taskContent.match(/# Task - (.*?)(?:\n|$)/);
  if (titleMatch && titleMatch[1].trim()) {
    return titleMatch[1].trim();
  }
  
  // If all else fails, return the whole content
  return taskContent.trim();
}

/**
 * Analyze a file
 * @param {string} filePath - File path
 * @param {Object} options - Analysis options
 * @returns {Promise<Object>} - Analysis result
 */
async function analyzeFile(filePath, options) {
  try {
    // Normalize the file path
    const normalizedPath = normalizePath(filePath);
    
    // Check if the file exists
    if (!await fileExists(normalizedPath)) {
      throw new Error(`File not found at ${normalizedPath}`);
    }
    
    // Read the file
    const fileContent = await readFile(normalizedPath);
    
    // Analyze the file content
    let analysis = await analyzeTaskDescription(fileContent, options);
    
    // Add file metadata
    analysis.filePath = normalizedPath;
    analysis.fileName = getFileName(normalizedPath);
    analysis.fileType = getFileExtension(normalizedPath);
    
    return analysis;
  } catch (error) {
    throw new Error(`Error analyzing file: ${error.message}`);
  }
}

/**
 * Get the file name from a path
 * @param {string} filePath - File path
 * @returns {string} - File name
 */
function getFileName(filePath) {
  return filePath.split('\\').pop();
}

/**
 * Get the file extension from a path
 * @param {string} filePath - File path
 * @returns {string} - File extension
 */
function getFileExtension(filePath) {
  const fileName = getFileName(filePath);
  const parts = fileName.split('.');
  return parts.length > 1 ? parts.pop() : '';
}

/**
 * Analyze a directory
 * @param {string} dirPath - Directory path
 * @param {Object} options - Analysis options
 * @returns {Promise<Object>} - Analysis result
 */
async function analyzeDirectory(dirPath, options) {
  try {
    // Normalize the directory path
    const normalizedPath = normalizePath(dirPath);
    
    // Check if the directory exists
    if (!await fileExists(normalizedPath)) {
      throw new Error(`Directory not found at ${normalizedPath}`);
    }
    
    // Get directory info
    const dirInfo = await getFileInfo(normalizedPath);
    if (!dirInfo.isDirectory) {
      throw new Error(`${normalizedPath} is not a directory`);
    }
    
    // List files in the directory
    const files = await listDirectory(normalizedPath);
    
    // Filter files based on extension
    const codeExtensions = ['.js', '.jsx', '.ts', '.tsx', '.py', '.java', '.c', '.cpp', '.cs', '.go', '.rb', '.php', '.html', '.css', '.scss'];
    const codeFiles = files
      .filter(file => {
        const ext = '.' + file.split('.').pop().toLowerCase();
        return codeExtensions.includes(ext);
      })
      .map(file => `${normalizedPath}\\${file}`);
    
    if (codeFiles.length === 0) {
      throw new Error(`No code files found in ${normalizedPath}`);
    }
    
    // Limit the number of files to analyze to avoid token limits
    const MAX_FILES = 5;
    const filesToAnalyze = codeFiles.slice(0, MAX_FILES);
    
    // Read each file
    const fileContents = [];
    for (const file of filesToAnalyze) {
      try {
        const content = await readFile(file);
        fileContents.push({
          path: file,
          name: getFileName(file),
          extension: getFileExtension(file),
          content: content.substring(0, 10000) // Limit content size
        });
      } catch (error) {
        console.log(`Error reading file ${file}: ${error.message}`);
      }
    }
    
    // Create a combined description for analysis
    const combinedDescription = `
# Code Directory Analysis

Directory: ${normalizedPath}
Total files: ${codeFiles.length}
Files analyzed: ${fileContents.length}

## Files:

${fileContents.map(file => `### ${file.name} (${file.extension})\n\n${file.content.substring(0, 500)}...\n`).join('\n\n')}
`;

    // Analyze the combined description
    let analysis = await analyzeTaskDescription(combinedDescription, {
      ...options,
      detailed: true, // Always use detailed analysis for directories
    });
    
    // Add directory metadata
    analysis.dirPath = normalizedPath;
    analysis.dirName = getFileName(normalizedPath);
    analysis.totalFiles = codeFiles.length;
    analysis.analyzedFiles = fileContents.length;
    analysis.fileList = fileContents.map(f => f.name);
    
    // If aggregate analysis is requested, analyze each file separately
    if (options.aggregate) {
      const fileAnalyses = [];
      
      for (const file of fileContents) {
        try {
          const fileAnalysis = await analyzeTaskDescription(file.content, {
            ...options,
            detailed: false
          });
          
          fileAnalyses.push({
            fileName: file.name,
            complexity: fileAnalysis.complexityScore,
            autonomy: fileAnalysis.autonomyScore
          });
        } catch (error) {
          console.log(`Error analyzing file ${file.name}: ${error.message}`);
        }
      }
      
      // Add file analyses to the result
      analysis.fileAnalyses = fileAnalyses;
      
      // Calculate aggregate metrics
      if (fileAnalyses.length > 0) {
        analysis.aggregateComplexity = Math.round(
          fileAnalyses.reduce((sum, f) => sum + f.complexity, 0) / fileAnalyses.length
        );
        
        if (options.aiAutonomy) {
          analysis.aggregateAutonomy = Math.round(
            fileAnalyses.reduce((sum, f) => sum + (f.autonomy || 3), 0) / fileAnalyses.length
          );
        }
      }
    }
    
    return analysis;
  } catch (error) {
    throw new Error(`Error analyzing directory: ${error.message}`);
  }
}

/**
 * Analyze a task description
 * @param {string} taskDescription - Task description
 * @param {Object} options - Analysis options
 * @returns {Promise<Object>} - Analysis result
 */
async function analyzeTaskDescription(taskDescription, options) {
  try {
    // This is where we would normally call the LLM
    // For Claude, we'll implement the analysis directly
    
    // Set up the prompt
    let prompt = "";
    
    if (options.autonomyOnly) {
      // Prompt for autonomy-only analysis
      prompt = `
Analyze the following task/code for AI autonomy potential only:

${taskDescription}

Evaluate the AI autonomy potential based on these criteria (score 1-5, where 1 is highest autonomy):
1. Clarity & Specificity: How clearly defined is the task/code?
2. Scope Definition: How well-bounded is the scope?
3. Technical Complexity: How technically complex is the implementation?
4. Integration Needs: How much integration with other systems is required?
5. Dependency on External Factors: How dependent is it on external systems or data?
6. Need for Human Judgment/Creativity: How much subjective judgment is needed?
7. Testability: How easily can the results be tested/verified?

Provide an overall autonomy score (1-5) where:
1 = Very High Autonomy (>90% AI success rate, minimal human involvement)
2 = High Autonomy (75-90% AI success rate, light human guidance)
3 = Medium Autonomy (50-75% AI success rate, regular human oversight)
4 = Low Autonomy (25-50% AI success rate, significant human guidance)
5 = Very Low Autonomy (<25% AI success rate, extensive human direction)

Format your analysis as a structured assessment with:
1. Summary of the task/code
2. Score for each criterion (1-5) with brief explanation
3. Overall autonomy score (1-5) with explanation
4. Human intervention points
5. Suggestions to increase autonomy potential
6. Estimated AI implementation time and human involvement time
`;
    } else if (options.aiAutonomy) {
      // Prompt for combined complexity and autonomy analysis
      prompt = `
Analyze the following task/code for both complexity and AI autonomy potential:

${taskDescription}

Part 1: Complexity Analysis
Evaluate complexity (score 1-10) based on:
- Conceptual complexity
- Implementation complexity
- Domain knowledge requirements
- Dependencies and integration points
- Risk factors and edge cases

Part 2: AI Autonomy Assessment
Evaluate AI autonomy potential (score 1-5, where 1 is highest autonomy) based on:
1. Clarity & Specificity
2. Scope Definition
3. Technical Complexity
4. Integration Needs
5. Dependency on External Factors
6. Need for Human Judgment/Creativity
7. Testability

Provide both:
1. Complexity score (1-10) with breakdown
2. Autonomy score (1-5) with breakdown
3. Specific human intervention points
4. Implementation approach recommendations
5. Estimated time for AI implementation and human involvement
`;
    } else {
      // Prompt for standard complexity analysis
      prompt = `
Analyze the following task/code for complexity:

${taskDescription}

Evaluate complexity based on:
- Conceptual complexity: How many concepts need to be understood?
- Implementation complexity: How difficult is the actual coding/implementation?
- Domain knowledge requirements: How much specialized knowledge is needed?
- Dependencies and integration points: How many components does this interact with?
- Risk factors and edge cases: What could go wrong?

Provide:
1. Overall complexity score (1-10)
2. Breakdown of complexity factors with scores (1-10)
3. Whether this should be broken down into subtasks
4. If applicable, suggested subtasks
5. Estimated effort (Low, Medium, High, Very High)
${options.detailed ? '6. Detailed analysis of the complexity factors and implementation challenges' : ''}
`;
    }
    
    // In a real implementation, we would call the LLM here
    // For now, we'll simulate the result
    
    // Generate complexity analysis
    const analysis = await generateComplexityAnalysis(taskDescription, options);
    
    return analysis;
  } catch (error) {
    throw new Error(`Error analyzing task description: ${error.message}`);
  }
}

/**
 * Generate a complexity analysis (This simulates the LLM's response)
 * @param {string} taskDescription - Task description
 * @param {Object} options - Analysis options
 * @returns {Promise<Object>} - Analysis result
 */
async function generateComplexityAnalysis(taskDescription, options) {
  // In a real implementation, this would call Claude or another LLM
  // For now, we'll use a rule-based approach to simulate the result
  
  const currentDate = new Date().toISOString().split('T')[0];
  
  // Default analysis
  const analysis = {
    date: currentDate,
    taskDescription: taskDescription.substring(0, 100) + (taskDescription.length > 100 ? '...' : ''),
    complexityScore: 5,
    complexityLevel: 'Moderate',
    breakdown: {
      conceptual: 5,
      implementation: 5,
      domainKnowledge: 5,
      dependencies: 5,
      risks: 5
    },
    shouldBreakDown: false,
    suggestedSubtasks: [],
    estimatedEffort: 'Medium',
    detailedAnalysis: 'This task involves moderate complexity based on the description.',
    analysisMethod: 'claude-simulated'
  };
  
  // Add autonomy assessment if requested
  if (options.aiAutonomy || options.autonomyOnly) {
    analysis.autonomyScore = 3;
    analysis.autonomyLevel = 'Medium Autonomy';
    analysis.autonomyBreakdown = {
      clarity: 3,
      scope: 3,
      technicalComplexity: 3,
      integration: 3,
      externalDependencies: 3,
      humanJudgment: 3,
      testability: 3
    };
    analysis.humanInterventionPoints = [
      'Initial requirements clarification',
      'Design review and approval',
      'Testing and validation of results'
    ];
    analysis.autonomyImprovements = [
      'Provide more detailed specifications',
      'Create clear acceptance criteria',
      'Break down into smaller, well-defined tasks'
    ];
    analysis.aiImplementationTime = '3-6 hours';
    analysis.humanInvolvementTime = '1-2 hours';
  }
  
  // Perform basic keyword analysis to adjust complexity
  const description = taskDescription.toLowerCase();
  let complexityAdjustment = 0;
  
  const complexityIndicators = {
    // Architecture terms
    'architecture': 2,
    'system design': 2,
    'design pattern': 1.5,
    'microservice': 2,
    'distributed': 2,
    'scalable': 1.5,
    
    // Integration terms
    'integrate': 1,
    'integration': 1,
    'api': 1,
    'third party': 1.5,
    'third-party': 1.5,
    'external service': 1.5,
    
    // Data complexity
    'database': 1,
    'data model': 1.5,
    'schema': 1,
    'migration': 1.5,
    'synchronization': 1.5,
    
    // Security terms
    'security': 1.5,
    'authentication': 1,
    'authorization': 1,
    'permission': 0.8,
    'encryption': 1.5,
    'oauth': 1,
    'jwt': 0.8,
    
    // UI/UX terms
    'responsive': 0.8,
    'animation': 0.8,
    'interactive': 0.8,
    'drag and drop': 1,
    'drag-and-drop': 1,
    
    // Performance terms
    'performance': 1.5,
    'optimization': 1.5,
    'caching': 1,
    'latency': 1,
    'throughput': 1,
    
    // Complexity indicators
    'complex': 1.5,
    'complicated': 1.5,
    'refactor': 1.5,
    'redesign': 1.5,
    
    // Scope indicators
    'system': 1,
    'framework': 1.5,
    'platform': 1.5,
    'end-to-end': 2,
    'end to end': 2,
    'full-stack': 1.5,
    'fullstack': 1.5
  };
  
  // Apply keyword scoring
  for (const [term, value] of Object.entries(complexityIndicators)) {
    if (description.includes(term)) {
      complexityAdjustment += value;
    }
  }
  
  // Adjust complexity score (1-10 scale)
  let adjustedComplexity = Math.min(Math.max(Math.round(5 + complexityAdjustment/2), 1), 10);
  
  // Update analysis with adjusted scores
  analysis.complexityScore = adjustedComplexity;
  analysis.complexityLevel = getComplexityLevel(adjustedComplexity);
  
  // Adjust breakdown scores based on keywords
  analysis.breakdown.conceptual = Math.min(Math.max(Math.round(5 + complexityAdjustment * 0.7), 1), 10);
  analysis.breakdown.implementation = Math.min(Math.max(Math.round(5 + complexityAdjustment * 0.8), 1), 10);
  analysis.breakdown.domainKnowledge = Math.min(Math.max(Math.round(5 + complexityAdjustment * 0.5), 1), 10);
  analysis.breakdown.dependencies = Math.min(Math.max(Math.round(5 + complexityAdjustment * 0.6), 1), 10);
  analysis.breakdown.risks = Math.min(Math.max(Math.round(5 + complexityAdjustment * 0.6), 1), 10);
  
  // Determine if task should be broken down
  analysis.shouldBreakDown = adjustedComplexity >= 7;
  
  // Generate suggested subtasks if complexity is high
  if (analysis.shouldBreakDown) {
    // Extract potential subtasks from description
    const sentences = description.split(/[.!?]+/);
    const keywords = ['create', 'implement', 'build', 'develop', 'design', 'add', 'update'];
    
    for (const sentence of sentences) {
      for (const keyword of keywords) {
        if (sentence.includes(keyword) && analysis.suggestedSubtasks.length < 5) {
          const subtask = sentence.trim().charAt(0).toUpperCase() + sentence.trim().slice(1);
          if (subtask.length > 10 && !analysis.suggestedSubtasks.includes(subtask)) {
            analysis.suggestedSubtasks.push(subtask);
          }
        }
      }
    }
    
    // If we couldn't extract subtasks, generate some generic ones
    if (analysis.suggestedSubtasks.length === 0) {
      analysis.suggestedSubtasks = [
        "Research and requirements gathering",
        "Design and architecture",
        "Implementation of core functionality",
        "Testing and validation",
        "Documentation and deployment"
      ];
    }
  }
  
  // Set estimated effort based on complexity
  if (adjustedComplexity <= 3) {
    analysis.estimatedEffort = 'Low';
  } else if (adjustedComplexity <= 6) {
    analysis.estimatedEffort = 'Medium';
  } else if (adjustedComplexity <= 8) {
    analysis.estimatedEffort = 'High';
  } else {
    analysis.estimatedEffort = 'Very High';
  }
  
  // If autonomy assessment is requested, adjust autonomy score based on complexity
  if (options.aiAutonomy || options.autonomyOnly) {
    // Scale autonomy inversely with complexity (1-5 scale, lower is more autonomous)
    let autonomyScore = Math.min(Math.max(Math.round((adjustedComplexity+2)/2), 1), 5);
    
    analysis.autonomyScore = autonomyScore;
    analysis.autonomyLevel = getAutonomyLevel(autonomyScore);
    
    // Adjust autonomy breakdown scores
    analysis.autonomyBreakdown.clarity = Math.min(Math.round(autonomyScore * 0.8), 5);
    analysis.autonomyBreakdown.scope = Math.min(Math.round(autonomyScore * 0.9), 5);
    analysis.autonomyBreakdown.technicalComplexity = Math.min(Math.round(autonomyScore * 1.1), 5);
    analysis.autonomyBreakdown.integration = Math.min(Math.round(autonomyScore * 1.0), 5);
    analysis.autonomyBreakdown.externalDependencies = Math.min(Math.round(autonomyScore * 1.0), 5);
    analysis.autonomyBreakdown.humanJudgment = Math.min(Math.round(autonomyScore * 1.1), 5);
    analysis.autonomyBreakdown.testability = Math.min(Math.round(autonomyScore * 0.9), 5);
    
    // Set implementation time based on autonomy score
    switch (autonomyScore) {
      case 1:
        analysis.aiImplementationTime = '< 1 hour';
        analysis.humanInvolvementTime = '~15 minutes';
        analysis.storyPoints = 1;
        break;
      case 2:
        analysis.aiImplementationTime = '1-3 hours';
        analysis.humanInvolvementTime = '~30 minutes';
        analysis.storyPoints = 2;
        break;
      case 3:
        analysis.aiImplementationTime = '3-6 hours';
        analysis.humanInvolvementTime = '1-2 hours';
        analysis.storyPoints = 3;
        break;
      case 4:
        analysis.aiImplementationTime = '~1 day (6-8 hours)';
        analysis.humanInvolvementTime = '3-4 hours';
        analysis.storyPoints = 8;
        break;
      case 5:
        analysis.aiImplementationTime = '>1 day (8+ hours)';
        analysis.humanInvolvementTime = '6+ hours';
        analysis.storyPoints = 13;
        break;
    }
  }
  
  return analysis;
}

/**
 * Get complexity level description based on score
 * @param {number} score - Complexity score
 * @returns {string} - Complexity level description
 */
function getComplexityLevel(score) {
  const levels = {
    1: 'Trivial',
    2: 'Very Simple',
    3: 'Simple',
    4: 'Moderate',
    5: 'Somewhat Complex',
    6: 'Complex',
    7: 'Very Complex',
    8: 'Extremely Complex',
    9: 'Highly Intricate',
    10: 'Maximum Complexity'
  };
  
  return levels[score] || 'Unknown';
}

/**
 * Get autonomy level description based on score
 * @param {number} score - Autonomy score
 * @returns {string} - Autonomy level description
 */
function getAutonomyLevel(score) {
  const levels = {
    1: 'Very High Autonomy',
    2: 'High Autonomy',
    3: 'Medium Autonomy',
    4: 'Low Autonomy',
    5: 'Very Low Autonomy'
  };
  
  return levels[score] || 'Unknown';
}

/**
 * Format the analysis output
 * @param {Object} analysis - Analysis result
 * @param {Object} options - Output options
 * @returns {string} - Formatted output
 */
function formatAnalysisOutput(analysis, options) {
  if (options.format === 'json') {
    return JSON.stringify(analysis, null, 2);
  } else {
    return formatAnalysisAsMarkdown(analysis, options);
  }
}

/**
 * Format the analysis as markdown
 * @param {Object} analysis - Analysis result
 * @param {Object} options - Output options
 * @returns {string} - Markdown formatted analysis
 */
function formatAnalysisAsMarkdown(analysis, options) {
  // Get entity name
  let entityName = '';
  if (analysis.taskId) {
    entityName = analysis.taskName || analysis.taskId;
  } else if (analysis.filePath) {
    entityName = analysis.fileName || 'File Analysis';
  } else if (analysis.dirPath) {
    entityName = analysis.dirName || 'Directory Analysis';
  } else {
    entityName = analysis.taskDescription || 'Task Analysis';
  }
  
  // Get entity type
  let entityType = '';
  if (analysis.taskId) {
    entityType = 'task';
  } else if (analysis.filePath) {
    entityType = 'file';
  } else if (analysis.dirPath) {
    entityType = 'directory';
  } else {
    entityType = 'description';
  }
  
  let markdown = `# Complexity Analysis: ${entityName}\n\n`;
  
  // Summary section
  markdown += '## Summary\n\n';
  
  if (options.aiAutonomy) {
    markdown += `This ${entityType} has been analyzed for both complexity and AI autonomy potential.\n\n`;
    markdown += `- **Complexity Score:** ${analysis.complexityScore}/10 (${analysis.complexityLevel})\n`;
    markdown += `- **AI Autonomy Score:** ${analysis.autonomyScore}/5 (${analysis.autonomyLevel})\n\n`;
  } else {
    markdown += `This ${entityType} has been analyzed for complexity.\n\n`;
    markdown += `- **Complexity Score:** ${analysis.complexityScore}/10 (${analysis.complexityLevel})\n\n`;
  }
  
  // Complexity assessment section
  markdown += '## Complexity Assessment\n\n';
  
  markdown += '### Complexity Breakdown\n\n';
  markdown += '| Factor | Score | Description |\n';
  markdown += '|--------|-------|-------------|\n';
  markdown += `| Conceptual Complexity | ${analysis.breakdown.conceptual}/10 | How many concepts need to be understood |\n`;
  markdown += `| Implementation Complexity | ${analysis.breakdown.implementation}/10 | How difficult the actual coding/implementation is |\n`;
  markdown += `| Domain Knowledge | ${analysis.breakdown.domainKnowledge}/10 | Level of specialized knowledge required |\n`;
  markdown += `| Dependencies | ${analysis.breakdown.dependencies}/10 | How many components this interacts with |\n`;
  markdown += `| Risk Factors | ${analysis.breakdown.risks}/10 | Potential issues and edge cases |\n\n`;
  
  markdown += `**Estimated Effort:** ${analysis.estimatedEffort}\n\n`;
  
  // Add AI autonomy assessment if requested
  if (options.aiAutonomy || options.autonomyOnly) {
    markdown += '## AI Autonomy Assessment\n\n';
    
    markdown += '### Autonomy Criteria\n\n';
    markdown += '| Criterion | Score | Description |\n';
    markdown += '|-----------|-------|-------------|\n';
    markdown += `| Clarity & Specificity | ${analysis.autonomyBreakdown.clarity}/5 | How clearly defined is the task |\n`;
    markdown += `| Scope Definition | ${analysis.autonomyBreakdown.scope}/5 | How well-bounded is the scope |\n`;
    markdown += `| Technical Complexity | ${analysis.autonomyBreakdown.technicalComplexity}/5 | How technically complex is the implementation |\n`;
    markdown += `| Integration Needs | ${analysis.autonomyBreakdown.integration}/5 | How much integration with other systems is required |\n`;
    markdown += `| External Dependencies | ${analysis.autonomyBreakdown.externalDependencies}/5 | How dependent is it on external systems or data |\n`;
    markdown += `| Human Judgment Needs | ${analysis.autonomyBreakdown.humanJudgment}/5 | How much subjective judgment is needed |\n`;
    markdown += `| Testability | ${analysis.autonomyBreakdown.testability}/5 | How easily can the results be tested/verified |\n\n`;
    
    markdown += '### Effort Estimates\n\n';
    markdown += `- **Estimated AI Implementation Time:** ${analysis.aiImplementationTime}\n`;
    markdown += `- **Estimated Human Involvement:** ${analysis.humanInvolvementTime}\n`;
    markdown += `- **Story Points:** ${analysis.storyPoints}\n\n`;
    
    markdown += '### Human Intervention Points\n\n';
    for (const point of analysis.humanInterventionPoints) {
      markdown += `- ${point}\n`;
    }
    markdown += '\n';
    
    markdown += '### Suggested Improvements for Autonomy\n\n';
    for (const improvement of analysis.autonomyImprovements) {
      markdown += `- ${improvement}\n`;
    }
    markdown += '\n';
  }
  
  // Task breakdown recommendation
  if (analysis.shouldBreakDown !== undefined) {
    markdown += '## Task Breakdown Recommendation\n\n';
    
    if (analysis.shouldBreakDown) {
      markdown += 'This task **should be broken down** into smaller subtasks based on its complexity.\n\n';
      
      if (analysis.suggestedSubtasks && analysis.suggestedSubtasks.length > 0) {
        markdown += '### Suggested Subtasks\n\n';
        
        for (const subtask of analysis.suggestedSubtasks) {
          markdown += `- ${subtask}\n`;
        }
        markdown += '\n';
      }
    } else {
      markdown += 'This task can be implemented as a single unit based on its complexity.\n\n';
    }
  }
  
  // If this is a directory analysis with aggregate metrics
  if (analysis.dirPath && options.aggregate && analysis.fileAnalyses) {
    markdown += '## Aggregate Analysis\n\n';
    
    if (analysis.fileAnalyses.length > 0) {
      markdown += '### File Complexity Breakdown\n\n';
      markdown += '| File | Complexity | Autonomy |\n';
      markdown += '|------|------------|----------|\n';
      
      for (const fileAnalysis of analysis.fileAnalyses) {
        markdown += `| ${fileAnalysis.fileName} | ${fileAnalysis.complexity}/10 | ${fileAnalysis.autonomy ? fileAnalysis.autonomy + '/5' : 'N/A'} |\n`;
      }
      
      markdown += '\n';
      
      if (analysis.aggregateComplexity) {
        markdown += `**Average Complexity:** ${analysis.aggregateComplexity}/10\n`;
      }
      
      if (analysis.aggregateAutonomy) {
        markdown += `**Average Autonomy:** ${analysis.aggregateAutonomy}/5\n`;
      }
    }
  }
  
  // Detailed analysis
  if (options.detailed && analysis.detailedAnalysis) {
    markdown += '## Detailed Analysis\n\n';
    markdown += analysis.detailedAnalysis + '\n\n';
  }
  
  // Add metadata footer
  markdown += '---\n\n';
  markdown += `*Analysis generated on ${analysis.date} using ScaffoldX's x-analyze-complexity command*\n`;
  
  return markdown;
}

/**
 * Save the analysis to a file
 * @param {Object} analysis - Analysis result
 * @param {Object} options - Save options
 * @returns {Promise<string>} - Path to saved file
 */
async function saveAnalysisToFile(analysis, options) {
  try {
    let savePath = '';
    
    // Determine where to save the file
    if (analysis.taskId) {
      // Save to task directory
      const taskDir = `C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xtasks\\${analysis.taskId}`;
      
      // Create analysis directory if it doesn't exist
      const analysisDir = `${taskDir}\\analysis`;
      await createDirectory(analysisDir);
      
      // Determine file name based on analysis type
      const fileName = options.aiAutonomy ? 
        (options.autonomyOnly ? 'autonomy_analysis.md' : 'complexity_autonomy_analysis.md') : 
        'complexity_analysis.md';
      
      savePath = `${analysisDir}\\${fileName}`;
    } else if (analysis.filePath) {
      // Save in the workbench directory
      const workbenchDir = 'C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xworkbench';
      await createDirectory(workbenchDir);
      
      const fileName = options.format === 'json' ? 'complexity_analysis.json' : 'complexity_analysis.md';
      savePath = `${workbenchDir}\\${fileName}`;
    } else if (analysis.dirPath) {
      // Save in the directory being analyzed
      const fileName = options.format === 'json' ? 'complexity_analysis.json' : 'complexity_analysis.md';
      savePath = `${analysis.dirPath}\\${fileName}`;
    } else {
      // Save in the workbench directory
      const workbenchDir = 'C:\\__dev\\_projects\\ScaffoldX\\.scaffoldx\\xworkbench';
      await createDirectory(workbenchDir);
      
      const fileName = options.format === 'json' ? 'complexity_analysis.json' : 'complexity_analysis.md';
      savePath = `${workbenchDir}\\${fileName}`;
    }
    
    // Format the content
    let content = '';
    if (options.format === 'json') {
      content = JSON.stringify(analysis, null, 2);
    } else {
      content = formatAnalysisAsMarkdown(analysis, options);
    }
    
    // Write the file
    await writeFile(savePath, content);
    
    return savePath;
  } catch (error) {
    throw new Error(`Error saving analysis to file: ${error.message}`);
  }
}

// File utility functions

/**
 * Normalize a file path
 * @param {string} filePath - File path
 * @returns {string} - Normalized path
 */
function normalizePath(filePath) {
  // Convert to absolute path if not already
  if (!filePath.includes(':') && !filePath.startsWith('\\\\')) {
    if (filePath.startsWith('./') || filePath.startsWith('.\\')) {
      filePath = filePath.substring(2);
    }
    
    filePath = `C:\\__dev\\_projects\\ScaffoldX\\${filePath}`;
  }
  
  // Normalize slashes
  return filePath.replace(/\//g, '\\');
}

/**
 * Check if a file exists
 * @param {string} path - Path to check
 * @returns {Promise<boolean>} - True if file exists
 */
async function fileExists(path) {
  try {
    await getFileInfo(path);
    return true;
  } catch (error) {
    return false;
  }
}

/**
 * Read a file
 * @param {string} path - Path to read
 * @returns {Promise<string>} - File content
 */
async function readFile(path) {
  return await window.fs.readFile(path, { encoding: 'utf8' });
}

/**
 * Write a file
 * @param {string} path - Path to write
 * @param {string} content - File content
 * @returns {Promise<void>}
 */
async function writeFile(path, content) {
  await window.fs.writeFile(path, content);
}

/**
 * Get file info
 * @param {string} path - Path to check
 * @returns {Promise<Object>} - File info
 */
async function getFileInfo(path) {
  const info = await window.fs.stat(path);
  return {
    ...info,
    isDirectory: info.isDirectory()
  };
}

/**
 * Create a directory
 * @param {string} path - Directory path
 * @returns {Promise<void>}
 */
async function createDirectory(path) {
  await window.fs.mkdir(path, { recursive: true });
}

/**
 * List directory contents
 * @param {string} path - Directory path
 * @returns {Promise<string[]>} - Directory contents
 */
async function listDirectory(path) {
  return await window.fs.readdir(path);
}

// Export the command handler function
module.exports = {
  processAnalyzeComplexityCommand
};
```

## Integration with Enhanced Command Processor

This handler integrates with the enhanced command processor to provide functionality for the `x-analyze-complexity` command. It:

1. Processes command arguments and options
2. Analyzes task descriptions, existing tasks, files, and directories
3. Provides both complexity and AI autonomy assessments
4. Generates detailed analyses with specific metrics
5. Supports various output formats (markdown, JSON)
6. Allows saving analysis results to files

The handler follows the centralized adapter pattern, ensuring that all command logic is processed through a single interface.

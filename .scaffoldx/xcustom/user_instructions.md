## xcore symlink resolution
IMPORTANT: Use `Read` for `.scaffoldx/xcore` since it is a symlink. 

1. Use `Read` with the symlinked path (reads follow symlinks fine)
2. Use the resolved path for Glob/Grep: `c:/__dev/_projects/ScaffoldX_Dev/.scaffoldx/xcore/`
3. Use Bash for directory listings through the symlink

## File Placement Preference (User-Enforced)

** NEVER write outputs to ~/.claude/ — everything stays in the project.

When producing analysis reports, synthesis documents, brainstorm outputs, or any artifact worth keeping:
1. If a task bucket exists → place inside the task directory
2. If a brainstorm folder fits → use `.scaffoldx/xtasks/_brainstorms/{date}_{topic}/`
3. If an issue is relevant → attach to the numbered issue
4. If none of the above → **suggest a project-tracked location first**, don't default to ephemeral

Rationale: Posterity, cohesiveness, integrity. User wants everything trackable inside the project structure, not scattered in tool-specific ephemeral directories.


## Output Format

- refrain from using em-dashes in output. Break up sentence structures instead and use periods ( if possible ). If not possible or otherwise resulting in awkward sentence structures, use a normal hyphen "-".
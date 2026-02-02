# Issue: x-checkpoint-create violates AI-GOV-013 (Unidirectionality Principle)

## Summary
`x-checkpoint-create` attempts to pass semantic content through bash command-line arguments, which:
1. Fails with code snippets (bash interprets special characters)
2. Violates AI-GOV-013 by mixing semantic content with deterministic execution

## The Architectural Violation

Current design tries to pass AI-crafted content through bash:
```
AI crafts content → passes via --content arg → bash mangles it → script receives garbage
```

This violates the oracle pattern: **Scripts should do deterministic work, LLM fills semantic content.**

## Observed Failure

```bash
x-checkpoint-create save_point --content "Added `expandedStates` HashMap..."
```

```
/usr/bin/bash: expandedStates: No such file or directory
/usr/bin/bash: syntax error near unexpected token 'new'
```

Bash interprets backticks, `$`, `()`, `<>` before the script receives them.

## Correct Architecture (AI-GOV-013 Compliant)

Follow the **oracle pattern** - script creates skeleton, returns path, LLM fills content:

### Flow
```
┌─────────────────┐    ┌──────────────────────┐    ┌─────────────────┐
│ AI requests     │ => │ Script creates       │ => │ AI fills        │
│ checkpoint      │    │ skeleton file        │    │ content via     │
│ (type, entity)  │    │ (returns path)       │    │ Edit/Write      │
└─────────────────┘    └──────────────────────┘    └─────────────────┘
     Semantic              Deterministic              Semantic
     (AI decides)          (Script executes)          (AI creates)
```

### Script Responsibility (Deterministic Only)
```javascript
// x-checkpoint-create.js - Oracle pattern
function main(payload) {
    // 1. Generate checkpoint ID (deterministic)
    const id = generateCheckpointId(payload.type);

    // 2. Determine path (deterministic)
    const path = payload.entity_id
        ? `.scaffoldx/xtasks/${payload.entity_id}/checkpoints/${id}_${payload.type}.md`
        : `.scaffoldx/xmemory/checkpoints/${id}_${payload.type}.md`;

    // 3. Create skeleton with YAML frontmatter only (deterministic)
    const skeleton = generateFrontmatter(id, payload);

    // 4. Write skeleton file
    fs.writeFileSync(path, skeleton);

    // 5. Return path in payload contract (LLM will fill content)
    return {
        success: true,
        checkpoint_id: id,
        file_path: path,
        action_required: "LLM must fill content using Edit tool"
    };
}
```

### LLM Responsibility (Semantic)
```
1. Call x-checkpoint-create with minimal args (type, entity, title)
2. Receive file_path from script response
3. Use Edit/Write tool to fill content (no bash escaping needed)
```

## Benefits of Oracle Pattern

| Aspect | Current (Broken) | Oracle Pattern |
|--------|------------------|----------------|
| Content passing | Via bash args | Via file system |
| Escaping issues | Yes (fatal) | None |
| AI-GOV-013 | Violated | Compliant |
| Testability | Hard | Easy (mock payloads) |
| Separation | Mixed | Clean |

## Implementation Changes Required

### 1. Refactor x-checkpoint-create.js
- Remove `--content` argument handling
- Create skeleton file with frontmatter only
- Return `{ file_path, checkpoint_id }` in JSON response

### 2. Update Command Spec (x-checkpoint-create.md)
- Document two-phase flow: create skeleton → fill content
- Remove `--content` as required argument
- Add examples showing Edit tool usage after creation

### 3. Example New Usage
```bash
# Phase 1: Script creates skeleton (deterministic)
x-checkpoint-create save_point --entity-type task --entity-id 0015 --title "Debug Session"
# Returns: { "file_path": ".scaffoldx/xtasks/0015.../CP_2026_02_02_001_save_point.md" }

# Phase 2: LLM fills content (semantic)
# LLM uses Edit/Write tool on the returned file_path
```

## Related Governance

- **AI-GOV-013**: Unidirectionality Principle - scripts are passive executors
- **AI-GOV-006**: AI crafts content - but via file system, not bash args
- **AI-GOV-009**: Oracle Usage - scripts provide deterministic data, LLM decides

## Environment
- Platform: Windows (MSYS bash)
- ScaffoldX: 2.0.0

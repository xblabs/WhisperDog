# Artifacts Directory

## Purpose

This directory collects **input materials** that will be used to populate task content via `x-task-populate`.

## Workflow

```
1. Collect → Place input materials here (specs, docs, requirements, code samples)
2. Populate → Run x-task-populate <task-id> to generate task corpus
3. Refine → Iterate on content as needed
```

## What to Put Here

- **Requirements documents**: User stories, specifications, business requirements
- **Technical documentation**: Architecture diagrams, API specs, schemas
- **Code samples**: Example implementations, reference code
- **Design files**: Mockups, wireframes, design systems
- **Research**: Technical research, proof of concepts, analysis

## File Organization

- Use descriptive filenames
- Group related files in subdirectories if needed
- Include brief notes if artifacts need explanation

## Usage

```bash
# Populate task from artifacts
x-task-populate <task-id>

# Or specify different source
x-task-populate <task-id> --from-context <other-source>
```

## Note

Artifacts are **inputs** (what goes in), not outputs. Generated content goes in canonical files (01_prd.md, etc.).

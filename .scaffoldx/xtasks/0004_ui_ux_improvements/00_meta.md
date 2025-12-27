---
task_id: "0004"
task_name: "UI/UX Improvements"
status: "todo"
priority: "medium"
created: "2025-12-30"
task_type: "executable"
tags: ["ui", "ux", "post-processing", "search", "polish"]
---

# Task 0004: UI/UX Improvements

## Summary

UI/UX polish for WhisperDog including post-processing UI reorganization, searchable log screen, and long-running process improvements. These changes create a more professional, usable interface while maintaining the existing functionality.

**Note**: Icons and padding work moved to Task 0001 (Rebranding) since they overlap with ongoing visual asset work.

## Context

Current UI issues:
- Post-processing section confusing (too many nested options)
- No way to search through transcription logs
- Long-running processes block UI with no visibility or control

## Key Objectives

1. **Post-Processing UI (WD-0003)**: Reorganize checkboxes and section visibility
2. **Searchable Log (WD-0009)**: Add Ctrl+F search with highlighting
3. **Long Process UX (WD-0011)**: Add file links, retry/cancel buttons

## Sub-elements

| ID | Name | Complexity | Description |
|----|------|------------|-------------|
| WD-0003 | Post-processing UI | Low-Medium | Reorganize visibility/auto checkboxes |
| WD-0009 | Searchable Log | Medium | Search input + highlighting + Ctrl+F |
| WD-0011 | Long Process UX | Medium | File links, retry, cancel buttons |

## Success Criteria

- Post-processing has separate show/auto checkboxes
- Log search accessible via Ctrl+F with match highlighting
- Long processes show file path and allow cancel/retry

## Related Files

- Source specification: `C:\Users\henry\Documents\Obsidian\Main\02_INBOX\PROJECTS\whisperdog.md`
- Icons/padding: See Task 0001 (Rebranding)

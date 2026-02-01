---
id: "0016"
title: Icon Theming System
slug: icon_theming_system
status: in_progress
priority: medium
created: 2026-02-01
task_type: executable
tags:
  - ui
  - icons
  - theming
  - architecture
---

# Task 0016: Icon Theming System

## Summary

Implement a runtime icon color theming system that stores icons as pure black source SVGs and applies context-specific colors at load time via FlatSVGIcon ColorFilter. Eliminates pre-baked colors, enables consistent theming, and supports hierarchical color contexts (app default → menu → panel → button).

## Context

Current icon system has two problems:
1. Menu icons are numbered by index (0.svg, 1.svg) causing misalignment when menu order changes
2. Icons have pre-baked gray colors, making theming impossible without editing each SVG file

This task creates a proper icon architecture: source icons remain pure black, colors are applied at runtime based on context.

## Scope

- Reorganize icon folder structure (source SVGs with semantic names)
- Create icon color configuration system
- Implement IconLoader utility with context-aware ColorFilter
- Support hierarchical color contexts: app → menu → menu.collapsed → panel → button
- Migrate existing icon usages to new system

## Dependencies

- Task 0015 (Recordings Panel UX) - icon fix portion superseded by this task

## Acceptance Criteria

- [ ] All source SVGs stored as pure black (#000000) outlines
- [ ] Icon colors defined in single configuration location
- [ ] Menu icons load with correct colors via IconLoader
- [ ] Button icons appear more prominent than menu icons
- [ ] Collapsed menu icons can have different color than expanded
- [ ] Adding new icons requires only placing SVG file + optional config entry

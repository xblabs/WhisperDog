# Icon Source Directory

This directory contains pure black (#000000) source SVG icons for runtime theming.

## Naming Convention

Icons are named using lowercase with hyphens for word separation:
- `mic.svg` - Microphone icon
- `waveform-1.svg` - Waveform icon (recordings)
- `pipeline-1.svg` - Pipeline icon
- `chevron-up.svg` - Chevron up arrow
- `menu_left.svg` - Menu collapse left (underscore for direction variants)
- `play-2.svg` / `stop.svg` - Playback control icons

## Adding New Icons

1. **Source file**: Must be pure black (#000000) for stroke or fill
2. **File format**: SVG only
3. **Naming**: Use semantic names that describe the icon's purpose
4. **Size**: Standard viewBox of `0 0 24 24` preferred

## Color Application

Colors are applied at runtime via `IconLoader.java`:
- Source black (#000000) is replaced with context-appropriate colors
- Different contexts (MENU, BUTTON, PANEL) have different color mappings
- Light/dark theme colors are handled automatically

## Icon Contexts

| Context | Light Theme | Dark Theme | Usage |
|---------|-------------|------------|-------|
| MENU | Gray-500 | Gray-400 | Side menu icons (expanded) |
| MENU_COLLAPSED | Gray-600 | Gray-300 | Side menu icons (collapsed) |
| BUTTON | Gray-700 | Gray-200 | Button icons (more prominent) |
| PANEL | Gray-500 | Gray-400 | Panel icons |
| DISABLED | Gray-400 | Gray-600 | Disabled state |

## Menu Icon Mappings

| Menu Name | Icon File |
|-----------|-----------|
| Record | mic.svg |
| Recordings | waveform-1.svg |
| Settings | sliders.svg |
| PostPro Pipelines | pipeline-1.svg |
| PostPro Unit Library | box.svg |

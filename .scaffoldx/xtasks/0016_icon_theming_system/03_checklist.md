# Checklist: Icon Theming System

## Phase 1: Icon Directory Reorganization
- [x] Create `icon/source/` directory
- [x] Add/convert source icons with pure black fill (#000000)
- [x] Verify all source icons have correct fill color
- [x] Document icon naming convention (see icon/source/README.md)

## Phase 2: Color Configuration System
- [x] Create `IconContext.java` enum
- [x] Create `IconColors.java` with color mappings
- [x] Define fallback chain logic
- [x] Test color retrieval for all contexts (via build verification)

## Phase 3: IconLoader Utility
- [x] Create `IconLoader.java` class
- [x] Implement ColorFilter for black→target replacement
- [x] Add icon caching for performance
- [x] Add helper method for button icon sets

## Phase 4: Migration
- [x] Update `MenuItem.java` to use IconLoader
- [x] Update `RecordingsPanel.java` button icons
- [ ] Search and update all other FlatSVGIcon usages
- [ ] Remove legacy numbered icons (0.svg, 1.svg, etc.)

## Phase 5: Verification
- [ ] Menu icons display with correct colors
- [ ] Button icons appear more prominent than menu icons
- [ ] Reordering menu items doesn't break icon associations
- [ ] Adding new icon works without code changes (just drop SVG)
- [ ] No missing/red icons anywhere in application

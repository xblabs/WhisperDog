# WhisperDog Rebranding - Execution Checklist

## Phase 1: Preparation & Repository Setup
- [x] Create new GitHub repository for WhisperDog with MIT license and project description
- [x] Clone and prepare codebase migration from whispercat-xb repository

## Phase 2: Core Code Migration
- [x] Rename package directories and update all Java package declarations from org.whispercat to org.whisperdog
- [x] Update all class imports, string literals, and UI text references throughout codebase
- [ ] Implement configuration migration logic in ConfigManager with non-destructive backup

## Phase 3: Visual Assets & Icons
- [x] Copy SVG icons from brainstorm directory to resources (play, chevrons, menu icons)
- [ ] Design and create new dog-themed application icon in all required formats and sizes
- [x] Update RecorderForm.java to use FlatSVGIcon for all buttons, replacing broken Unicode characters

## Phase 4: UI Improvements
- [x] Fix list padding in UnitLibraryListForm and PipelineListForm by adding empty borders
- [x] Update left menu icons with concrete mappings (git-pull-request, box, sliders, terminal)

## Phase 5: Build Configuration
- [x] Update pom.xml with new groupId, artifactId, version, and main class configuration
- [x] Update build scripts for Windows installer and portable packages with new naming

## Phase 6: Documentation Updates
- [x] Rewrite README.md with WhisperDog branding, history section, and acknowledgements
- [x] Update CLAUDE.md with all package and project name references
- [x] Create CHANGELOG entry and RELEASE_NOTES for v2.0.0

## Phase 7: Testing & Verification
- [x] Execute full Maven build and verify JAR outputs with correct naming
- [ ] Test application launch, config migration, and all core functionality (recording, transcription, pipelines)
- [ ] Verify UI elements display correctly including icons, padding, and text across light/dark themes

## Phase 8: Repository Publishing
- [x] Commit all changes, tag v2.0.0, and push to GitHub repository
- [ ] Create GitHub release with binaries and comprehensive release notes
- [ ] Archive whispercat-xb repository with redirect notice in README

## Phase 9: Communication & Rollout
- [ ] Publish announcement about WhisperDog rebrand with acknowledgements to original project
- [ ] Update all external references, documentation links, and project mentions

---

**Note**: Each phase contains detailed technical steps in `02_implementation_plan.md`
This checklist provides high-level tracking - refer to implementation plan for code examples and file paths.

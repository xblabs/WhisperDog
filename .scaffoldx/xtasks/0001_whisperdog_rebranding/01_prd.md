# Product Requirements Document: WhisperDog Rebranding

## Problem Statement

The current WhisperCat-xblabs fork has diverged significantly from the original WhisperCat project through extensive feature additions (pipeline system, processing units library, silence removal, console logging, and more). Continuing under the WhisperCat name creates confusion about project ownership, makes it difficult to establish independent branding, and doesn't clearly communicate the evolution of features. Users may be unclear about which version they're using, and the xblabs fork deserves its own identity to reflect the substantial development investment. Additionally, the current UI has broken Unicode icons that don't render properly on all systems (appearing as empty boxes), and list padding issues that make the interface cramped. The rebrand to WhisperDog addresses these issues while establishing a memorable brand identity that honors the original project.

**Measurable Impact**:
- **Brand Clarity**: Eliminate confusion between original WhisperCat and xblabs fork
- **UI Quality**: Replace 3 broken Unicode icons with SVG icons that render universally
- **User Experience**: Fix padding in 2 list components (Unit Library, Pipeline Library)
- **Professional Identity**: Establish independent project with clear attribution
- **Version Progression**: Start v2.0.0 to reflect major milestone (v1.x was WhisperCat era)

## Solution Overview

Comprehensive rebrand from WhisperCat to WhisperDog across all layers:

### 1. Brand Identity
- **Name**: WhisperDog
- **Tagline Options**:
  - "Your attentive audio companion"
  - "Listen. Transcribe. Transform."
  - "The loyal transcription assistant"
- **Brand Rationale**: Dogs have excellent hearing, are attentive listeners, and loyal companions—perfect metaphor for a transcription tool that "listens" quietly and reliably

### 2. Repository Strategy
```
New Repository: github.com/xblabs/whisperdog
Description: "WhisperDog - Your attentive audio companion for transcription and text processing"
License: MIT
```

Migration approach:
1. Create new `whisperdog` repository
2. Copy all code from `whispercat-xb`
3. Apply rebranding changes (detailed in implementation plan)
4. Create v2.0.0 release
5. Archive `whispercat-xb` with redirect notice

### 3. Technical Renaming
- **Package**: `org.whispercat` → `org.whisperdog`
- **Main Class**: `AudioRecorderUI` → `WhisperDogApp` (or keep as-is for consistency)
- **Config**: `whispercat.properties` → `whisperdog.properties`
- **Config Directory**: `~/.whispercat/` → `~/.whisperdog/` (with migration)

### 4. Visual Assets

#### New Icon Requirements
- **Style**: Friendly dog silhouette with headphones or sound waves
- **Color Palette**: Keep similar or refresh
- **Formats**: SVG (main), PNG (16x16, 32x32, 64x64, 128x128, 256x256), ICO (Windows), ICNS (macOS)

#### Icon Concepts
1. **Listening Dog** - Dog head in profile with perked ears, sound waves coming in
2. **Headphone Dog** - Friendly dog face wearing headphones
3. **Whisper Dog** - Dog with paw to mouth in "shh" gesture
4. **Abstract** - Dog ear shape forming a sound wave or speech bubble

#### Tray Icon
- Simplified version for system tray (16x16, 22x22)
- Recognizable at small sizes
- Consider animated version for "recording" state

#### Concrete Icon Mapping (from brainstorm)
**Left Menu Icons**:
- **Pipelines**: `.scaffoldx/xtasks/_brainstorms/rebrand/icons/git-pull-request.svg`
- **Units**: `.scaffoldx/xtasks/_brainstorms/rebrand/icons/box.svg`
- **Settings**: `.scaffoldx/xtasks/_brainstorms/rebrand/icons/sliders.svg`
- **Log**: `.scaffoldx/xtasks/_brainstorms/rebrand/icons/terminal.svg`

**Button Icons**:
- **Play/Run Pipeline**: `.scaffoldx/xtasks/_brainstorms/rebrand/icons/play.svg`
- **Arrow Up**: `.scaffoldx/xtasks/_brainstorms/rebrand/icons/chevron-up.svg`
- **Arrow Down**: `.scaffoldx/xtasks/_brainstorms/rebrand/icons/chevron-down.svg`
- **Record**: `.scaffoldx/xtasks/_brainstorms/rebrand/icons/mic.svg`

### 5. UI Text Updates

#### Window Titles
```java
// Old: "WhisperCat - Audio Recorder"
// New: "WhisperDog - Audio Recorder"
```

#### Toast Notifications
```java
// Old: "WhisperCat: Transcription complete"
// New: "WhisperDog: Transcription complete"
```

#### About Dialog
```
WhisperDog v2.0.0
Your attentive audio companion

Transcription powered by OpenAI Whisper
Post-processing powered by LLMs

Originally forked from WhisperCat by ddxy
Enhanced by xblabs

MIT License
```

#### System Tray
```
Tooltip: "WhisperDog - Recording Ready"
Menu items: (keep same functionality, update any "WhisperCat" references)
```

### 6. Configuration Migration

Provide automatic migration for existing WhisperCat users:

**Directory Migration**:
```
OLD: ~/.whispercat/ or %APPDATA%/WhisperCat/
NEW: ~/.whisperdog/ or %APPDATA%/WhisperDog/
```

**Migration Strategy**:
- Detect old config file on startup
- Copy to new location if not exists
- Rename old file to `.migrated` extension
- Log migration for user awareness
- Non-destructive (keeps backup)

### 7. Documentation Strategy

#### README.md
- Full rewrite with new branding
- Clear "History" section acknowledging WhisperCat origins
- Updated feature list
- New icon/logo in header

#### CLAUDE.md
- Update project name throughout
- Update package references
- Keep technical content, rebrand naming

#### Acknowledgements
```markdown
## Acknowledgements

WhisperDog is built on the foundation of [WhisperCat](https://github.com/ddxy/whispercat)
by [ddxy](https://github.com/ddxy). We are grateful for their original work
which made this project possible.

### Original WhisperCat Contributors
- [ddxy](https://github.com/ddxy) - Original creator

### WhisperDog Enhancements
- [xblabs](https://github.com/xblabs) - Fork maintainer, feature development
```

### 8. Build Artifacts

#### Maven pom.xml
```xml
<groupId>org.whisperdog</groupId>
<artifactId>whisperdog</artifactId>
<version>2.0.0</version>
<name>WhisperDog</name>
<description>Your attentive audio companion for transcription and text processing</description>

<properties>
    <main.class>org.whisperdog.WhisperDogApp</main.class>
</properties>
```

#### Output Names
```
OLD: Audiorecorder-1.0-SNAPSHOT-jar-with-dependencies.jar
NEW: WhisperDog-2.0.0-jar-with-dependencies.jar

OLD: WhisperCat-Setup.exe
NEW: WhisperDog-Setup.exe
```

## Version Strategy

### Initial WhisperDog Release: v2.0.0

**Rationale**:
- v1.x was WhisperCat era
- v2.0.0 signifies major rebrand + all accumulated features
- Clean break while honoring version progression

### Version History Bridge
```markdown
## Version History

### WhisperDog Releases
- v2.0.0 - Initial WhisperDog release (rebrand from WhisperCat-xblabs)

### WhisperCat-xblabs Legacy (archived)
- v1.6.0-xblabs - Final WhisperCat-xblabs release
- v1.5.0-xblabs - Pipeline optimization, silence removal
- v1.4.0-xblabs - Processing unit library, console logging
```

## Acceptance Criteria

### Code
- [ ] All Java packages renamed to `org.whisperdog`
- [ ] All class imports updated
- [ ] All string references updated (case-sensitive search/replace)
- [ ] pom.xml updated with new artifact names
- [ ] Main class renamed and registered
- [ ] Config file migration logic implemented and tested

### Visual Assets
- [ ] New dog icon designed and approved
- [ ] All icon size variants created (SVG, PNG, ICO, ICNS)
- [ ] Tray icon created and tested at small sizes
- [ ] All SVG icons copied from brainstorm to resources
- [ ] Icon files replaced in `src/main/resources/`
- [ ] Left menu icons updated with concrete mappings
- [ ] Broken Unicode icons replaced with SVG equivalents

### UI
- [ ] All window titles updated
- [ ] Toast notifications updated
- [ ] About dialog updated with new branding
- [ ] System tray tooltip and menu updated
- [ ] RecorderForm.java updated to use FlatSVGIcon for buttons
- [ ] List padding fixed in UnitLibraryListForm.java
- [ ] List padding fixed in PipelineListForm.java

### Documentation
- [ ] README.md rewritten with new branding
- [ ] CLAUDE.md updated with package references
- [ ] CHANGELOG.md updated with rebrand entry
- [ ] Acknowledgements section added
- [ ] Migration guide created for existing users
- [ ] All spec files updated

### Build & Test
- [ ] Maven build succeeds with new artifact names
- [ ] Application launches successfully
- [ ] Config migration works (test with old config file)
- [ ] All features still functional
- [ ] All UI text displays correctly
- [ ] All icons display correctly (test light/dark themes)

### Repository
- [ ] New `whisperdog` repository created on GitHub
- [ ] Code migrated to new repository
- [ ] v2.0.0 tag created
- [ ] GitHub release created with artifacts
- [ ] `whispercat-xb` archived with redirect notice

### Communication
- [ ] Release notes written for v2.0.0
- [ ] `whispercat-xb` README updated with redirect
- [ ] Announcement prepared for relevant channels

## Out of Scope

- Changing core functionality (all features remain the same)
- Modifying architecture (core design stays intact)
- Changing user experience (UX remains consistent)
- License changes (MIT License maintained)

## Rollback Plan

If critical issues arise:
1. Keep `whispercat-xb` repo intact (archived, not deleted)
2. Users can continue using last WhisperCat release (v1.6.0-xblabs)
3. Config migration is non-destructive (keeps `.migrated` backup)
4. Clear communication about both options available

## Timeline Estimate

| Phase | Tasks | Duration |
|-------|-------|----------|
| Design | Icon creation, planning | 1-2 days |
| Code | Package rename, string updates | 2-3 hours |
| Docs | README, changelog, guides | 1-2 hours |
| Test | Full feature verification | 1-2 hours |
| Release | Build, tag, publish | 1 hour |

**Total: 2-3 days** (icon design being the longest part)

## Why This Rebrand Matters

### Reasons
- Significant feature deviation from original WhisperCat
- Professional/ethical to establish independent identity
- Clearer branding for users
- Freedom to evolve without confusion about project ownership
- Dog metaphor perfectly suits the "attentive listening" nature of transcription

### What Stays the Same
- All functionality
- MIT License
- Core architecture
- User experience

### What Changes
- Name and branding only
- Package structure (technical requirement)
- Config file locations (with seamless migration)
- Visual assets (icons, logos)

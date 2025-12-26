# Implementation Plan: WhisperDog Rebranding

## Overview

This implementation plan provides detailed, copy-paste ready instructions for completing the WhisperDog rebrand. All code examples, file paths, and configurations are preserved from the specification to ensure gapless implementation.

---

## Phase 1: Repository Setup

### 1.1 Create New GitHub Repository

```bash
# Create new repository on GitHub
# Repository: github.com/xblabs/whisperdog
# Description: "WhisperDog - Your attentive audio companion for transcription and text processing"
# License: MIT
# Public/Private: Public (recommended)
```

**Settings to configure**:
- Enable Issues
- Enable Discussions (recommended)
- Add topics: `transcription`, `whisper`, `java`, `swing`, `audio-processing`, `llm`

### 1.2 Repository Branch Strategy

```bash
# Main branch: master (keep consistent with original)
# Development branch: develop (optional)
```

---

## Phase 2: Code Migration

### 2.1 Copy Codebase

```bash
# Clone current whispercat-xb repository
git clone <whispercat-xb-repo-url> whisperdog-migration
cd whisperdog-migration

# Remove old git history (optional - clean slate approach)
rm -rf .git

# Initialize new git repository
git init
git remote add origin git@github.com:xblabs/whisperdog.git
```

**Alternative (preserve history)**:
```bash
git clone <whispercat-xb-repo-url> whisperdog
cd whisperdog
git remote set-url origin git@github.com:xblabs/whisperdog.git
```

### 2.2 Package Renaming

**Search and Replace Strategy**:
```bash
# Find all Java files requiring package updates
find src/main/java -name "*.java" | wc -l

# Pattern to search for: org.whispercat
# Pattern to replace with: org.whisperdog
```

**Manual Approach** (recommended for verification):
1. Rename package directories first
2. Update package declarations in each file
3. Update imports across all files
4. Verify with IDE refactoring tools

**File Operations**:
```bash
# Rename package directory
mv src/main/java/org/whispercat src/main/java/org/whisperdog
```

**Files to Update** (package declarations):
```
src/main/java/org/whisperdog/*.java - All files
```

**Sample package declaration update**:
```java
// OLD:
package org.whispercat;

// NEW:
package org.whisperdog;
```

**Sample import update**:
```java
// OLD:
import org.whispercat.AudioRecorderUI;
import org.whispercat.config.ConfigManager;

// NEW:
import org.whisperdog.AudioRecorderUI;
import org.whisperdog.config.ConfigManager;
```

### 2.3 Class Renaming

| Old Name | New Name | File Path | Notes |
|----------|----------|-----------|-------|
| `AudioRecorderUI` | `WhisperDogApp` | `src/main/java/org/whisperdog/AudioRecorderUI.java` | Optional - can keep as AudioRecorderUI for consistency |

**If renaming main class**:
```java
// File: src/main/java/org/whisperdog/WhisperDogApp.java
// OLD:
public class AudioRecorderUI extends JFrame {

// NEW:
public class WhisperDogApp extends JFrame {
```

### 2.4 String Literal Updates

**Global Search and Replace** (case-sensitive):

```
Pattern 1: "WhisperCat" → "WhisperDog"
Pattern 2: "whispercat" → "whisperdog"
Pattern 3: "Whispercat" → "Whisperdog"
```

**Files Requiring String Updates**:
```
src/main/java/org/whisperdog/**/*.java - All UI strings, window titles, messages
src/main/resources/**/* - Any text resources
pom.xml - Artifact names, descriptions
README.md - Full document
CLAUDE.md - Project references
CHANGELOG.md - Add rebrand entry
```

**Specific String Updates**:

**Window Titles** (search in all Java files):
```java
// File: src/main/java/org/whisperdog/AudioRecorderUI.java (or WhisperDogApp.java)
// OLD:
setTitle("WhisperCat - Audio Recorder");

// NEW:
setTitle("WhisperDog - Audio Recorder");
```

**Toast Notifications**:
```java
// Search for pattern: "WhisperCat:"
// OLD:
Notifications.showNotification("WhisperCat: Transcription complete");

// NEW:
Notifications.showNotification("WhisperDog: Transcription complete");
```

**System Tray**:
```java
// File: Search for TrayIcon initialization
// OLD:
trayIcon.setToolTip("WhisperCat - Recording Ready");

// NEW:
trayIcon.setToolTip("WhisperDog - Recording Ready");
```

**About Dialog**:
```java
// File: Search for "About" or version dialog
// OLD:
String aboutText = "WhisperCat v1.6.0-xblabs\n...";

// NEW:
String aboutText = "WhisperDog v2.0.0\n" +
                   "Your attentive audio companion\n\n" +
                   "Transcription powered by OpenAI Whisper\n" +
                   "Post-processing powered by LLMs\n\n" +
                   "Originally forked from WhisperCat by ddxy\n" +
                   "Enhanced by xblabs\n\n" +
                   "MIT License";
```

### 2.5 Configuration Files

**Config File Name Update**:
```java
// File: src/main/java/org/whisperdog/ConfigManager.java
// OLD:
private static final String CONFIG_FILE = "whispercat.properties";

// NEW:
private static final String CONFIG_FILE = "whisperdog.properties";
```

**Config Directory Update**:
```java
// File: src/main/java/org/whisperdog/ConfigManager.java
// OLD:
private static final String CONFIG_DIR = ".whispercat";

// NEW:
private static final String CONFIG_DIR = ".whisperdog";
```

### 2.6 Configuration Migration Implementation

**Add Migration Logic to ConfigManager**:

```java
// File: src/main/java/org/whisperdog/ConfigManager.java
// Location: Add as new method in ConfigManager class

/**
 * Migrates configuration from WhisperCat to WhisperDog if needed.
 * Non-destructive - keeps old config as .migrated backup.
 */
public static void migrateConfigIfNeeded() {
    String oldConfigFile = "whispercat.properties";
    String newConfigFile = "whisperdog.properties";

    File configDir = getConfigDirectory();
    File oldConfig = new File(configDir, oldConfigFile);
    File newConfig = new File(configDir, newConfigFile);

    // Migration conditions:
    // 1. Old config exists
    // 2. New config does NOT exist
    // 3. We're running WhisperDog (not WhisperCat)
    if (oldConfig.exists() && !newConfig.exists()) {
        try {
            // Copy old config to new location
            Files.copy(oldConfig.toPath(), newConfig.toPath(),
                      StandardCopyOption.COPY_ATTRIBUTES);

            // Rename old file to .migrated (keep as backup)
            File migratedFile = new File(configDir, oldConfigFile + ".migrated");
            oldConfig.renameTo(migratedFile);

            // Log migration for user awareness
            System.out.println("✓ Migrated settings from WhisperCat to WhisperDog");
            System.out.println("  Old config backed up to: " + migratedFile.getName());

        } catch (IOException e) {
            System.err.println("Warning: Could not migrate config from WhisperCat");
            e.printStackTrace();
        }
    }
}

/**
 * Gets the configuration directory, with support for old WhisperCat directory.
 * Checks new WhisperDog directory first, falls back to old location if exists.
 */
private static File getConfigDirectory() {
    String userHome = System.getProperty("user.home");

    // New WhisperDog directory
    File newConfigDir = new File(userHome, ".whisperdog");

    // Old WhisperCat directory
    File oldConfigDir = new File(userHome, ".whispercat");

    // If new directory doesn't exist but old one does, use old location
    // (migration will handle moving to new location)
    if (!newConfigDir.exists() && oldConfigDir.exists()) {
        return oldConfigDir;
    }

    // Otherwise, ensure new directory exists and use it
    if (!newConfigDir.exists()) {
        newConfigDir.mkdirs();
    }

    return newConfigDir;
}
```

**Call Migration on Startup**:
```java
// File: src/main/java/org/whisperdog/WhisperDogApp.java (or AudioRecorderUI.java)
// Location: In main() method, before loading config

public static void main(String[] args) {
    // Migrate config from WhisperCat if needed
    ConfigManager.migrateConfigIfNeeded();

    // Continue with normal startup
    ConfigManager.loadConfig();
    // ... rest of main() method
}
```

---

## Phase 3: Visual Assets

### 3.1 Icon Files Location

**Source Icons** (already provided in brainstorm):
```
.scaffoldx/xtasks/_brainstorms/rebrand/icons/
├── box.svg (Units)
├── chevron-down.svg (Arrow down)
├── chevron-up.svg (Arrow up)
├── git-pull-request.svg (Pipelines)
├── mic.svg (Record)
├── play.svg (Run Pipeline)
├── sliders.svg (Settings)
└── terminal.svg (Log)
```

**Destination** (copy to resources):
```
src/main/resources/icon/svg/
├── box.svg
├── chevron-down.svg
├── chevron-up.svg
├── git-pull-request.svg
├── mic.svg
├── play.svg
├── sliders.svg
└── terminal.svg
```

**Copy Command**:
```bash
# Copy SVG icons from brainstorm to resources
cp .scaffoldx/xtasks/_brainstorms/rebrand/icons/*.svg src/main/resources/icon/svg/
```

### 3.2 Main Application Icon

**Current Icon Location**:
```
src/main/resources/whispercat.svg
src/main/resources/icon/*.png (various sizes)
```

**New Icon Requirements**:
- Design new dog-themed icon (Listening Dog, Headphone Dog, or Whisper Dog concept)
- Create in SVG format first
- Generate raster versions: 16x16, 32x32, 64x64, 128x128, 256x256
- Create platform-specific formats:
  - `whisperdog.ico` (Windows)
  - `whisperdog.icns` (macOS)

**Icon Files to Create**:
```
src/main/resources/whisperdog.svg - Main vector icon
src/main/resources/icon/whisperdog_16.png
src/main/resources/icon/whisperdog_32.png
src/main/resources/icon/whisperdog_64.png
src/main/resources/icon/whisperdog_128.png
src/main/resources/icon/whisperdog_256.png
src/main/resources/whisperdog.ico - Windows icon
src/main/resources/whisperdog.icns - macOS icon
```

**Icon Design Notes**:
- Use `currentColor` in SVG for theme compatibility
- Keep design recognizable at 16x16 for system tray
- Consider animated version for "recording" state (optional)

### 3.3 Update RecorderForm.java to Use SVG Icons

**File**: `src/main/java/org/whisperdog/RecorderForm.java`

**Broken Unicode Icons to Replace**:

**Line 261 - Run Pipeline Button**:
```java
// OLD:
runPipelineButton = new JButton("\u25B6 Run Pipeline");

// NEW:
runPipelineButton = new JButton("Run Pipeline");
runPipelineButton.setIcon(new FlatSVGIcon("icon/svg/play.svg", 16, 16));
```

**Line 364 - Search Previous Button**:
```java
// OLD:
searchPrevButton = new JButton("\u25B2");

// NEW:
searchPrevButton = new JButton();
searchPrevButton.setIcon(new FlatSVGIcon("icon/svg/chevron-up.svg", 16, 16));
searchPrevButton.setToolTipText("Previous match");
```

**Line 369 - Search Next Button**:
```java
// OLD:
searchNextButton = new JButton("\u25BC");

// NEW:
searchNextButton = new JButton();
searchNextButton.setIcon(new FlatSVGIcon("icon/svg/chevron-down.svg", 16, 16));
searchNextButton.setToolTipText("Next match");
```

**Import Statement** (add if not present):
```java
// File: src/main/java/org/whisperdog/RecorderForm.java
// Location: Top of file with other imports

import com.formdev.flatlaf.extras.FlatSVGIcon;
```

### 3.4 Update Left Menu Icons

**Current Implementation**:
```java
// Uses numbered icons: icon/0.svg, icon/1.svg, icon/2.svg, icon/3.svg
```

**New Icon Mapping**:
```
icon/0.svg (Pipelines)  → icon/svg/git-pull-request.svg
icon/1.svg (Units)      → icon/svg/box.svg
icon/2.svg (Settings)   → icon/svg/sliders.svg
icon/3.svg (Log)        → icon/svg/terminal.svg
```

**Implementation** (find menu creation code and update):
```java
// File: Search for menu creation (likely MainForm.java or similar)
// OLD:
pipelinesIcon = new FlatSVGIcon("icon/0.svg", 24, 24);

// NEW:
pipelinesIcon = new FlatSVGIcon("icon/svg/git-pull-request.svg", 24, 24);
unitsIcon = new FlatSVGIcon("icon/svg/box.svg", 24, 24);
settingsIcon = new FlatSVGIcon("icon/svg/sliders.svg", 24, 24);
logIcon = new FlatSVGIcon("icon/svg/terminal.svg", 24, 24);
```

---

## Phase 4: UI Padding Fixes

### 4.1 UnitLibraryListForm.java Padding Fix

**File**: `src/main/java/org/whisperdog/UnitLibraryListForm.java`
**Location**: Lines 75-84 (infoPanel creation)

```java
// OLD:
JPanel infoPanel = new JPanel();
infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

// NEW:
JPanel infoPanel = new JPanel();
infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
infoPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 0));  // top, left, bottom, right
```

### 4.2 PipelineListForm.java Padding Fix

**File**: `src/main/java/org/whisperdog/PipelineListForm.java`
**Location**: Find similar infoPanel structure

```java
// OLD:
JPanel infoPanel = new JPanel();
infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

// NEW:
JPanel infoPanel = new JPanel();
infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
infoPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 0));  // top, left, bottom, right
```

---

## Phase 5: Build Configuration

### 5.1 Update pom.xml

**File**: `pom.xml`

**Group ID and Artifact ID**:
```xml
<!-- OLD: -->
<groupId>org.whispercat</groupId>
<artifactId>whispercat</artifactId>
<version>1.6.0-xblabs</version>

<!-- NEW: -->
<groupId>org.whisperdog</groupId>
<artifactId>whisperdog</artifactId>
<version>2.0.0</version>
```

**Name and Description**:
```xml
<!-- OLD: -->
<name>WhisperCat</name>
<description>Audio transcription tool</description>

<!-- NEW: -->
<name>WhisperDog</name>
<description>Your attentive audio companion for transcription and text processing</description>
```

**Main Class Property**:
```xml
<!-- OLD: -->
<properties>
    <main.class>org.whispercat.AudioRecorderUI</main.class>
</properties>

<!-- NEW (if renamed): -->
<properties>
    <main.class>org.whisperdog.WhisperDogApp</main.class>
</properties>

<!-- NEW (if keeping AudioRecorderUI): -->
<properties>
    <main.class>org.whisperdog.AudioRecorderUI</main.class>
</properties>
```

**Final Name (JAR output)**:
```xml
<!-- OLD: -->
<build>
    <finalName>Audiorecorder-${version}-jar-with-dependencies</finalName>
</build>

<!-- NEW: -->
<build>
    <finalName>WhisperDog-${version}</finalName>
</build>
```

**Maven Assembly Plugin Configuration**:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <configuration>
        <archive>
            <manifest>
                <mainClass>org.whisperdog.WhisperDogApp</mainClass>
            </manifest>
        </archive>
        <descriptorRefs>
            <descriptorRef>jar-with-dependencies</descriptorRef>
        </descriptorRefs>
        <finalName>WhisperDog-${project.version}-jar-with-dependencies</finalName>
        <appendAssemblyId>false</appendAssemblyId>
    </configuration>
</plugin>
```

### 5.2 Build Scripts

**Windows Installer Script**:
```batch
REM File: build-windows-installer.bat
REM OLD:
set APP_NAME=WhisperCat
set INSTALLER_NAME=WhisperCat-Setup.exe

REM NEW:
set APP_NAME=WhisperDog
set INSTALLER_NAME=WhisperDog-Setup.exe
```

**Portable Package Script**:
```batch
REM File: create-portable-package.bat
REM OLD:
set PACKAGE_NAME=WhisperCat-Portable

REM NEW:
set PACKAGE_NAME=WhisperDog-Portable
```

**PowerShell Scripts** (*.ps1):
```powershell
# Search for any WhisperCat references and update
# Pattern: "WhisperCat" → "WhisperDog"
```

---

## Phase 6: Documentation Updates

### 6.1 README.md

**Complete Rewrite**:

```markdown
# WhisperDog

<p align="center">
  <img src="whisperdog.svg" alt="WhisperDog Icon" width="350"/>
</p>

<p align="center">
  <em>Your attentive audio companion</em>
</p>

WhisperDog is a desktop application for audio recording, transcription,
and intelligent text processing. Capture audio, transcribe with OpenAI
Whisper, and transform your text with customizable LLM-powered pipelines.

## History

WhisperDog evolved from the [WhisperCat](https://github.com/ddxy/whispercat)
project by [ddxy](https://github.com/ddxy). After extensive feature development
by xblabs, it was rebranded as an independent project while maintaining full
credit to the original creators.

## Features

[... current feature list from existing README ...]

## Installation

[... installation instructions ...]

## Usage

[... usage instructions ...]

## Acknowledgements

WhisperDog is built on the foundation of [WhisperCat](https://github.com/ddxy/whispercat)
by [ddxy](https://github.com/ddxy). We are grateful for their original work
which made this project possible.

### Original WhisperCat Contributors
- [ddxy](https://github.com/ddxy) - Original creator

### WhisperDog Enhancements
- [xblabs](https://github.com/xblabs) - Fork maintainer, feature development

## License

MIT License (same as original WhisperCat)
```

### 6.2 CLAUDE.md

**File**: `CLAUDE.md`

```markdown
# CLAUDE.md - Project Context for Claude Code

## Project Overview

**WhisperDog** is a Java Swing desktop application for audio recording
and transcription using OpenAI Whisper API or local Faster-Whisper servers.

[... rest of CLAUDE.md with updated references ...]
```

**Search and Replace Throughout**:
```
"WhisperCat" → "WhisperDog"
"whispercat" → "whisperdog"
"org.whispercat" → "org.whisperdog"
```

### 6.3 CHANGELOG.md

**Add Rebrand Entry**:

```markdown
# Changelog

## [2.0.0] - 2025-12-26

### Changed
- **REBRAND**: WhisperCat → WhisperDog
- Renamed all packages from `org.whispercat` to `org.whisperdog`
- Updated all visual assets with dog-themed branding
- Replaced broken Unicode icons with SVG icons (play, arrow up, arrow down)
- Fixed list padding in Unit Library and Pipeline Library
- Configuration migration from `~/.whispercat/` to `~/.whisperdog/`
- New tagline: "Your attentive audio companion"

### Added
- Automatic config migration from WhisperCat to WhisperDog
- FlatSVGIcon support for all UI icons
- New dog-themed application icons
- Comprehensive acknowledgements section crediting original WhisperCat

### Notes
- v2.0.0 signifies major rebrand milestone
- All functionality remains identical to v1.6.0-xblabs
- WhisperCat-xblabs repository archived with redirect notice

## [1.6.0-xblabs] - [Previous Date]
[... previous entries ...]
```

### 6.4 RELEASE_NOTES.md

**Create v2.0.0 Release Notes**:

```markdown
# WhisperDog v2.0.0 Release Notes

## Overview

Welcome to **WhisperDog v2.0.0** - the first release under the new branding!

This release marks the rebrand from WhisperCat-xblabs to WhisperDog,
establishing an independent identity while honoring the original project.

## What's New

### Rebranding
- New name: **WhisperDog**
- New tagline: "Your attentive audio companion"
- All packages renamed: `org.whispercat` → `org.whisperdog`
- Fresh dog-themed icons and visual assets

### UI Improvements
- **Fixed**: Broken Unicode icons replaced with universal SVG icons
- **Fixed**: List padding in Unit Library and Pipeline Library
- **Improved**: Icon visibility across all themes (light/dark)

### Migration Support
- Automatic configuration migration from WhisperCat
- Non-destructive migration (keeps backup)
- Seamless transition for existing users

## Installation

### For New Users
Download the appropriate package for your system:
- **Windows**: `WhisperDog-Setup.exe`
- **JAR (all platforms)**: `WhisperDog-2.0.0-jar-with-dependencies.jar`

### For WhisperCat Users
Your configuration will be automatically migrated on first launch:
- Old config: `~/.whispercat/whispercat.properties`
- New config: `~/.whisperdog/whisperdog.properties`
- Backup created: `~/.whispercat/whispercat.properties.migrated`

## History

WhisperDog evolved from [WhisperCat](https://github.com/ddxy/whispercat)
by [ddxy](https://github.com/ddxy). We're grateful for the original work
that made this project possible.

## What's Changed Since WhisperCat?
- Pipeline system enhancements
- Processing units library
- Silence removal features
- Console logging improvements
- ... and many more features developed by xblabs

## Full Changelog
See [CHANGELOG.md](CHANGELOG.md) for complete version history.

## License
MIT License (same as original WhisperCat)
```

---

## Phase 7: Testing & Verification

### 7.1 Build Verification

```bash
# Clean build
mvn clean

# Compile
mvn compile

# Expected output: BUILD SUCCESS
# Verify no package errors

# Package with dependencies
mvn package assembly:single

# Expected output: WhisperDog-2.0.0-jar-with-dependencies.jar
```

### 7.2 Launch Test

```bash
# Run the JAR
java -jar target/WhisperDog-2.0.0-jar-with-dependencies.jar

# Verify:
# - Application launches
# - Window title shows "WhisperDog - Audio Recorder"
# - Icons display correctly
# - No console errors about missing icons
```

### 7.3 Config Migration Test

```bash
# Create test environment
mkdir -p ~/.whispercat
echo "test.property=value" > ~/.whispercat/whispercat.properties

# Launch application
java -jar target/WhisperDog-2.0.0-jar-with-dependencies.jar

# Verify:
# - Console shows "✓ Migrated settings from WhisperCat to WhisperDog"
# - ~/.whisperdog/whisperdog.properties exists
# - ~/.whispercat/whispercat.properties.migrated exists
```

### 7.4 UI Test Checklist

- [ ] Window title shows "WhisperDog"
- [ ] About dialog shows v2.0.0 with acknowledgements
- [ ] System tray tooltip shows "WhisperDog - Recording Ready"
- [ ] Run Pipeline button shows play icon (not broken Unicode)
- [ ] Search arrows show chevron icons (not broken Unicode)
- [ ] Left menu shows: git-pull-request, box, sliders, terminal icons
- [ ] Unit Library list has proper padding (not cramped)
- [ ] Pipeline Library list has proper padding (not cramped)
- [ ] Icons display correctly in light theme
- [ ] Icons display correctly in dark theme

### 7.5 Functionality Test

Test all core features to ensure nothing broke:
- [ ] Audio recording works
- [ ] Transcription works (OpenAI Whisper API)
- [ ] Transcription works (local Faster-Whisper)
- [ ] Pipeline execution works
- [ ] Processing units work
- [ ] Silence removal works
- [ ] Console logging works
- [ ] Settings save/load correctly
- [ ] All existing features operational

---

## Phase 8: Repository Publishing

### 8.1 Initial Commit

```bash
# Stage all changes
git add .

# Commit with rebrand message
git commit -m "Initial WhisperDog v2.0.0 release

Complete rebrand from WhisperCat to WhisperDog:
- Renamed packages: org.whispercat → org.whisperdog
- Updated all visual assets with dog-themed branding
- Replaced broken Unicode icons with SVG icons
- Fixed list padding issues
- Implemented config migration from WhisperCat
- Updated all documentation

Originally forked from WhisperCat by ddxy
Enhanced by xblabs

MIT License"
```

### 8.2 Tag Release

```bash
# Create v2.0.0 tag
git tag -a v2.0.0 -m "WhisperDog v2.0.0 - Initial rebrand release"

# Push to GitHub
git push origin master
git push origin v2.0.0
```

### 8.3 Create GitHub Release

**Release Title**: `WhisperDog v2.0.0 - Initial Release`

**Release Description**:
```markdown
# WhisperDog v2.0.0

Welcome to **WhisperDog** - your attentive audio companion!

This is the first release under the new WhisperDog branding, evolved from
the WhisperCat project by [ddxy](https://github.com/ddxy).

## Highlights
- Complete rebrand with dog-themed visual identity
- Fixed broken Unicode icons (now using SVG)
- Improved UI padding and spacing
- Automatic configuration migration from WhisperCat
- All features from WhisperCat-xblabs v1.6.0

## Download
- **Windows Installer**: WhisperDog-Setup.exe
- **Universal JAR**: WhisperDog-2.0.0-jar-with-dependencies.jar

## For WhisperCat Users
Your settings will be automatically migrated on first launch.

See [RELEASE_NOTES.md](RELEASE_NOTES.md) for complete details.
```

**Attach Files**:
- `WhisperDog-2.0.0-jar-with-dependencies.jar`
- `WhisperDog-Setup.exe` (if built)

### 8.4 Archive whispercat-xb

**Update whispercat-xb README.md**:
```markdown
# WhisperCat-xblabs (ARCHIVED)

**⚠️ This repository has been superseded by [WhisperDog](https://github.com/xblabs/whisperdog)**

## Notice

This fork has been rebranded and moved to a new repository:
**[github.com/xblabs/whisperdog](https://github.com/xblabs/whisperdog)**

Please visit the new repository for:
- Latest releases
- Active development
- Issue tracking
- Feature requests

## Why the Rebrand?

After significant feature development, this fork evolved into an independent
project deserving its own identity. WhisperDog continues the work started
here while honoring the original WhisperCat project.

## Final Version

The final release under the WhisperCat-xblabs name was **v1.6.0-xblabs**.
All future development continues as WhisperDog starting from v2.0.0.

---

*Original WhisperCat by [ddxy](https://github.com/ddxy/whispercat)*
```

**Archive Repository**:
1. Go to repository Settings
2. Scroll to "Danger Zone"
3. Click "Archive this repository"
4. Confirm archival

---

## Phase 9: Communication

### 9.1 Announcement Template

```markdown
# Announcing WhisperDog v2.0.0 🐕

I'm excited to announce **WhisperDog v2.0.0** - a rebrand of the WhisperCat-xblabs fork!

## What is WhisperDog?

Your attentive audio companion for transcription and text processing.
WhisperDog combines OpenAI Whisper transcription with customizable
LLM-powered processing pipelines.

## Why the Rebrand?

After extensive feature development (pipelines, processing units, silence
removal, and more), the fork evolved significantly from the original.
The rebrand establishes a clear independent identity while honoring
the original WhisperCat project by ddxy.

## What's New in v2.0.0?

- Fresh dog-themed branding
- Fixed broken Unicode icons (now SVG)
- Improved UI spacing
- Automatic config migration from WhisperCat
- All features from v1.6.0-xblabs

## Get WhisperDog

Repository: https://github.com/xblabs/whisperdog
Releases: https://github.com/xblabs/whisperdog/releases

## Acknowledgements

Huge thanks to [ddxy](https://github.com/ddxy) for the original WhisperCat
project that made this possible!

---

*WhisperDog v2.0.0 - Your attentive audio companion*
```

### 9.2 Update External References

**Places to Update**:
- [ ] Personal website (if applicable)
- [ ] Social media profiles
- [ ] Forum signatures
- [ ] Blog posts referencing the project
- [ ] Any external documentation

---

## Rollback Procedures

### If Critical Issues Discovered

**Option 1: Keep Both Available**
```
WhisperCat-xblabs v1.6.0 (last stable) - available in archived repo
WhisperDog v2.0.1 (with fixes) - released after addressing issues
```

**Option 2: Emergency Patch**
```bash
# Fix critical issue
git checkout -b hotfix/critical-bug
# ... apply fix ...
git commit -m "Fix: Critical bug in v2.0.0"
git tag -a v2.0.1 -m "Emergency patch"
git push origin master v2.0.1
```

**Communication**:
```markdown
## Known Issue in v2.0.0

We've identified [issue description].

**Workaround**: [temporary solution]

**Fix**: v2.0.1 released with patch

**Alternative**: WhisperCat-xblabs v1.6.0 remains available at [archived repo URL]
```

---

## Success Criteria Validation

After completing all phases, verify:

### Code
- [ ] All Java packages renamed to `org.whisperdog`
- [ ] All class imports updated
- [ ] All string references updated
- [ ] pom.xml artifact names updated
- [ ] Main class registered correctly
- [ ] Config migration implemented and tested

### Visual Assets
- [ ] New dog icon designed
- [ ] All icon variants created
- [ ] SVG icons copied to resources
- [ ] Icon files replaced in resources
- [ ] Left menu icons updated
- [ ] Broken Unicode icons fixed

### UI
- [ ] Window titles updated
- [ ] Toast notifications updated
- [ ] About dialog updated
- [ ] System tray updated
- [ ] RecorderForm uses FlatSVGIcon
- [ ] UnitLibraryListForm padding fixed
- [ ] PipelineListForm padding fixed

### Documentation
- [ ] README rewritten
- [ ] CLAUDE.md updated
- [ ] CHANGELOG updated
- [ ] RELEASE_NOTES created
- [ ] Acknowledgements section added

### Build & Test
- [ ] Maven build succeeds
- [ ] Application launches
- [ ] Config migration works
- [ ] All features functional
- [ ] UI displays correctly
- [ ] Icons render correctly

### Repository
- [ ] New repository created
- [ ] Code migrated
- [ ] v2.0.0 tagged
- [ ] GitHub release created
- [ ] whispercat-xb archived

### Communication
- [ ] Release notes published
- [ ] whispercat-xb README redirects
- [ ] Announcement posted

---

## Notes

- **Icon Design**: The longest part of this process will be designing the dog icon. Consider hiring a designer or using AI tools.
- **Testing**: Thoroughly test on multiple platforms (Windows, macOS, Linux)
- **Config Migration**: Critical for user retention - ensure this works perfectly
- **Attribution**: Always maintain clear credit to original WhisperCat project

---

## Time Estimates

| Phase | Duration |
|-------|----------|
| Repository Setup | 30 minutes |
| Code Migration | 2-3 hours |
| Visual Assets | 1-2 days (icon design) |
| UI Fixes | 30 minutes |
| Build Configuration | 30 minutes |
| Documentation | 1-2 hours |
| Testing | 1-2 hours |
| Publishing | 1 hour |
| Communication | 30 minutes |

**Total**: 2-3 days (with icon design being the variable)

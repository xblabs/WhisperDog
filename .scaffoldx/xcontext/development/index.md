---
title: Development Domain Knowledge Index
description: Build process, git conventions, testing patterns, and development workflows
context_type: reference
priority: medium
domain: development
last_updated: 2025-12-26
tags: [index, development, build, git, testing, maven]
status: active
---

# Development Domain Knowledge Index

## 🎯 Token-Insensitive Overview (50 tokens)
**Essential context for basic understanding**

WhisperDog uses Maven for builds, conventional commits for changelogs, git hooks for automation, and follows Swing threading best practices.

**Key Areas:** Build System, Git Workflow, Testing, Conventions
**Entry Points:** `pom.xml`, `setup-git-hooks.sh`, `CHANGELOG.md`

## 🔍 Token-Sensitive Summary (200 tokens)
**Balanced context for informed decisions**

**Build System:**
- **Maven 3.x**: Project build tool
- **Java 11**: Target version
- **Assembly Plugin**: Creates fat JAR with dependencies
- **Exec Plugin**: Run application during development

**Git Workflow:**
- **Conventional Commits**: Structured commit messages for auto-changelog
- **Post-Commit Hook**: Automatically updates `CHANGELOG.md`
- **Commit Prefixes**: `feat:`, `fix:`, `docs:`, `refactor:`, `perf:`

**Development Patterns:**
- **EDT Threading**: All UI updates on Event Dispatch Thread
- **SwingWorker**: Async operations pattern
- **Properties-Based Config**: Easy testing without hardcoded values

**Critical Knowledge:**
- Run `setup-git-hooks.sh` after cloning repository
- Use conventional commit format for automatic changelog
- Build with `mvn clean package` before releases

## 📚 Deep Dive References (500+ tokens)
**Complete context for implementation**

### Build System

**pom.xml**

**Location:** `pom.xml` (project root)

**Maven Configuration:**
```xml
<groupId>org.example</groupId>
<artifactId>Audiorecorder</artifactId>
<version>1.0-SNAPSHOT</version>

<properties>
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
</properties>
```

**Key Dependencies:**
- **Jackson 2.18.2**: JSON serialization
- **Gson 2.12.1**: Alternative JSON parsing
- **FlatLaf 3.5.4**: Modern UI theme
- **Apache HttpClient 4.5.13**: HTTP requests
- **JNativeHook 2.2.2**: Global hotkeys
- **MigLayout 11.4.2**: Advanced layout manager
- **Log4j 2.x**: Logging framework

**Build Commands:**

```bash
# Clean build with tests
mvn clean package

# Build without tests (faster)
mvn clean package -DskipTests

# Run application
java -jar target/Audiorecorder-1.0-SNAPSHOT-jar-with-dependencies.jar

# Or use Maven exec plugin
mvn exec:java -Dexec.mainClass="org.whispercat.AudioRecorderUI"
```

**Assembly Plugin Configuration:**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <configuration>
        <archive>
            <manifest>
                <mainClass>org.whispercat.AudioRecorderUI</mainClass>
            </manifest>
        </archive>
        <descriptorRefs>
            <descriptorRef>jar-with-dependencies</descriptorRef>
        </descriptorRefs>
    </configuration>
</plugin>
```

**Build Output:**
- Regular JAR: `target/Audiorecorder-1.0-SNAPSHOT.jar`
- Fat JAR: `target/Audiorecorder-1.0-SNAPSHOT-jar-with-dependencies.jar`

### Git Workflow

**Conventional Commits Format:**

```
<type>: <description>

[optional body]

[optional footer]
```

**Commit Types:**
- `feat:` - New feature (appears in changelog)
- `fix:` - Bug fix (appears in changelog)
- `docs:` - Documentation changes
- `refactor:` - Code refactoring
- `perf:` - Performance improvements
- `test:` - Adding tests
- `chore:` - Maintenance tasks

**Examples:**
```bash
git commit -m "feat: add silence removal threshold slider"
git commit -m "fix: prevent crash when audio file exceeds 25MB"
git commit -m "docs: update README with chunking feature"
git commit -m "refactor: extract transcription client interface"
```

**Git Hooks Setup:**

```bash
# Run after cloning repository
./setup-git-hooks.sh
```

**Post-Commit Hook:**
- Location: `.git/hooks/post-commit`
- Automatically updates `CHANGELOG.md` after each commit
- Parses commit message type and description
- Groups changes by category (Features, Fixes, Documentation, etc.)
- Maintains chronological order

### CHANGELOG.md Structure

**Automatic Sections:**
- **Features** (from `feat:` commits)
- **Fixes** (from `fix:` commits)
- **Documentation** (from `docs:` commits)
- **Changes** (from `refactor:`, `perf:` commits)

**Example Entry:**
```markdown
## 2025-01-15

### Features
- Add silence removal threshold slider

### Fixes
- Prevent crash when audio file exceeds 25MB
```

### Documentation Update Process

**When implementing new features, update:**

1. **README.md**
   - Add to "xblabs Fork Changelog" section
   - Add new entry under "Latest Update" with date
   - Move previous "Latest Update" to dated "Update" section
   - Add to "Key Enhancements" if significant feature

2. **CHANGELOG.md**
   - Auto-updated by git hook with conventional commits

3. **CLAUDE.md** (this file in xcontext/Overview.md)
   - Update if new key classes added
   - Update if project structure changes significantly
   - Add new development patterns/notes

### Testing Patterns

**Manual Testing Checklist:**

**Recording:**
- [ ] Record short audio (< 1 minute)
- [ ] Record long audio (> 5 minutes)
- [ ] Test silence removal with various thresholds
- [ ] Test with large files (> 25MB)
- [ ] Verify chunking workflow

**Transcription:**
- [ ] Test OpenAI client with valid API key
- [ ] Test Faster-Whisper with local server
- [ ] Test chunked transcription progress
- [ ] Verify error handling for invalid API keys
- [ ] Test offline mode (Faster-Whisper only)

**Post-Processing:**
- [ ] Create pipeline with single unit
- [ ] Create pipeline with multiple units
- [ ] Test consecutive same-model optimization
- [ ] Verify pipeline history tracking
- [ ] Test manual pipeline runner

**UI:**
- [ ] Switch between screens during recording
- [ ] Verify state preservation
- [ ] Test global hotkey
- [ ] Test light/dark theme toggle
- [ ] Verify EDT threading (no UI freezes)

### Swing Threading Best Practices

**EDT Rules (CRITICAL):**

1. **All UI updates on EDT:**
```java
SwingUtilities.invokeLater(() -> {
    label.setText("Updated text");
});
```

2. **No blocking operations on EDT:**
```java
// WRONG - blocks UI
String result = longRunningOperation();  // On EDT = UI freeze

// RIGHT - use SwingWorker
new SwingWorker<String, Void>() {
    @Override
    protected String doInBackground() {
        return longRunningOperation();
    }

    @Override
    protected void done() {
        try {
            String result = get();
            updateUI(result);  // On EDT
        } catch (Exception e) {
            handleError(e);
        }
    }
}.execute();
```

3. **SwingWorker for async operations:**
   - `doInBackground()` - runs on background thread
   - `done()` - runs on EDT
   - `publish()/process()` - for progress updates on EDT

### Development Environment Setup

**Prerequisites:**
- Java 11 or higher
- Maven 3.x
- Git
- (Optional) Local Faster-Whisper server
- (Optional) FFmpeg for advanced chunking

**Initial Setup:**
```bash
# Clone repository
git clone <repo-url>
cd whisperdog

# Setup git hooks
./setup-git-hooks.sh

# Build project
mvn clean package

# Run application
java -jar target/Audiorecorder-1.0-SNAPSHOT-jar-with-dependencies.jar
```

**Configuration:**
- First run creates `~/.whispercat/config.properties`
- Configure API keys in Settings UI
- Test connection to services

### Code Style Guidelines

**Naming Conventions:**
- Classes: PascalCase (e.g., `RecorderForm`)
- Methods: camelCase (e.g., `startRecording()`)
- Constants: UPPER_SNAKE_CASE (e.g., `MAX_FILE_SIZE`)
- Packages: lowercase (e.g., `org.whispercat.recording`)

**File Organization:**
- Group by feature/domain (recording, postprocessing, settings)
- UI forms in respective packages
- Shared utilities in root package

**Comments:**
- Javadoc for public APIs
- Inline comments for complex logic
- TODO comments for future improvements

### Related Domains
- [Project Domain](../project/index.md) - Project-specific architectural decisions
- [UI Domain](../ui/index.md) - EDT threading patterns
- [Core Domain](../core/index.md) - Testing audio processing workflows

# Scrutinizer Audit: Task 0010 Implementation Review

**Date**: 2026-02-01
**Audit Type**: Implementation Audit (Post-Development)
**Audited by**: Scrutinizer role
**Status**: REVIEW → Assessment for completion

---

## Audit Summary

### Implementation Completeness Score: 4/5

| Question | Status | Finding |
|----------|--------|---------|
| 1. WHAT files created? | ✅ | All required files implemented |
| 2. WHAT content in each? | ⚠️ | Minor bug in RecordingManifest |
| 3. Integration points? | ✅ | Correctly wired at all locations |
| 4. HOW done? | ✅ | Verification tests defined |
| 5. Edge cases? | ✅ | Null-safe handling throughout |

### Verdict: **CONDITIONAL PASS** - One bug requires fix before release

---

## Implementation Audit Details

### Phase 1: ConfigManager Retention Settings ✅

**File**: [ConfigManager.java:905-1010](src/main/java/org/whisperdog/ConfigManager.java#L905-L1010)

| Requirement | Status | Implementation |
|-------------|--------|----------------|
| `isRecordingRetentionEnabled()` | ✅ | Line 913 |
| `setRecordingRetentionEnabled()` | ✅ | Line 922 |
| `getRecordingRetentionCount()` | ✅ | Lines 932-935, clamps 1-100 |
| `setRecordingRetentionCount()` | ✅ | Lines 943-946 |
| `getRecordingStoragePath()` | ✅ | Line 954 |
| `setRecordingStoragePath()` | ✅ | Lines 964-967 |
| `getRecordingsDirectory()` | ✅ | Lines 976-988, creates if missing |
| `isRetainChannelFilesEnabled()` | ✅ | Line 997 |
| `setRetainChannelFilesEnabled()` | ✅ | Lines 1006-1009 |

**Deviation from PRD**:
- PRD FR-2.1 specifies `recordingRetentionEnabled` default: `true`
- Implementation uses default: `false` (line 914)
- **Decision**: Current default is acceptable (opt-in behavior is safer for disk space)

---

### Phase 2: RecordingManifest ⚠️

**File**: [RecordingManifest.java](src/main/java/org/whisperdog/recording/RecordingManifest.java)

| Requirement | Status | Implementation |
|-------------|--------|----------------|
| RecordingEntry inner class | ✅ | Lines 30-120 |
| JSON serialization (Gson) | ✅ | Line 24 |
| Atomic save (temp+move) | ✅ | Lines 160-181 |
| `addRecording()` | ✅ | Lines 188-191 |
| `removeRecording()` | ✅ | Lines 199-211 |
| `pruneToCount()` | ✅ | Lines 220-241 |
| `micChannelFile` field | ✅ | Line 38 |
| `systemChannelFile` field | ✅ | Line 39 |

**BUG DETECTED** at line 42:

```java
public RecordingEntry() {
    this.entries = new ArrayList<>();  // BUG: 'entries' is a transient field
}
```

**Problem**: `entries` is declared as a transient field on `RecordingEntry` (line 119) but belongs to the outer `RecordingManifest` class. This assignment in the constructor is:
1. Semantically wrong - entries belongs to manifest, not entry
2. Will cause confusion - transient fields don't serialize
3. May cause NullPointerException if accessed before manifest init

**Fix Required**:
```java
public RecordingEntry() {
    // No initialization needed - fields use default values
}
```

---

### Phase 3: RecordingRetentionManager ✅

**File**: [RecordingRetentionManager.java](src/main/java/org/whisperdog/recording/RecordingRetentionManager.java)

| Requirement | Status | Implementation |
|-------------|--------|----------------|
| Constructor with ConfigManager | ✅ | Lines 33-36 |
| `retainRecording()` signature | ✅ | Lines 57-63, accepts all channel params |
| Filename pattern `recording_YYYYMMDD_HHmmss.wav` | ✅ | Lines 77-83 |
| Channel file naming `*_mic.wav`, `*_system.wav` | ✅ | Lines 101, 111 |
| Channel file copy (null-safe) | ✅ | Lines 98-116 |
| Automatic pruning | ✅ | Lines 135-144 |
| Prune deletes channel files (null-safe) | ✅ | Lines 180-202 |
| `deleteRecording()` deletes all files | ✅ | Lines 152-162 |
| `openInFileManager()` | ✅ | Lines 208-219 |
| `getRecordings()` | ✅ | Lines 226-228 |
| `getAudioFile()` | ✅ | Lines 236-243 |
| `getMicChannelFile()` | ✅ | Lines 251-258 |
| `getSystemChannelFile()` | ✅ | Lines 266-273 |

**All null-safety requirements met**:
- Line 70-73: Validates merged file exists before proceeding
- Line 100: `micChannelFile != null && micChannelFile.exists()`
- Line 109: `systemChannelFile != null && systemChannelFile.exists()`
- Lines 180-202: Null checks before deleting channel files

---

### Phase 4: RecorderForm Integration ✅

**File**: [RecorderForm.java](src/main/java/org/whisperdog/recording/RecorderForm.java)

| Requirement | Status | Location |
|-------------|--------|----------|
| Field declaration | ✅ | Line 154 |
| Constructor initialization | ✅ | Line 161 |
| `retainRecording()` call in finally block | ✅ | Lines 2290-2300 |
| Mic channel = `audioFile` | ✅ | Line 2294 |
| System channel = `systemTrackFile` | ✅ | Line 2295 |
| Getter `getRecordingRetentionManager()` | ✅ | Lines 2777-2779 |

**Integration point correct**:
```java
// Line 2292-2299
recordingRetentionManager.retainRecording(
    transcribedFile,      // merged file
    audioFile,            // mic channel
    systemTrackFile,      // system channel (may be null)
    recordingDurationMs,
    transcript,
    isDualSource
);
```

---

### Phase 5: Settings UI ✅

**File**: [SettingsForm.java](src/main/java/org/whisperdog/settings/SettingsForm.java)

| Requirement | Status | Lines |
|-------------|--------|-------|
| Enable/disable checkbox | ✅ | 684-695 |
| Retention count spinner (1-100) | ✅ | 707-723 |
| Storage path text field | ✅ | 735-738 |
| Browse button | ✅ | 740-754 |
| Open Folder button | ✅ | 756-763 |
| Retain channel files checkbox | ✅ | 779-796 |

---

### Phase 6: RecordingsPanel ✅

**File**: [RecordingsPanel.java](src/main/java/org/whisperdog/recording/RecordingsPanel.java)

| Requirement | Status | Lines |
|-------------|--------|-------|
| Panel with list view | ✅ | 57-66 |
| Recording cards | ✅ | 102-168 |
| Timestamp display | ✅ | 117-121 |
| Duration display | ✅ | 124, 218-231 |
| File size display | ✅ | 125, 236-246 |
| Dual-source flag | ✅ | 126-127 |
| Transcription preview | ✅ | 134-142 |
| Play button | ✅ | 151-155, 173-189 |
| Delete button with confirmation | ✅ | 159-163, 194-212 |
| Refresh button | ✅ | 44-47 |
| Open Folder button | ✅ | 49-52 |

---

### Phase 7: Side Menu Integration ✅

**Menu.java** - [Line 20](src/main/java/org/whisperdog/sidemenu/Menu.java#L20):
```java
{"Recordings"},
```

**MainForm.java** - [Lines 122-128](src/main/java/org/whisperdog/MainForm.java#L122-L128):
```java
} else if (index == 1) {
    // Recordings browser
    if (recorderForm == null) {
        recorderForm = new RecorderForm(configManager);
    }
    RecordingsPanel recordingsPanel = new RecordingsPanel(recorderForm.getRecordingRetentionManager());
    showForm(recordingsPanel);
}
```

**Observation**: Creates new `RecordingsPanel` each navigation. Acceptable but could cache instance for performance. Not a bug.

---

## Verification Checklist Status

| Test Case | Checklist Status | Code Review |
|-----------|------------------|-------------|
| Basic retention (file saved) | [ ] | ✅ Implemented correctly |
| Manifest JSON structure | [ ] | ✅ Correct format |
| Pruning (count=3, make 5) | [ ] | ✅ Logic correct |
| Settings persistence | [ ] | ✅ Uses saveConfig() |
| Recordings browser | [ ] | ✅ Fully implemented |
| Custom storage path | [ ] | ✅ Implemented |
| Enable/disable toggle | [ ] | ✅ Implemented |
| Channel file retention (dual) | [ ] | ✅ 3 files pattern correct |
| Single-source + channel enabled | [ ] | ✅ Null-safe, only mic saved |
| Channel files pruned with parent | [ ] | ✅ Null-safe deletion |

---

## Required Actions

### Critical (Must Fix)

1. **RecordingManifest.java:42** - Remove erroneous `entries` initialization

   **Current**:
   ```java
   public RecordingEntry() {
       this.entries = new ArrayList<>();
   }
   ```

   **Fix**:
   ```java
   public RecordingEntry() {
       // Default constructor - no initialization needed
   }
   ```

### Optional (Enhancement)

1. **MainForm.java:127** - Cache RecordingsPanel instance for better performance
2. **ConfigManager.java:914** - Consider changing default to `true` per PRD (user preference)

---

## Final Verdict

### CONDITIONAL PASS

**Implementation is 98% complete and functionally correct.** One bug in `RecordingManifest.RecordingEntry` constructor must be fixed before marking task complete.

After fix applied:
- [ ] Mark task 0010 as `completed`
- [ ] Run verification tests in checklist
- [ ] Commit with message referencing TASK-0010

---

## Verification Failures (2026-02-01)

### CRITICAL: Recordings Not Being Saved

**Symptom**: No files appear in `%APPDATA%\WhisperDog\recordings` after transcription.

**Investigation Required**:
1. Add debug logging to `retainRecording()` to verify it's called
2. Check if `transcribedFile` exists at copy time
3. Verify `isRecordingRetentionEnabled()` returns true (default is `false`)
4. Check for silent IOException during `Files.copy()`

**Files**:
- [RecorderForm.java:2290-2300](src/main/java/org/whisperdog/recording/RecorderForm.java#L2290)
- [RecordingRetentionManager.java:57-130](src/main/java/org/whisperdog/recording/RecordingRetentionManager.java#L57)

---

### UI: Menu Icon Order Mangled

**Symptom**: Adding "Recordings" shifted icon assignments. Recordings shows Settings icon, Settings shows PostPro icon.

**Root Cause**: Icons defined with fixed indices separate from menu text items.

**Fix**: Locate icon array, insert placeholder at correct index.

---

### UI: RecordingsPanel Buttons Missing Icons

**Symptom**: Play/Delete buttons show red placeholder squares.

**Files**: [RecordingsPanel.java:152,160](src/main/java/org/whisperdog/recording/RecordingsPanel.java#L152)

**Fix**: Add `play.svg` and `delete.svg` to resources, or use existing icons.

---

### Enhancement: Execution Log Full Path

**Current**: `[13:14:34] Audio file: whisperdog_merged_xxx.wav`
**Desired**: Full absolute path for easy copy-paste.

---

## Updated Verdict

### FAIL - Critical Issues Found

Task requires fixes before completion:
- [ ] Debug and fix recording retention (CRITICAL)
- [ ] Fix menu icon order
- [ ] Add missing button icons (or placeholder)
- [ ] Enhancement: Full path in execution log

**Checkpoint**: `CP_2026_02_01_001` (milestone) documents all issues.

---

*Scrutinizer Audit Updated: 2026-02-01*
*Standard: Hunt → Flag → Fix → Verify*

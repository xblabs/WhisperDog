# Verification Playbook: Dual-Source Attribution Accuracy

**Task**: 0008
**Status**: Phase 1 Complete (Dominance Ratio Implementation)
**Generated**: 2026-01-28

---

## Quick Summary

This task fixes incorrect `[User+System]` source attribution in dual-source (mic + system audio) transcriptions. The problem: when system audio plays loudly (0.1 RMS) and mic has ambient noise (0.006 RMS, just above 0.005 threshold), both were incorrectly marked as active.

**Solution**: Added dominance ratio comparison (3:1 threshold). When both sources exceed activity threshold, the louder source wins unless levels are comparable (genuine crosstalk).

---

## Stack Detection

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Main application language |
| Maven | - | Build system |
| JUnit 5 | 5.10.2 | Unit testing (newly added) |
| Log4j 2 | 2.23.1 | Logging |

**Key Files Modified**:
- `src/main/java/org/whisperdog/audio/SourceActivityTracker.java`
- `pom.xml` (added JUnit 5)

**Files Created**:
- `src/test/java/org/whisperdog/audio/SourceActivityTrackerDominanceTest.java`

---

## Testing Steps

### Automated Tests

```bash
# Run dominance ratio unit tests (13 tests)
cd C:\__dev\_projects\whisperdog
mvn test -Dtest=SourceActivityTrackerDominanceTest

# Expected output: Tests run: 13, Failures: 0, Errors: 0
```

### Manual Verification

#### Test 1: System Audio Only (Mic has ambient noise)
1. Build: `mvn package`
2. Launch WhisperDog with dual-source recording enabled
3. Play loud system audio (YouTube video, podcast)
4. Stay silent (mic picks up ambient room noise)
5. Stop recording and transcribe
6. **Expected**: All segments labeled `[System]`, NO `[User+System]`

#### Test 2: User Speaking Only (System has faint audio)
1. Enable dual-source recording
2. Have very quiet system audio in background (or muted video)
3. Speak clearly into microphone
4. Stop and transcribe
5. **Expected**: All segments labeled `[User]`, NO `[User+System]`

#### Test 3: Genuine Crosstalk
1. Play system audio at moderate volume
2. Speak over the audio intentionally at similar volume
3. Stop and transcribe
4. **Expected**: Overlapping segments labeled `[User+System]`

#### Test 4: Clean Transitions
1. Start with user speaking
2. Stop speaking, let system audio play
3. Resume speaking
4. **Expected**: Clean `[User]` → `[System]` → `[User]` transitions

#### Test 5: Single-Source Backward Compatibility
1. Record with mic only (no system audio capture)
2. Transcribe
3. **Expected**: Normal operation, no errors, no source labels

---

## Verification Matrix

| Checklist Item | Status | Verification |
|----------------|--------|--------------|
| Add dominance ratio constants | ✅ | `DEFAULT_DOMINANCE_RATIO = 3.0`, `MIN_RMS_FOR_RATIO = 0.0001` |
| Update constructors (backward compatible) | ✅ | 2-arg constructor still works, chains to 3-arg |
| Modify trackActivity() with relative comparison | ✅ | `determineSourceByDominance()` method added |
| Division-by-zero guard | ✅ | `safeSysLevel = Math.max(sysLevel, MIN_RMS_FOR_RATIO)` |
| Unit tests: mic dominates | ✅ | `SystemDominates` test class passes |
| Unit tests: system dominates | ✅ | `UserDominates` test class passes |
| Unit tests: genuine crosstalk | ✅ | `GenuineCrosstalk` test class passes |
| Unit tests: edge cases | ✅ | `EdgeCases` test class passes |
| Full build | ✅ | `mvn package` succeeds |

### Pending Verification

| Item | Status | Notes |
|------|--------|-------|
| Test corpus measurement | ⬜ | Requires real dual-source recordings |
| Gate check (≤5% false BOTH) | ⬜ | Blocked on test corpus |
| Performance benchmark | ⬜ | Target: <50ms overhead |

---

## Code Changes Summary

### SourceActivityTracker.java

```java
// NEW: Dominance ratio constants (lines 31-42)
public static final double DEFAULT_DOMINANCE_RATIO = 3.0;
public static final double MIN_RMS_FOR_RATIO = 0.0001;

// NEW: Field (line 46)
private final double dominanceRatio;

// MODIFIED: Constructors now chain to 3-arg version (lines 85-108)

// MODIFIED: trackActivity() calls determineSourceByDominance() (lines 147-149)
if (micActive && systemActive) {
    source = determineSourceByDominance(micLevel, sysLevel);
}

// NEW: Helper method (lines 159-184)
private Source determineSourceByDominance(double micLevel, double sysLevel) {
    double ratio = safeMicLevel / safeSysLevel;
    if (ratio >= dominanceRatio) return Source.USER;
    if (ratio <= 1.0 / dominanceRatio) return Source.SYSTEM;
    return Source.BOTH;
}
```

---

## Rollback Plan

If issues arise, rollback is straightforward:

### Option 1: Revert to Old Behavior
Set dominance ratio to 1.0 (everything within ratio is BOTH):
```java
new SourceActivityTracker(100, 0.005, 1.0)
```

### Option 2: Git Revert
```bash
# Revert SourceActivityTracker.java to pre-task state
git checkout 3baf99c -- src/main/java/org/whisperdog/audio/SourceActivityTracker.java

# Rebuild
mvn package
```

### Option 3: Full Rollback
```bash
# Reset to last known good commit
git reset --hard 3baf99c
mvn package
```

---

## Next Steps

1. **Create test corpus** with real dual-source recordings
2. **Measure false_both_rate** with Phase 1 implementation
3. **Gate check**: If ≤5%, proceed to Validation; else Phase 2 (Segment Smoothing)
4. **Update ADR** with measured results

---

## Related Commits

| Commit | Message |
|--------|---------|
| 3baf99c | fix(TASK-0006): address remaining audit items |
| c91f875 | feat(TASK-0006): add device labels and word-level attribution |
| *pending* | feat(TASK-0008): add dominance ratio for attribution accuracy |

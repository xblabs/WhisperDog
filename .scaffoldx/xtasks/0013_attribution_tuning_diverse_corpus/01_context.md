# Context: Attribution Tuning with Diverse Test Corpus

## Why Diverse Testing Matters

The current attribution algorithm uses two key parameters:
- **RMS_THRESHOLD** (0.005): Minimum signal level to consider a source "active"
- **DOMINANCE_RATIO** (3.0): How much louder one source must be to "win" over the other

These values were tuned against a single recording of a user speaking over a YouTube video (Manni the train driver). This scenario has clear characteristics:
- User speech is close-mic'd, high RMS when active
- System audio is consistent volume, clear speech
- Minimal overlap between speakers

But real-world usage is messier. Each scenario below represents a different acoustic profile that could challenge our heuristics.

---

## Test Scenarios with Reasoning

### Scenario 1: Rapid Speaker Alternation
**Example**: Language learning app with call-and-response prompts

**Why it's challenging**:
- Short utterances (1-3 seconds) from each source
- Minimal silence gaps between speakers
- Risk: `mergeShortSegments()` might incorrectly merge rapid switches into BOTH
- Risk: Segment boundaries might land mid-word, causing split attribution

**What we'd learn**: Whether the 300ms minimum segment duration is appropriate, or if we need scenario-specific tuning.

**Ground truth complexity**: HIGH - requires precise timestamp annotations at word boundaries.

---

### Scenario 2: Music Playback with Commentary
**Example**: User commenting over a music video or podcast intro with music

**Why it's challenging**:
- Music has continuous, high RMS - never "silent"
- User voice must dominate over sustained musical energy
- Quiet passages in music might flip attribution incorrectly
- Bass-heavy music has different RMS profile than speech

**What we'd learn**: Whether dominance ratio needs frequency-aware weighting, or if simple RMS comparison is sufficient for typical music volumes.

**Ground truth complexity**: MEDIUM - music sections are clear, but transitions during fade-ins/outs are ambiguous.

---

### Scenario 3: Video Call with User Participation
**Example**: Zoom/Teams meeting where system plays remote participants, user also speaks

**Why it's challenging**:
- System audio contains multiple distinct speakers (should all be [System])
- User might interject, creating genuine [User+System] crosstalk
- Remote audio often has compression artifacts affecting RMS profile
- Echo cancellation might leak user voice into system channel

**What we'd learn**: Whether the algorithm correctly handles multi-speaker system audio as a single source, and if genuine crosstalk is preserved.

**Ground truth complexity**: HIGH - crosstalk moments require subjective judgment on "who was really speaking."

---

### Scenario 4: Low-Volume Background Content
**Example**: User working while a video plays quietly in background

**Why it's challenging**:
- System audio RMS near or below threshold
- User speech clearly dominant but system never truly "silent"
- Risk: Low system audio might still trigger [User+System] due to threshold sensitivity

**What we'd learn**: Whether RMS_THRESHOLD is appropriate for quiet playback scenarios, or if we need dynamic thresholding.

**Ground truth complexity**: LOW - user speech clearly dominant, but "is the background audible?" is subjective.

---

### Scenario 5: Echo/Feedback Loop
**Example**: User in a call hearing their own voice played back through speakers

**Why it's challenging**:
- User voice appears in BOTH channels simultaneously
- Slight delay between mic capture and system playback
- This is a TRUE [User+System] scenario, but might sound like single-source

**What we'd learn**: Whether the algorithm correctly identifies this as genuine dual-source, and if the delay causes temporal misalignment in attribution.

**Ground truth complexity**: MEDIUM - technically dual-source, but semantically single-speaker.

---

### Scenario 6: Silence with Ambient Noise
**Example**: Recording running with no intentional audio, just room noise and computer fan

**Why it's challenging**:
- Both sources have low-level noise above zero
- Neither source has "real" content
- Risk: Noise floor might exceed threshold and trigger false attribution

**What we'd learn**: Whether RMS_THRESHOLD correctly filters ambient noise, or if we need noise floor calibration.

**Ground truth complexity**: LOW - all segments should be NONE or very short spurious detections.

---

### Scenario 7: Asymmetric Audio Quality
**Example**: High-quality condenser mic vs. compressed system audio from streaming service

**Why it's challenging**:
- Mic audio has full dynamic range, higher peak RMS
- System audio compressed/normalized, flatter RMS profile
- Equal "loudness" perception might have different RMS values

**What we'd learn**: Whether perceived loudness vs. measured RMS causes attribution bias toward the less-compressed source.

**Ground truth complexity**: MEDIUM - requires careful volume matching during recording setup.

---

### Scenario 8: Extended Single-Source with Brief Interjections
**Example**: Listening to a 20-minute podcast, occasionally saying "hmm" or "interesting"

**Why it's challenging**:
- Long [System] segments punctuated by very brief [User] moments
- Brief user sounds might be below minimum segment duration
- Risk: User interjections get merged into surrounding [System] segments

**What we'd learn**: Whether minimum segment duration should vary based on surrounding context, or if brief interjections should be preserved regardless of duration.

**Ground truth complexity**: MEDIUM - brief sounds are clearly audible but deciding if they're "meaningful" is subjective.

---

### Scenario 9: Gaming with Voice Chat
**Example**: Playing a game with dynamic audio while talking on Discord

**Why it's challenging**:
- Game audio highly variable (explosions, music, silence)
- Voice chat audio variable quality (different users, distances from mic)
- User voice competing with sudden loud game events

**What we'd learn**: Whether the algorithm handles sudden RMS spikes (explosions) without misattributing during the spike.

**Ground truth complexity**: HIGH - game audio varies wildly, making temporal alignment tricky.

---

### Scenario 10: Genuine Sustained Crosstalk
**Example**: User arguing with someone on a video call, both talking over each other

**Why it's challenging**:
- This SHOULD produce [User+System] labels - genuine simultaneous speech
- Current algorithm might incorrectly pick a "winner" if dominance ratio triggers
- Need to preserve crosstalk when it's real

**What we'd learn**: What dominance ratio correctly distinguishes "one source with noise" from "two sources actually talking."

**Ground truth complexity**: HIGH - subjective judgment on whether overlap is crosstalk or just close timing.

---

## Parameter Tuning Strategy

After collecting diverse recordings with retention enabled:

1. **Baseline**: Run all scenarios with current parameters, measure error rates per scenario
2. **Identify outliers**: Which scenarios exceed 5% error?
3. **Hypothesis**: For each outlier, reason about which parameter change would help
4. **Experiment**: Adjust parameter, re-run ALL scenarios (regression check)
5. **Trade-off analysis**: Document any scenarios where improvement in one causes regression in another
6. **Final parameters**: Choose values that minimize max error across scenarios (minimax)

## Questions to Answer

1. Is a single parameter set sufficient for all scenarios, or do we need configurable profiles?
2. Should we add frequency-aware RMS (e.g., weight speech frequencies higher)?
3. Is the fixed dominance ratio appropriate, or should it adapt to signal levels?
4. How do we handle scenarios with subjective ground truth (crosstalk, brief interjections)?

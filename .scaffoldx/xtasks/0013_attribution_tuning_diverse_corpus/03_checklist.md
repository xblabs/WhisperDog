# Checklist: Attribution Tuning with Diverse Test Corpus

> **Context Recovery**: Find first unchecked `[ ]` item → resume from there.
> **Blocking Dependency**: Task 0010 (Recording Retention) must be complete first.

---

## Phase 0: Prerequisites

- [ ] Verify Task 0010 (Recording Retention) is complete
- [ ] Confirm retention flag preserves mic + system WAV files after merge
- [ ] Allocate storage location for test corpus

---

## Phase 1: Corpus Collection

### Scenario 1: Rapid Speaker Alternation
- [ ] Record language-learning or call-and-response scenario (3-5 min)
- [ ] Export retained WAV files to `artifacts/test_corpus/rapid_alternation/`
- [ ] Annotate ground truth with segment timestamps and expected labels
- [ ] Note boundary ambiguity confidence levels

### Scenario 2: Music Playback with Commentary
- [ ] Record user commenting over music/podcast intro (3-5 min)
- [ ] Export retained WAV files to `artifacts/test_corpus/music_commentary/`
- [ ] Annotate ground truth, noting fade transitions
- [ ] Document music genre and typical RMS characteristics

### Scenario 3: Video Call Participation
- [ ] Record meeting with remote speakers + user interjections (5 min)
- [ ] Export retained WAV files to `artifacts/test_corpus/video_call/`
- [ ] Annotate ground truth, marking genuine crosstalk moments
- [ ] Note any echo cancellation artifacts observed

### Scenario 4: Low-Volume Background Content
- [ ] Record user working with quiet video in background (3 min)
- [ ] Export retained WAV files to `artifacts/test_corpus/low_volume/`
- [ ] Annotate ground truth with threshold for "audible" background
- [ ] Document playback volume setting used

### Scenario 5: Echo/Feedback Loop
- [ ] Record call scenario where user hears own voice delayed (2 min)
- [ ] Export retained WAV files to `artifacts/test_corpus/echo_feedback/`
- [ ] Annotate as genuine dual-source throughout
- [ ] Measure delay between mic and system channels

### Scenario 6: Silence with Ambient Noise
- [ ] Record idle session with room noise only (2 min)
- [ ] Export retained WAV files to `artifacts/test_corpus/ambient_noise/`
- [ ] Annotate as NONE throughout (no intentional content)
- [ ] Measure RMS of ambient noise for threshold calibration

### Scenario 7: Asymmetric Audio Quality
- [ ] Record with high-quality mic + compressed stream source (3 min)
- [ ] Export retained WAV files to `artifacts/test_corpus/asymmetric_quality/`
- [ ] Annotate ground truth
- [ ] Document mic model and stream source bitrate

### Scenario 8: Extended Single-Source with Interjections
- [ ] Record podcast listening with occasional user sounds (10-15 min)
- [ ] Export retained WAV files to `artifacts/test_corpus/long_with_interjections/`
- [ ] Annotate brief user sounds with confidence levels
- [ ] Note which interjections are "meaningful" vs. ambient

### Corpus Validation
- [ ] Verify all 8 scenarios have mic.wav, system.wav, and ground_truth.json
- [ ] Run smoke test: load all scenarios in measurement harness
- [ ] Calculate total corpus duration and storage size

---

## Phase 2: Measurement & Tuning

### Baseline Measurement
- [ ] Run all scenarios with current parameters (THRESHOLD=0.005, RATIO=3.0)
- [ ] Record per-scenario error rates in measurement report
- [ ] Identify scenarios exceeding 5% error threshold
- [ ] Document which error type dominates (false_both vs. false_single vs. boundary)

### Parameter Search
- [ ] Implement parameter sweep in test harness (or script)
- [ ] Run exhaustive search over defined parameter space
- [ ] Record aggregate metrics for each parameter combination
- [ ] Identify Pareto-optimal parameter sets (no dominated options)

### Trade-off Analysis
- [ ] For top 3 parameter sets, document per-scenario error rates
- [ ] Identify any scenario where improvement causes regression elsewhere
- [ ] Decide on final parameters with documented reasoning
- [ ] If no single set works for all, consider scenario profiles (future task)

### Implementation
- [ ] Update SourceActivityTracker with tuned parameter values
- [ ] Run full test suite to verify no regressions
- [ ] Update constants documentation with tuning rationale

---

## Phase 3: Regression Suite

- [ ] Create `AttributionRegressionTest.java` that loads corpus
- [ ] Test fails if any scenario exceeds 5% error with committed parameters
- [ ] Add regression test to CI/build pipeline
- [ ] Document how to add new scenarios to corpus

---

## Wrap-up

- [ ] Update ADR-002 with tuning methodology and final results
- [ ] Create brief summary of findings for project knowledge base
- [ ] Archive measurement reports and parameter search logs
- [ ] Mark task complete

---

**Reference**: Full scenario reasoning in `01_context.md`
**Implementation details**: `02_implementation_plan.md`
**Dependency**: Task 0010 - Recording Retention System

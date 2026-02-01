# Artifacts Directory

## Purpose

This directory collects **input materials** for Task 0008: Dual-Source Attribution Accuracy.

## Relevant Artifacts

Place the following here for reference during implementation:

- **Test recordings**: Dual-source WAV files exhibiting the attribution problem
- **Expected outputs**: Manually corrected transcripts showing correct attribution
- **RMS analysis logs**: Debug output from SourceActivityTracker showing RMS values

## Related Context

- **ADR**: [002_dual_source_attribution.md](../../../xcontext/development/adr/002_dual_source_attribution.md)
- **Implementation**: [SourceActivityTracker.java](../../../../src/main/java/org/whisperdog/audio/SourceActivityTracker.java)

## Usage

```bash
# Populate task from artifacts
x-task-populate 0008

# Or specify different source
x-task-populate 0008 --from-context <other-source>
```

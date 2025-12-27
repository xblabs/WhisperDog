---
title: "ADR-001: Pipeline API Call Chaining"
adr_id: "001"
status: accepted
date: 2025-12-30
context_type: adr
domain: development
tags: [adr, pipeline, optimization, api, chaining, llm]
---

# ADR-001: Pipeline API Call Chaining

## Status

**Accepted** - Implemented in `PostProcessingService.java`

## Context

Multi-unit pipelines make separate API calls per unit, even when using the same model:
- 3 GPT-4 units = 3 API calls = ~$0.15, ~9 seconds
- Each call throws away context from previous steps
- Users pay 3x for what could be a single conversation

**Constraint**: Each unit must see the previous unit's output as `{{input}}`. This chaining semantic must be preserved.

## Decision

Automatically detect consecutive same-model units and compile them into a single chained prompt using **explicit variable scoping (Style 1)**:

```
INPUT_TEXT: [transcript]
STEP_1: Summarize INPUT_TEXT → STEP1_OUTPUT
STEP_2: Fix grammar in STEP1_OUTPUT → STEP2_OUTPUT
STEP_3: Add emojis to STEP2_OUTPUT → STEP3_OUTPUT
Return only: STEP3_OUTPUT
```

**Result**: 3 GPT-4 units = 1 API call = ~$0.06, ~4 seconds

## Alternatives Considered

### Alternative 1: Simple Concatenation

```
"Summarize this, then fix grammar, then add emojis: [transcript]"
```

**Rejected**: No clear data flow. Model doesn't know each step operates on previous output.

### Alternative 2: Natural Language References

```
"Take the summary and fix its grammar..."
```

**Rejected**: ~5% lower reliability. Ambiguous references ("output from Transformation 1"), harder to debug failures.

### Alternative 3: XML-Delimited Structure

```xml
<FRAGMENT id="1">
  <INSTRUCTION>Summarize: <INITIAL_INPUT/></INSTRUCTION>
</FRAGMENT>
<FRAGMENT id="2">
  <INSTRUCTION>Fix grammar in: <FRAGMENT id="1"/></INSTRUCTION>
</FRAGMENT>
```

**Rejected**: 15-20% more tokens due to XML overhead. Some models parse XML inconsistently.

## Why Style 1 Won

- Highest reliability across models (GPT-4, GPT-3.5, Open WebUI)
- Clear debugging (explicit variable names)
- Scales to 10+ units without confusion
- No ambiguity in data flow
- Minimal token overhead (~100-200 tokens)

## Chain Breakers

Optimization breaks when encountering:

- Different models (GPT-4 → GPT-3.5)
- Different providers (OpenAI → Open WebUI)
- Text Replacement units (post-processing, not promptable)

**Example**:
```
Unit 1: GPT-4 "Summarize"        ← Batch 1
Unit 2: GPT-4 "Fix Grammar"      ← Batch 1
Unit 3: Text Replace "um" → ""   ← Batch 2 (breaks chain)
Unit 4: GPT-4 "Add Emojis"       ← Batch 3

Result: 3 API calls (was 4 without optimization)
```

## Consequences

### Positive

- 2-3x faster (single network round trip)
- 2-3x cheaper (fewer request fees)
- Context preservation across steps
- Automatic - no user configuration needed
- Fully transparent in console logs

### Negative

- Harder to isolate which step failed in chained prompts
- Falls back to individual calls when optimization not possible
- Minor token overhead for prompt structure

### Future Considerations

1. **Retry fallback**: If chained fails, execute individually
2. **Parallel execution**: For independent units
3. **Cost estimation**: Show savings before execution

## Related

- [Core Domain](../../core/index.md) - Pipeline processing workflow
- [API Domain](../../api/index.md) - Transcription client implementations

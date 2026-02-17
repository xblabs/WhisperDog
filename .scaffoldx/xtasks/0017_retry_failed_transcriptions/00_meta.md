---
id: "0017"
name: Audio recovery and crash resilience
status: todo
priority: medium
tags: [recording, recovery, ui, crash-resilience, data-loss]
created: 2026-02-17
related_issues: [ISS_00012]
---

# Task 0017: Audio recovery and crash resilience

Follow-up to ISS_00012. Two complementary capabilities to close remaining data-loss gaps:

1. **Retry failed transcriptions** — Detect preserved files from failed transcriptions and offer in-app re-submission (startup dialog + menu item)
2. **Incremental WAV writes** — Stream audio to disk during recording so partial data survives JVM crashes

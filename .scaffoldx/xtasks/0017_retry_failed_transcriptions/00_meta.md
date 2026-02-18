---
id: "0017"
name: Retry failed transcriptions
status: todo
priority: medium
tags: [recording, recovery, ui, data-loss]
created: 2026-02-17
related_issues: [ISS_00012]
---

# Task 0017: Retry failed transcriptions

Follow-up to ISS_00012. ISS_00012 added file preservation on transcription failure, but users have no in-app way to retry those preserved files. They see console log paths and must manually locate files.

This task adds a scanner, recovery dialog, and re-submission pipeline so users can retry failed transcriptions from within the app.

**Scope**: Recovery UI and re-submission only. Incremental WAV writes (crash resilience during recording) are tracked separately.

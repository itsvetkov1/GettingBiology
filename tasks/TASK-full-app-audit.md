# TASK-full-app-audit

## Objective
Execute the GettingBiology App Audit (Greatest-Buff) prompt against the live codebase and produce a full audit report.

## Context
The audit prompt is at /opt/alpharius/wiki/prompts/gettingbiology-app-audit-greatest.md. It defines a 14-dimension, 8-phase, evidence-backed audit of the GettingBiology Android app (com.znam.app). The app is a Bulgarian biology quiz app built with Kotlin, Jetpack Compose, Room, and Koin DI.

The modernization/phase-0-1 branch is active with recent work: hints system, language toggle, English translation of UI, R8 minification, asset cleanup, Compose migration, and bug fixes.

## Scope
IN: Execute all 8 phases (A through H) of the audit prompt. Produce the full report in the required format (8 sections). Run actual builds, lint, SQLite integrity checks, and code inspection.
OUT: Do not modify the codebase. This is a read-only audit. Do not deploy anything.

## Steps
1. Read the audit prompt: cat /opt/alpharius/wiki/prompts/gettingbiology-app-audit-greatest.md
2. cd /home/alpharius/projects/GettingBiology && git checkout modernization/phase-0-1
3. Execute PHASE A through PHASE H in order, following the prompt's test plan exactly
4. Produce the full markdown report per the OUTPUT FORMAT section
5. Save the report to /home/alpharius/projects/GettingBiology/AUDIT-REPORT.md

## Deliverables
- /home/alpharius/projects/GettingBiology/AUDIT-REPORT.md (full audit report)

## Verification
1. Report contains all 8 required sections
2. Every finding has evidence in the Appendix
3. Score math is shown for all 14 dimensions
4. Self-check passes (all boxes in the prompt's self-check section)

## Files / References
- /opt/alpharius/wiki/prompts/gettingbiology-app-audit-greatest.md (the audit prompt)
- /home/alpharius/projects/GettingBiology/ (the repo)
- /opt/alpharius/wiki/projects/getting-biology.md (wiki context)

## Notes
- Build requires JDK + Android SDK on Hydra. If not available, skip build-dependent steps (A2, A3) and mark as Not Assessed.
- The three SQLite databases are in app/src/main/assets/
- R8 is enabled for both debug and release builds

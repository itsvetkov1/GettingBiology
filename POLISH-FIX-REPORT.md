# GettingBiology Polish Pass

Branch: polish/nth-and-strings
Base: 029b74c
Final gate: build/lint/test PASS

| ID | Problem | Fix commit SHA | Status |
|---|---|---|---|
| NTH-1 | Speed Demon threshold was 90s but achievement description said 60s. | 47d8be8 | FIXED |
| NTH-2 | Manifest targetSdkVersion/tools:targetApi 34 conflicted with Gradle targetSdk 35. | 167e92b | FIXED |
| NTH-3 | KSP schemaLocation was active while AppDatabase still had exportSchema=false; StatsDatabase schemas are required by migration validation. | 7d6a43f | FIXED / DOCUMENTED: exportSchema=true |
| NTH-4 | ZnamTheme cast LocalView context directly to Activity. | 75de4dc | FIXED |
| NTH-5 | Debug build enabled minify/shrink while debuggable, causing Gradle warning and ambiguous R8 smoke testing. | 697cf3c | FIXED |
| strings | Achievement display names/descriptions had hardcoded Kotlin text and Bulgarian values-en resources. | 857f850 | FIXED |

Verification:
- Per-fix scripts: /tmp/verify_nth1.sh, /tmp/verify_nth2.sh, /tmp/verify_nth3.sh, /tmp/verify_nth4.sh, /tmp/verify_nth5.sh, /tmp/verify_strings.sh all PASS after their fixes.
- Final full gate after strings commit: ./gradlew clean assembleDebug lint test --console=plain PASS.
- Unit tests: 82 passed / 0 failed.
- NTH-5 warning check: zero isMinifyEnabled warning matches.

Notes:
- No merge to master.
- No remote push.
- No SDK/dependency bumps.
- No release signing changes.
- Existing untracked LOGIC-REVIEW.md was not touched.

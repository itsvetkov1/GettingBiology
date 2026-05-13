# GettingBiology Phase-1 Rework

Branch: `modernization/phase-1`
Final gate: PASS — `/tmp/verify_mf1.sh` through `/tmp/verify_sf9.sh`, `./gradlew clean assembleDebug lint test`, release shrink check.
Final lint: 0 errors / 0 warnings.
Final tests: 40 passed / 0 failed.
Release shrink: debug `18,058,859` bytes → release `9,643,999` bytes = 46.60% smaller.

| ID | original problem (1 line) | fix commit SHA | verification evidence (build/test/manual) | status |
|---|---|---|---|---|
| MF-1 | Result screen could navigate before gamification rewards were computed. | ef6db78 | `/tmp/verify_mf1.sh` PASS; final `./gradlew clean assembleDebug lint test` PASS. | FIXED |
| MF-2 | Daily challenge completion used generic quiz activity state instead of explicit challenge state. | b000496, 5b26055 | `/tmp/verify_mf2.sh` PASS; migration/unit tests included in final 40/40 PASS. | FIXED |
| MF-3 | Room migration SQL defaults did not match entity schema defaults. | 9b8fd7d | `/tmp/verify_mf3.sh` PASS; migration tests included in final 40/40 PASS. | FIXED |
| SF-1 | XP streak bonus used stale pre-rollover streak. | 5cbaa63 | `/tmp/verify_sf1.sh` PASS; final gate PASS. | FIXED |
| SF-2 | Daily challenge XP bonus constant was unused. | b000496 | `/tmp/verify_sf2.sh` PASS; final gate PASS. | FIXED |
| SF-3 | First correct spaced-repetition answer scheduled the second interval. | 21cd95a | `/tmp/verify_sf3.sh` PASS; final gate PASS. | FIXED |
| SF-4 | Answer feedback relied on color only. | c9896d1 | `/tmp/verify_sf4.sh` PASS; final gate PASS. | FIXED |
| SF-5 | New gamification/daily-challenge UI strings bypassed BG/EN resources. | 875a1f4 | `/tmp/verify_sf5.sh` PASS; final gate PASS. | FIXED |
| SF-6 | Blanket app ProGuard keep rule prevented meaningful shrink. | 77afdef | `/tmp/verify_sf6.sh` PASS; release APK 46.60% smaller than debug APK. | FIXED |
| SF-7 | Compose `StateFlow` collection used lifecycle-unaware `collectAsState`. | 9537f25 | `/tmp/verify_sf7.sh` PASS; final gate PASS. | FIXED |
| SF-8 | Room database construction bypassed `DatabaseProvider`. | f185946 | `/tmp/verify_sf8.sh` PASS; final gate PASS. | FIXED |
| SF-9 | Lint reported 97 warnings. | 1f12665 | `/tmp/verify_sf9.sh` PASS; final lint 0 errors / 0 warnings. | FIXED |

Deferred: none.
Blockers: none.
Merge readiness: PASS.

# GettingBiology post-review logic fix report

Branch: `improvements/post-merge-review`
Base: `b476680`
Final gate: `./gradlew clean assembleDebug lint test --console=plain` PASS
Unit tests: debug 41/41 pass, release 41/41 pass
Build/lint: assembleDebug PASS; lint PASS. Kotlin deprecation warnings are pre-existing API deprecations in transition calls/status bar color.

| ID | Original problem | Fix commit SHA | Verification | Status |
|---|---|---:|---|---|
| BUG-1 | Cached quiz DB was closed by `QuizViewModel` and then reused. | `2d60447` | Final gate PASS; repeated DB lifecycle no longer closes provider-owned cache. | FIXED |
| BUG-2 | Gamification read-modify-write was not atomic. | `cfed287` | `GamificationManagerTest.concurrentCompletionsAreSerializedByMutex`; final gate PASS. | FIXED |
| BUG-3 | Spaced repetition interval used pre-answer streak state. | `ce7e5b6` | `QuestionPerformanceTest.computeNextInterval_usesPostAnswerConsecutiveCorrectSequence`; final gate PASS. | FIXED |
| BUG-4 | Smart selector could repeat just-answered questions immediately. | `c018085` | `SmartQuestionSelectorTest.immediateSecondQuiz_excludesRecentIdsWhenPoolIsLargeEnough`; final gate PASS. | FIXED |
| BUG-5 | Completed daily challenge still launched as active challenge. | `9de1c17` | `DailyChallengeManagerTest.completedChallengeDoesNotAwardAgain`; final gate PASS. | FIXED |
| IMP-1 | Day logic used direct `LocalDate.now()` calls instead of injectable clock. | `5d4b12b` | Fixed-clock tests in `DailyChallengeManagerTest` and `GamificationManagerTest`; final gate PASS. | FIXED |
| IMP-2 | Daily challenge rotation was off by one on Jan 1. | `515b272` | `DailyChallengeManagerTest.janFirstMapsToFirstChallengeType`; final gate PASS. | FIXED |
| IMP-3 | Streak XP bonus could grow without bound. | `8c95248` | `GamificationManagerTest` XP/streak paths; final gate PASS. | FIXED |
| IMP-4 | Achievement unlock persistence was split from profile update. | `cfed287` | `GamificationManagerTest.crossingAchievementThresholdUnlocksNewAchievement`; final gate PASS. | FIXED |
| IMP-5 | Migration tests did not validate v1→4/v2→4/v3→4 or content/default preservation. | `9fa28c4`, `a002ddd` | `StatsDatabaseMigrationTest` covers v1→4, v2→4, v3→4 with `quiz_sessions` preservation and v4 daily challenge defaults; final gate PASS. | FIXED |
| IMP-6 | Stats dashboard created a profile just by viewing stats. | `423d8d7` | Final gate PASS; stats path now reads existing profile only. | FIXED |
| IMP-7 | Reward summary lacked XP breakdown. | `65abeb6` | Final gate PASS; reward result carries/display XP components. | FIXED |
| Phase F tests | Logic coverage gaps for gamification, level math, performance intervals, smart selector, daily challenge. | `2de3b98` | `GamificationManagerTest`, `LevelMathTest`, `QuestionPerformanceTest`, `SmartQuestionSelectorTest`, `DailyChallengeManagerTest`; final gate PASS. | FIXED |

Additional notes:
- No merge to master.
- No remote push.
- No SDK/dependency bumps.
- `LOGIC-REVIEW.md` remained untracked/off-limits and was not modified.

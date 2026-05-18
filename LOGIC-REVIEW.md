# GettingBiology phase-1 Logic Review
**Date:** 2026-05-17
**Reviewer:** dev (hermes)
**HEAD:** b47668054c071396626ed1e9ae537fb702d686cb
**Pass:** fresh logic review (post-merge, separate from PHASE-1-AUDIT.md)

## Executive summary
Found 5 BUGs, 7 IMPROVEMENTs, and 3 NOTEs. Highest priority: stop closing cached Room databases from `QuizViewModel`; the current lifecycle can return a closed cached database on the next quiz. The other user-visible risks are gamification lost updates, spaced-repetition interval off-by-one, immediate smart-selector repeats, and daily-challenge midnight/click-state edge cases.

## Files reviewed
1. `app/src/main/java/com/znam/app/GamificationManager.kt` — 1 BUG, 2 IMPROVEMENTs.
2. `app/src/main/java/com/znam/app/data/GamificationEntities.kt` — 1 NOTE.
3. `app/src/main/java/com/znam/app/data/GamificationDao.kt` — 1 BUG via caller atomicity, 1 IMPROVEMENT.
4. `app/src/main/java/com/znam/app/SmartQuestionSelector.kt` — 2 BUGs, 1 IMPROVEMENT.
5. `app/src/main/java/com/znam/app/data/QuestionPerformance.kt` — 1 BUG.
6. `app/src/main/java/com/znam/app/data/QuestionPerformanceDao.kt` — reviewed; no direct finding beyond selector integration.
7. `app/src/main/java/com/znam/app/DailyChallengeManager.kt` — 1 BUG, 2 IMPROVEMENTs.
8. `app/src/main/java/com/znam/app/ui/GamificationComponents.kt` — 1 IMPROVEMENT.
9. `app/src/main/java/com/znam/app/data/StatsDatabase.kt` — 1 IMPROVEMENT.
10. `app/src/main/java/com/znam/app/DatabaseProvider.kt` — 1 BUG.
11. `app/src/main/java/com/znam/app/QuizViewModel.kt` — 2 BUGs, 2 IMPROVEMENTs.
12. `app/src/main/java/com/znam/app/ComposeQuizActivity.kt` — reviewed; no direct finding.
13. `app/src/main/java/com/znam/app/ResultActivity.kt` — reviewed; no direct finding.
14. `app/src/main/java/com/znam/app/WelcomeActivity.kt` — 1 BUG, 1 IMPROVEMENT.
15. `app/src/main/java/com/znam/app/ui/StatsScreen.kt` — 1 IMPROVEMENT, 1 NOTE.
16. `app/src/main/java/com/znam/app/AppModule.kt` — reviewed; wiring is coherent.
17. `app/src/main/java/com/znam/app/ui/QuizScreen.kt` — 1 NOTE.
18. `app/src/main/java/com/znam/app/StatsViewModel.kt` — 1 IMPROVEMENT.

Also reviewed `app/src/test/java/com/znam/app/StatsDatabaseMigrationTest.kt` — 1 IMPROVEMENT.

## Bugs

### BUG-1: Cached quiz database can be closed and then reused
- **File:** `app/src/main/java/com/znam/app/DatabaseProvider.kt:26`, `app/src/main/java/com/znam/app/QuizViewModel.kt:476`
- **Problem:** `DatabaseProvider` caches `AppDatabase` instances, but each `QuizViewModel` closes its cached instance in `onCleared()`.
- **Scenario:** User finishes a class 9 quiz. `ComposeQuizActivity.finish()` clears the VM and closes `class9.db`. User starts another class 9 quiz. `DatabaseProvider.createDatabase("class9.db")` returns the same closed instance from `appDatabases`; Room queries can fail with a closed connection pool.
- **Evidence:**
```kotlin
// DatabaseProvider.kt
private val appDatabases = ConcurrentHashMap<String, AppDatabase>()
return appDatabases.getOrPut(dbName) { ... .build() }

// QuizViewModel.kt
override fun onCleared() {
    ...
    db?.close()
}
```
- **Suggested fix:** Either do not cache `AppDatabase` instances, or make `DatabaseProvider` own close/eviction. ViewModels should not close singleton/cached databases they did not create.
- **Severity:** User-visible crash or no-questions fallback after normal repeated quiz use.

### BUG-2: Gamification read-modify-write is not atomic
- **File:** `app/src/main/java/com/znam/app/GamificationManager.kt:90`, `app/src/main/java/com/znam/app/data/GamificationDao.kt:24`
- **Problem:** `processQuizCompletion()` reads `UserProfile`, computes many counters, then writes the whole row back with no Room transaction, mutex, or SQL increments.
- **Scenario:** Two completions land close together (double finish path, restored activity, background/foreground overlap). Both read `totalXp=100`, both compute from the same `currentStreak` and `totalQuizzesCompleted`, then the later update overwrites the earlier one. XP, quiz count, perfect count, daily challenge status, and achievements can be lost or duplicated in UI.
- **Evidence:**
```kotlin
val profile = gamificationDao.ensureProfile()
...
val baseUpdatedProfile = profile.copy(
    totalXp = newTotalXp,
    ...
    totalQuizzesCompleted = profile.totalQuizzesCompleted + 1
)
gamificationDao.updateProfile(updatedProfile)
```
- **Suggested fix:** Add a DAO-level `@Transaction` method or manager `Mutex` around profile update + achievement unlocks. Prefer SQL `UPDATE ... SET totalXp = totalXp + :delta` for counters where practical.
- **Severity:** Data corruption under plausible lifecycle/concurrency races.

### BUG-3: Spaced repetition interval is one answer behind after the first review
- **File:** `app/src/main/java/com/znam/app/SmartQuestionSelector.kt:123`, `app/src/main/java/com/znam/app/data/QuestionPerformance.kt:35`
- **Problem:** Existing performance rows compute `nextReviewAt` using `existing.computeNextInterval(wasCorrect)`, which reads the old `consecutiveCorrect`/`consecutiveWrong` values, not the newly computed streak.
- **Scenario:** First correct answer creates `consecutiveCorrect=1`, next review in 1 day. On the second consecutive correct answer, the code sets `newConsecutiveCorrect=2` but computes interval from old value `1`, so it schedules 1 day again instead of 3 days. All later correct intervals lag one step: 1d, 1d, 3d, 7d, 14d, 30d, then 60d.
- **Evidence:**
```kotlin
val newConsecutiveCorrect = if (wasCorrect) existing.consecutiveCorrect + 1 else 0
...
nextReviewAt = now + existing.computeNextInterval(wasCorrect)

// QuestionPerformance.kt
when (consecutiveCorrect) {
    1 -> baseIntervalMs
    2 -> baseIntervalMs * 3
```
- **Suggested fix:** Compute the interval from a temporary updated object or change `computeNextInterval(wasCorrect, consecutiveCorrectAfterAnswer)` to use post-answer counts.
- **Severity:** User-visible learning cadence bug; hard questions/easy questions are scheduled at the wrong times.

### BUG-4: Smart selector can immediately repeat just-answered questions
- **File:** `app/src/main/java/com/znam/app/SmartQuestionSelector.kt:42`, `app/src/main/java/com/znam/app/SmartQuestionSelector.kt:97`, `app/src/main/java/com/znam/app/QuizViewModel.kt:202`
- **Problem:** When smart selection is enabled, `QuizViewModel` bypasses the old `answeredQuestionIds` filter. The selector's final random fill is `allQuestions.filter { it.id !in usedIds }`, not “unseen or due” questions.
- **Scenario:** User completes first 15-question quiz. Performance rows now exist for those 15 with future `nextReviewAt`. User starts a second quiz immediately. Due count is empty, weak count is empty (`timesAnswered >= 2`), unseen contributes only 3 questions, and random fill can pull the 15 just-answered questions even though they are not due.
- **Evidence:**
```kotlin
// QuizViewModel.kt: smart path ignores answeredQuestionIds
if (smartSelector != null && allQuestions.isNotEmpty()) {
    smartSelector.selectQuestions(...)
}

// SmartQuestionSelector.kt
val pool = allQuestions.filter { it.id !in usedIds }.shuffled()
```
- **Suggested fix:** Pass recently answered IDs / performance rows into the selector and make random fill prefer unseen + due + stale questions before not-due recent questions. Keep a fallback only if the pool is otherwise exhausted.
- **Severity:** Observable UX regression: back-to-back quizzes can feel repetitive and violate the old no-repeat behavior.

### BUG-5: Completed daily challenge still launches as active challenge
- **File:** `app/src/main/java/com/znam/app/WelcomeActivity.kt:103`, `app/src/main/java/com/znam/app/WelcomeActivity.kt:116`
- **Problem:** The button text/alpha changes when completed, but the click listener still launches `ComposeQuizActivity` with `IS_DAILY_CHALLENGE=true`.
- **Scenario:** User completes today's challenge, returns home, sees the “done” styling, taps the same button again, and is taken into the daily challenge flow. `shouldAwardDailyChallenge()` suppresses bonus XP, but the UX state says completed while behavior remains active.
- **Evidence:**
```kotlin
if (completed) {
    button.text = getString(R.string.daily_challenge_done_format, challengeName)
    button.alpha = 0.7f
}
...
button.setOnClickListener {
    putExtra("IS_DAILY_CHALLENGE", true)
}
```
- **Suggested fix:** Disable the button when completed, or change the click to launch a normal quiz with clear copy (“practice again, no bonus”). Also re-check completion inside the click handler in case state changes while the screen is open.
- **Severity:** User-visible state transition bug; not data-corrupting, but misleading.

## Improvements

### IMP-1: Inject a `Clock`/date provider for all day logic
- **File:** `app/src/main/java/com/znam/app/GamificationManager.kt:91`, `app/src/main/java/com/znam/app/DailyChallengeManager.kt:28`
- **Current:** Multiple calls to `LocalDate.now()` decide streaks, challenge type, completion checks, and marking completion.
- **Proposed:** Inject `Clock` or a small `DateProvider`, compute `today` and `challengeType` once per flow, and pass them through.
- **Why:** Avoid midnight rollover inconsistencies and make DST/timezone/travel cases unit-testable. Example: `shouldAwardDailyChallenge()` can evaluate one date, then `markDailyChallengeCompleted()` can stamp another if midnight rolls during completion.

### IMP-2: Fix daily challenge rotation indexing
- **File:** `app/src/main/java/com/znam/app/DailyChallengeManager.kt:27`
- **Current:** `QUIZ_TYPES[dayOfYear % QUIZ_TYPES.size]`; Jan 1 (`dayOfYear == 1`) selects index 1, not index 0.
- **Proposed:** Use `(dayOfYear - 1) % QUIZ_TYPES.size`, or base the rotation on epoch day if continuity across years matters.
- **Why:** The current math is deterministic, but off by one for a natural “first day maps to first item” rotation and resets by year/leap-year rather than continuing smoothly.

### IMP-3: Cap or rebalance streak XP bonus
- **File:** `app/src/main/java/com/znam/app/GamificationManager.kt:130`
- **Current:** `streakBonus = newStreak * 5` grows without limit and is awarded on every same-day quiz too because `newStreak` remains positive.
- **Proposed:** Cap the bonus, award it once per day, or make it a daily multiplier rather than a per-quiz additive.
- **Why:** A long streak can dominate score XP, and same-day grinding can repeatedly monetize the streak value.

### IMP-4: Persist achievement unlocks in the same unit of work as profile updates
- **File:** `app/src/main/java/com/znam/app/GamificationManager.kt:155`, `app/src/main/java/com/znam/app/GamificationManager.kt:200`
- **Current:** Profile update is committed before achievements are read/inserted.
- **Proposed:** Put profile update and unlock inserts in one transaction, or record pending unlocks in the profile update transaction.
- **Why:** If the app/process dies after profile update and before unlock inserts, the user can cross a threshold without getting the corresponding achievement until a later quiz happens to re-check it.

### IMP-5: Migration tests should validate real upgrade paths and all new columns
- **File:** `app/src/test/java/com/znam/app/StatsDatabaseMigrationTest.kt:89`
- **Current:** `migration1To3_runsBothMigrationPaths()` only asserts migration version numbers. Tests inspect a few defaults but do not validate v1→v4 schema, v2→v4, or v3→v4 content preservation.
- **Proposed:** Use `MigrationTestHelper.runMigrationsAndValidate()` for v1→4, v2→4, and v3→4. Assert `lastDailyChallengeDateEpochDay` and `dailyChallengeQuizType` defaults too.
- **Why:** The tests currently miss the exact chain a production user will run from older versions to current version 4.

### IMP-6: Stats dashboard should not create a profile just by viewing stats
- **File:** `app/src/main/java/com/znam/app/StatsViewModel.kt:67`
- **Current:** `StatsViewModel` calls `gamificationDao?.ensureProfile()` while loading stats.
- **Proposed:** Use `getProfile()` for read-only screens; reserve `ensureProfile()` for quiz completion or explicit onboarding.
- **Why:** Opening stats mutates persistent gamification state. This can make “no profile yet” UI impossible to distinguish from “profile exists with zero progress.”

### IMP-7: Reward summary lacks XP breakdown despite calculating multiple bonus sources
- **File:** `app/src/main/java/com/znam/app/ui/GamificationComponents.kt:253`
- **Current:** Result UI only shows total XP earned. The manager adds base XP, perfect, no-hints, speed, daily, and streak bonuses but does not expose them separately.
- **Proposed:** Extend `GamificationResult` with component fields or a list of reward lines.
- **Why:** Users cannot tell why they got a reward, and future tuning/debugging is harder.

## Notes

### N-1: Level math is internally consistent at boundaries
- **File / context:** `app/src/main/java/com/znam/app/GamificationManager.kt:37`
- **Observation:** `xpForLevel(level) = 100 * level * (level - 1) / 2` and the loop in `levelFromXp()` agree at tested boundaries: level 2 starts at 100, 3 at 300, 4 at 600, 5 at 1000. The KDoc line says “Formula: 100 * level * (level + 1) / 2” but the examples and implementation use `level - 1`.

### N-2: Achievement definitions are hardcoded but acceptable for phase 1
- **File / context:** `app/src/main/java/com/znam/app/data/GamificationEntities.kt:38`
- **Observation:** 17 constants/maps are fine now. If this grows to 50+ achievements, move definitions to a single typed registry or asset-backed catalog to avoid keeping IDs, strings, icons, and descriptions in sync manually.

### N-3: Perfect-score konfetti path does not trigger for the current event type
- **File / context:** `app/src/main/java/com/znam/app/ui/QuizScreen.kt:125`
- **Observation:** The konfetti effect checks only `QuizEvent.NavigateToResults`, while `finishQuiz()` emits `ShowInterstitialAd`. This is mostly harmless because navigation is immediate/ad-mediated and the result screen has the durable reward UI, but if quiz-screen celebration is intended, key it off both result events or move celebration to results.

## What's well-designed
- Gamification and daily-challenge logic is mostly separated from UI and wired through DI, which keeps screens thin.
- Result navigation now waits for gamification processing before emitting the result event, so reward extras are available to `ResultActivity`.
- `SmartQuestionSelector` degrades gracefully on empty/no-history data and avoids duplicates within a single selected session.
- Room schema defaults for new gamification/performance columns are explicit, and migration tests at least cover default drift partially.
- UI state in `QuizViewModel` is mostly immutable `StateFlow`, which reduces Compose recomposition footguns.

## Test coverage gaps
- Unit tests for `GamificationManager.processQuizCompletion()` covering first quiz, same-day second quiz, yesterday streak extension, missed-day reset, zero-question quiz, daily challenge bonus once per day, achievement threshold crossing, and concurrent completions.
- Unit tests for `GamificationManager.levelFromXp()` and `levelProgress()` at exact boundaries: 0, 99, 100, 299, 300, 999, 1000.
- Unit tests for `QuestionPerformance.computeNextInterval()` through consecutive correct counts 1/2/3/4/5/6 and wrong-answer reset; include the existing-row off-by-one scenario.
- Unit tests for `SmartQuestionSelector.selectQuestions()` with empty history, all history not due, all questions answered correctly, too-small question pools, and immediate second quiz after first quiz.
- Unit tests for `DailyChallengeManager` with fixed clock: Jan 1, Dec 31, leap day, midnight rollover between award check and mark-completed, and completed button state.
- Instrumented/DAO tests using `runMigrationsAndValidate()` for v1→4, v2→4, v3→4, plus content preservation of existing `quiz_sessions` rows.
- UI/state tests for `WelcomeActivity` daily challenge completed/reopen path and `WelcomeGamificationBar` refresh after returning from a quiz.

## Recommended next pass
Fix BUG-1 first because it can break normal repeated quiz usage. Then fix BUG-2/IMP-4 together by making gamification updates transactional. Next fix BUG-3 and BUG-4 because they directly affect the learning algorithm. Finally clean up daily-challenge date/button behavior with an injected clock and completed-state click handling, then expand migration and manager/selector tests around those changes.

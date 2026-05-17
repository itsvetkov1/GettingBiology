# GettingBiology phase-1 Audit
**Date:** 2026-05-13
**Auditor:** dev (hermes)
**Branch:** modernization/phase-1 @ 19bebc0
**Base:** master @ a0d58f6

## Environment: HYDRA-NATIVE
- Repo: `/home/alpharius/projects/GettingBiology`
- HEAD: `19bebc02636bfd0613377343e8be2a7d3ec13b79` (`19bebc0`)
- Branch: `modernization/phase-1`
- Git status at start: clean; status after audit: clean except the allowed audit report after writing.
- Gradle: 8.13; JVM: 17.0.18; OS: Linux 6.17.0-23-generic amd64.

## Verification summary
| Check | Result | Evidence |
|---|---|---|
| Phase 0 environment detection | PASS | `git rev-parse HEAD` returned `19bebc02636bfd0613377343e8be2a7d3ec13b79`; branch `modernization/phase-1`; `./gradlew --version` succeeded. |
| `./gradlew clean assembleDebug` | PASS with warnings | `/tmp/phase1-build.log`: `BUILD SUCCESSFUL in 1m 38s`; APK exists at `app/build/outputs/apk/debug/app-debug.apk`; captured 8 Kotlin deprecation warnings plus debug-minify/native-strip warnings. |
| No new warnings vs master baseline | PARTIAL | Current warnings captured. I did not checkout master or create a master worktree because the task explicitly blocks `git checkout master` and this was a read-only audit. Treat new-warning comparison as requiring a separate baseline run if the operator permits it. |
| `./gradlew lint` | PASS with warnings | `/tmp/phase1-lint.log`: `BUILD SUCCESSFUL`; `app/build/reports/lint-results-debug.txt`: `0 errors, 97 warnings`. |
| `./gradlew test` | PASS | `/tmp/phase1-test.log`: `BUILD SUCCESSFUL`; parsed test XML: `34 passed / 0 failed (34 total)`. Prior Koin failures are fixed relative to `AUDIT-REPORT.md` pre-phase-1. |
| APK install verification | NOT RUN | Debug APK build is sufficient per task; no emulator/device install was required. No real-device action attempted. |
| Diff-by-file review | PASS | Reviewed 27 changed files across 6 clusters; artifact `/tmp/phase1-diff-review.json`. |
| 12 audit dimensions | PASS | All 12 checklist dimensions recorded in `/tmp/phase1-dimensions.json`. |
| Triage report and resolution JSON | PASS | This report and `/opt/alpharius/tasks/getting-biology/TASK-audit-phase1.md.resolved.json`. |

## Scope and reviewed clusters
- Build/dependencies/R8: 3 files — app/build.gradle, app/proguard-rules.pro, build.gradle
- DI/database/migrations: 7 files — app/src/main/java/com/znam/app/AppModule.kt, app/src/main/java/com/znam/app/DatabaseProvider.kt, app/src/main/java/com/znam/app/data/GamificationDao.kt, app/src/main/java/com/znam/app/data/GamificationEntities.kt, app/src/main/java/com/znam/app/data/QuestionPerformance.kt, app/src/main/java/com/znam/app/data/QuestionPerformanceDao.kt, app/src/main/java/com/znam/app/data/StatsDatabase.kt
- Gamification/domain managers: 3 files — app/src/main/java/com/znam/app/DailyChallengeManager.kt, app/src/main/java/com/znam/app/GamificationManager.kt, app/src/main/java/com/znam/app/SmartQuestionSelector.kt
- Quiz flow/ViewModels: 3 files — app/src/main/java/com/znam/app/ComposeQuizActivity.kt, app/src/main/java/com/znam/app/QuizViewModel.kt, app/src/main/java/com/znam/app/StatsViewModel.kt
- Compose UI/theme: 4 files — app/src/main/java/com/znam/app/ui/GamificationComponents.kt, app/src/main/java/com/znam/app/ui/QuizScreen.kt, app/src/main/java/com/znam/app/ui/StatsScreen.kt, app/src/main/java/com/znam/app/ui/theme/ZnamTheme.kt
- Legacy activities/layouts/resources: 7 files — app/src/main/java/com/znam/app/ResultActivity.kt, app/src/main/java/com/znam/app/WelcomeActivity.kt, app/src/main/res/layout/activity_result.xml, app/src/main/res/layout/activity_welcome.xml, app/src/main/res/raw/correct_check.json, app/src/main/res/raw/incorrect_x.json, app/src/main/res/raw/loading_dna.json

## Must-fix (blocks merge)
### MF-1: Result screen usually loses gamification rewards due to async navigation race
- **File:** app/src/main/java/com/znam/app/QuizViewModel.kt:436
- **Phase:** Phase 2 integration
- **Problem:** Quiz completion launches gamification processing in a background coroutine, then immediately emits the navigation/ad event. ComposeQuizActivity reads gamificationResult synchronously, so ResultActivity commonly receives no XP/level/achievement extras and hides the new reward UI.
- **Evidence:**
```
436:         viewModelScope.launch(Dispatchers.IO) {
437:             try {
438:                 statsDao?.insertSession(
439:                     QuizSession(
440:                         quizType = state.quizType,
441:                         score = state.score,
442:                         totalQuestions = state.totalQuestions,
443:                         elapsedTimeSeconds = state.elapsedSeconds,
444:                         hintsUsed = totalHintsUsed,
445:                         timestamp = System.currentTimeMillis()
446:                     )
447:                 )
448:             } catch (e: Exception) {
449:                 Log.w("QuizViewModel", "Stats persistence failed (best-effort)", e)
450:             }
451: 
452:             // Gamification: award XP, update streak, check achievements
453:             try {
454:                 val result = gamificationManager?.processQuizCompletion(
455:                     score = state.score,
456:                     totalQuestions = state.totalQuestions,
457:                     elapsedTimeSeconds = state.elapsedSeconds,
458:                     hintsUsed = totalHintsUsed
459:                 )
460:                 if (result != null) {
461:                     _gamificationResult.value = result
462:                 }
463:             } catch (e: Exception) {
464:                 Log.w("QuizViewModel", "Gamification processing failed (best-effort)", e)
465:             }
466:         }
467: 
468:         // Emit event  the UI layer decides whether to show an ad first
469:         _events.value = QuizEvent.ShowInterstitialAd(results)
118:             userAnswers = ArrayList(results.userAnswers),
119:             elapsedTimeInSeconds = results.elapsedTimeSeconds
120:         )
121: 
122:         val gamResult = quizViewModel.gamificationResult.value
123: 
124:         val intent = Intent(this, ResultActivity::class.java).apply {
125:             putExtra(ResultActivity.EXTRA_QUIZ_RESULT, quizResult)
126:             // Pass gamification data
127:             if (gamResult != null) {
128:                 putExtra(ResultActivity.EXTRA_XP_EARNED, gamResult.xpEarned)
129:                 putExtra(ResultActivity.EXTRA_NEW_TOTAL_XP, gamResult.newTotalXp)
130:                 putExtra(ResultActivity.EXTRA_OLD_LEVEL, gamResult.oldLevel)
131:                 putExtra(ResultActivity.EXTRA_NEW_LEVEL, gamResult.newLevel)
132:                 putExtra(ResultActivity.EXTRA_LEVELED_UP, gamResult.leveledUp)
133:                 putExtra(ResultActivity.EXTRA_CURRENT_STREAK, gamResult.currentStreak)
134:                 putExtra(ResultActivity.EXTRA_NEW_ACHIEVEMENTS, gamResult.newAchievements.toTypedArray())
96:         val xpEarned = intent.getIntExtra(EXTRA_XP_EARNED, -1)
97: 
98:         if (xpEarned < 0) {
99:             // No gamification data — hide the view
100:             composeView.visibility = View.GONE
101:             return
102:         }
103: 
```
- **Suggested fix:** Make reward processing part of the completion path before emitting ShowInterstitialAd, or include the reward result in the QuizEvent/result payload after awaiting the IO work.
- **Severity rationale:** Broken primary UX path for a shipped phase-2 feature: rewards may be persisted later but the celebratory result surface is missing.

### MF-2: Daily challenge completion is tracked as any quiz completed today
- **File:** app/src/main/java/com/znam/app/DailyChallengeManager.kt:45
- **Phase:** Phase 5 daily challenge
- **Problem:** isDailyChallengeCompleted checks only lastQuizDateEpochDay and quizzesCompletedToday. It does not verify the IS_DAILY_CHALLENGE launch path or challenge type, and the daily flag passed by WelcomeActivity is not read by ComposeQuizActivity.
- **Evidence:**
```
16: ) {
17:     companion object {
18:         const val DAILY_CHALLENGE_XP_BONUS = 50
19:         private val QUIZ_TYPES = listOf("class9.db", "class10.db", "db_entrance_exam.db")
20:         private val CHALLENGE_NAMES = mapOf(
21:             "class9.db" to "9th Grade Challenge",
22:             "class10.db" to "10th Grade Challenge",
23:             "db_entrance_exam.db" to "Entrance Exam Challenge"
24:         )
25:     }
26: 
27:     /**
28:      * Get today's challenge quiz type (deterministic based on date).
29:      */
30:     fun getTodaysChallengeType(): String {
31:         val dayOfYear = LocalDate.now().dayOfYear
32:         return QUIZ_TYPES[dayOfYear % QUIZ_TYPES.size]
33:     }
34: 
35:     /**
36:      * Get the display name for today's challenge.
37:      */
38:     fun getTodaysChallengeName(): String {
39:         return CHALLENGE_NAMES[getTodaysChallengeType()] ?: "Daily Challenge"
40:     }
41: 
42:     /**
43:      * Check if the user has completed today's challenge.
44:      */
45:     suspend fun isTodaysChallengeCompleted(): Boolean {
46:         val profile = gamificationDao.getProfile() ?: return false
47:         val today = LocalDate.now().toEpochDay()
48:         // If last quiz was today and they've done at least one quiz today
49:         return profile.lastQuizDateEpochDay == today && profile.quizzesCompletedToday > 0
99:         val challengeName = dailyChallengeManager.getTodaysChallengeName()
100:         val challengeType = dailyChallengeManager.getTodaysChallengeType()
101: 
102:         // Check if challenge is completed
103:         lifecycleScope.launch {
104:             val completed = withContext(Dispatchers.IO) {
105:                 dailyChallengeManager.isTodaysChallengeCompleted()
106:             }
107:             if (completed) {
108:                 button.text = "✅ $challengeName (Done!)"
109:                 button.alpha = 0.7f
110:             } else {
111:                 button.text = "⚡ $challengeName"
112:             }
113:         }
114: 
115:         button.setOnClickListener {
116:             val intent = Intent(this, ComposeQuizActivity::class.java).apply {
117:                 putExtra("QUIZ_TYPE", challengeType)
118:                 putExtra("IS_DAILY_CHALLENGE", true)
119:             }
120:             startActivity(intent)
38:         super.onCreate(savedInstanceState)
39: 
40:         val quizType = intent.getStringExtra("QUIZ_TYPE")
41:             ?: getSharedPreferences("QuizPrefs", MODE_PRIVATE)
42:                 .getString("LAST_QUIZ_TYPE", "default")
43:             ?: "default"
44: 
45:         // Preload interstitial ad
46:         loadInterstitialAd()
47: 
48:         // Initialize the ViewModel with the quiz type
49:         quizViewModel.initialize(quizType)
```
- **Suggested fix:** Persist explicit daily challenge state, e.g. lastDailyChallengeDateEpochDay and challenge type/key, and update it only when IS_DAILY_CHALLENGE=true and the completed quiz matches today’s challenge.
- **Severity rationale:** Blocks merge because a headline engagement feature can show Done after an unrelated quiz and cannot prove the daily challenge was completed.

### MF-3: Room migrations likely fail validation on existing installs because SQL defaults do not match entity defaults
- **File:** app/src/main/java/com/znam/app/data/StatsDatabase.kt:29
- **Phase:** Phases 2-3 migrations
- **Problem:** Migrations create v2/v3 tables with SQL DEFAULT clauses, but entity columns use Kotlin defaults without @ColumnInfo(defaultValue). Room validates schema defaults during migration and can reject mismatches on upgrade.
- **Evidence:**
```
24:         val MIGRATION_1_2 = object : Migration(1, 2) {
25:             override fun migrate(db: SupportSQLiteDatabase) {
26:                 db.execSQL("""
27:                     CREATE TABLE IF NOT EXISTS `user_profile` (
28:                         `userId` INTEGER NOT NULL PRIMARY KEY,
29:                         `totalXp` INTEGER NOT NULL DEFAULT 0,
30:                         `level` INTEGER NOT NULL DEFAULT 1,
31:                         `currentStreak` INTEGER NOT NULL DEFAULT 0,
32:                         `longestStreak` INTEGER NOT NULL DEFAULT 0,
33:                         `lastQuizDateEpochDay` INTEGER NOT NULL DEFAULT 0,
34:                         `quizzesCompletedToday` INTEGER NOT NULL DEFAULT 0,
35:                         `perfectScoreCount` INTEGER NOT NULL DEFAULT 0,
36:                         `totalQuizzesCompleted` INTEGER NOT NULL DEFAULT 0
37:                     )
38:                 """.trimIndent())
39: 
40:                 db.execSQL("""
41:                     CREATE TABLE IF NOT EXISTS `achievements` (
42:                         `achievementId` TEXT NOT NULL PRIMARY KEY,
43:                         `unlockedAt` INTEGER NOT NULL DEFAULT 0
44:                     )
45:                 """.trimIndent())
46:             }
47:         }
48: 
49:         val MIGRATION_2_3 = object : Migration(2, 3) {
50:             override fun migrate(db: SupportSQLiteDatabase) {
51:                 db.execSQL("""
52:                     CREATE TABLE IF NOT EXISTS `question_performance` (
53:                         `quizType` TEXT NOT NULL,
54:                         `questionId` INTEGER NOT NULL,
55:                         `timesAnswered` INTEGER NOT NULL DEFAULT 0,
56:                         `timesCorrect` INTEGER NOT NULL DEFAULT 0,
57:                         `consecutiveCorrect` INTEGER NOT NULL DEFAULT 0,
58:                         `consecutiveWrong` INTEGER NOT NULL DEFAULT 0,
59:                         `lastAnsweredAt` INTEGER NOT NULL DEFAULT 0,
60:                         `nextReviewAt` INTEGER NOT NULL DEFAULT 0,
61:                         `difficultyScore` REAL NOT NULL DEFAULT 0.5,
62:                         PRIMARY KEY(`quizType`, `questionId`)
8:  * Single-row table (userId always = 1).
9:  */
10: @Entity(tableName = "user_profile")
11: data class UserProfile(
12:     @PrimaryKey val userId: Int = 1,
13:     val totalXp: Int = 0,
14:     val level: Int = 1,
15:     val currentStreak: Int = 0,
16:     val longestStreak: Int = 0,
17:     val lastQuizDateEpochDay: Long = 0L,  // LocalDate.toEpochDay()
18:     val quizzesCompletedToday: Int = 0,
19:     val perfectScoreCount: Int = 0,
20:     val totalQuizzesCompleted: Int = 0
21: )
22: 
23: /**
24:  * Records an unlocked achievement.
10:     tableName = "question_performance",
11:     primaryKeys = ["quizType", "questionId"]
12: )
13: data class QuestionPerformance(
14:     val quizType: String,
15:     val questionId: Int,
16:     val timesAnswered: Int = 0,
17:     val timesCorrect: Int = 0,
18:     val consecutiveCorrect: Int = 0,
19:     val consecutiveWrong: Int = 0,
20:     val lastAnsweredAt: Long = 0L,
21:     val nextReviewAt: Long = 0L,      // spaced repetition: when to show again
22:     val difficultyScore: Float = 0.5f  // 0.0 = easy, 1.0 = hard (for this user)
23: ) {
24:     val accuracyRate: Float
```
- **Suggested fix:** Either remove SQL DEFAULT clauses from migration CREATE TABLE statements or add matching @ColumnInfo(defaultValue=...) annotations. Add migration tests for 1->2, 2->3, and 1->3 before merge.
- **Severity rationale:** Potential upgrade-time database-open crash/data-access failure for existing users, which is a merge blocker even though fresh debug builds pass.

## Should-fix (degrades quality)
### SF-1: Stale streak is used for XP before rollover/reset
- **File:** app/src/main/java/com/znam/app/GamificationManager.kt:98
- **Phase:** Phase 2 gamification
- **Problem:** XP streak bonus is calculated from profile.currentStreak before the code decides whether the streak continued, reset, or already counted today.
- **Evidence:**
```
88:         val today = LocalDate.now().toEpochDay()
89:         val isPerfect = score == totalQuestions && totalQuestions > 0
90:         val isSpeedRun = elapsedTimeSeconds < XP_SPEED_BONUS_THRESHOLD_SECONDS && totalQuestions > 0
91: 
92:         // --- Calculate XP ---
93:         var xpEarned = score * XP_PER_CORRECT
94:         if (isPerfect) xpEarned += XP_PERFECT_BONUS
95:         if (hintsUsed == 0 && isPerfect) xpEarned += XP_NO_HINTS_BONUS
96:         if (isSpeedRun) xpEarned += XP_SPEED_BONUS
97: 
98:         // Streak multiplier
99:         val streakBonus = profile.currentStreak * XP_STREAK_MULTIPLIER_BASE
100:         xpEarned += streakBonus
101: 
102:         val newTotalXp = profile.totalXp + xpEarned
103:         val oldLevel = profile.level
104:         val newLevel = levelFromXp(newTotalXp)
105: 
106:         // --- Update streak ---
107:         val yesterday = today - 1
108:         val newStreak: Int
109:         val quizzesToday: Int
110: 
111:         when (profile.lastQuizDateEpochDay) {
112:             today -> {
113:                 // Same day — streak unchanged, increment daily count
114:                 newStreak = profile.currentStreak
115:                 quizzesToday = profile.quizzesCompletedToday + 1
116:             }
117:             yesterday -> {
118:                 // Consecutive day — extend streak
119:                 newStreak = profile.currentStreak + 1
120:                 quizzesToday = 1
121:             }
122:             else -> {
123:                 // Gap — reset streak (but this quiz starts a new one)
124:                 newStreak = 1
125:                 quizzesToday = 1
126:             }
```
- **Suggested fix:** Compute the post-rollover streak first and base the bonus on that value, or award no streak bonus when the last quiz day is not today/yesterday.
- **Severity rationale:** Meaningful logic bug that over-awards XP after missed days.

### SF-2: Daily challenge XP bonus constant is unused
- **File:** app/src/main/java/com/znam/app/DailyChallengeManager.kt:18
- **Phase:** Phase 5 daily challenge
- **Problem:** DAILY_CHALLENGE_XP_BONUS is declared but processQuizCompletion accepts no daily-challenge parameter and no call site awards the bonus.
- **Evidence:**
```
16: ) {
17:     companion object {
18:         const val DAILY_CHALLENGE_XP_BONUS = 50
19:         private val QUIZ_TYPES = listOf("class9.db", "class10.db", "db_entrance_exam.db")
20:         private val CHALLENGE_NAMES = mapOf(
21:             "class9.db" to "9th Grade Challenge",
22:             "class10.db" to "10th Grade Challenge",
23:             "db_entrance_exam.db" to "Entrance Exam Challenge"
24:         )
25:     }
26: 
27:     /**
28:      * Get today's challenge quiz type (deterministic based on date).
29:      */
30:     fun getTodaysChallengeType(): String {
31:         val dayOfYear = LocalDate.now().dayOfYear
32:         return QUIZ_TYPES[dayOfYear % QUIZ_TYPES.size]
33:     }
34: 
35:     /**
36:      * Get the display name for today's challenge.
37:      */
38:     fun getTodaysChallengeName(): String {
39:         return CHALLENGE_NAMES[getTodaysChallengeType()] ?: "Daily Challenge"
40:     }
41: 
42:     /**
43:      * Check if the user has completed today's challenge.
44:      */
45:     suspend fun isTodaysChallengeCompleted(): Boolean {
46:         val profile = gamificationDao.getProfile() ?: return false
47:         val today = LocalDate.now().toEpochDay()
48:         // If last quiz was today and they've done at least one quiz today
49:         return profile.lastQuizDateEpochDay == today && profile.quizzesCompletedToday > 0
38:         super.onCreate(savedInstanceState)
39: 
40:         val quizType = intent.getStringExtra("QUIZ_TYPE")
41:             ?: getSharedPreferences("QuizPrefs", MODE_PRIVATE)
42:                 .getString("LAST_QUIZ_TYPE", "default")
43:             ?: "default"
44: 
45:         // Preload interstitial ad
46:         loadInterstitialAd()
47: 
48:         // Initialize the ViewModel with the quiz type
49:         quizViewModel.initialize(quizType)
```
- **Suggested fix:** Thread isDailyChallenge through activity/viewmodel/manager and award the bonus once with the explicit daily completion record.
- **Severity rationale:** Feature promise is not fulfilled; users get no special daily reward.

### SF-3: First correct answer schedules review too far out
- **File:** app/src/main/java/com/znam/app/SmartQuestionSelector.kt:147
- **Phase:** Phase 3 smart learning
- **Problem:** New performance rows set consecutiveCorrect=1 and then computeNextInterval adds +1 again, so the first correct answer gets the second-correct interval.
- **Evidence:**
```
143:                 difficultyScore = newDifficulty.coerceIn(0f, 1f)
144:             )
145:         } else {
146:             // First time seeing this question
147:             val perf = QuestionPerformance(
148:                 quizType = quizType,
149:                 questionId = questionId,
150:                 timesAnswered = 1,
151:                 timesCorrect = if (wasCorrect) 1 else 0,
152:                 consecutiveCorrect = if (wasCorrect) 1 else 0,
153:                 consecutiveWrong = if (!wasCorrect) 1 else 0,
154:                 lastAnsweredAt = now,
155:                 difficultyScore = if (wasCorrect) 0.3f else 0.7f
156:             )
157:             perf.copy(nextReviewAt = now + perf.computeNextInterval(wasCorrect))
158:         }
159: 
160:         performanceDao.upsert(updated)
161:     }
31:     fun computeNextInterval(wasCorrect: Boolean): Long {
32:         val baseIntervalMs = 24 * 60 * 60 * 1000L // 1 day in ms
33:         return if (wasCorrect) {
34:             when (consecutiveCorrect + 1) {
35:                 1 -> baseIntervalMs           // 1 day
36:                 2 -> baseIntervalMs * 3       // 3 days
37:                 3 -> baseIntervalMs * 7       // 1 week
38:                 4 -> baseIntervalMs * 14      // 2 weeks
39:                 5 -> baseIntervalMs * 30      // 1 month
40:                 else -> baseIntervalMs * 60   // 2 months max
```
- **Suggested fix:** Compute interval from the pre-answer state, or change computeNextInterval to use already-updated counters without +1.
- **Severity rationale:** Adaptive review quality degrades because new correct questions are delayed beyond intended spacing.

### SF-4: Answer feedback is color-only and inaccessible
- **File:** app/src/main/java/com/znam/app/ui/QuizScreen.kt:426
- **Phase:** Phase 1/4 UI
- **Problem:** Correct/incorrect answer state is communicated by green/red colors without semantic stateDescription or visible non-color text/icon.
- **Evidence:**
```
426:     OutlinedButton(
427:         onClick = onSelected,
428:         enabled = !isAnswered,
429:         modifier = Modifier
430:             .fillMaxWidth()
431:             .scale(scale),
432:         shape = RoundedCornerShape(12.dp),
433:         border = BorderStroke(
434:             width = if (feedback != null && (feedback.selectedOption == index || feedback.correctOption == index)) 2.dp else 1.dp,
435:             color = animatedBorder
436:         ),
437:         colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
438:             containerColor = animatedBg,
439:             disabledContainerColor = animatedBg
440:         )
441:     ) {
442:         Text(
443:             text = text,
444:             modifier = Modifier
445:                 .fillMaxWidth()
446:                 .padding(vertical = 8.dp),
447:             style = MaterialTheme.typography.bodyLarge,
448:             color = textColor,
449:             textAlign = TextAlign.Start
450:         )
451:     }
452: }
453: 
454: private data class OptionColors(
455:     val background: Color,
456:     val border: Color,
457:     val text: Color
458: )
459: 
460: private fun resolveOptionColors(
461:     index: Int,
462:     feedback: AnswerFeedback?,
463:     isAnswered: Boolean
464: ): OptionColors {
465:     if (feedback == null) {
466:         // Default state  not yet answered
467:         return OptionColors(
468:             background = Color.White,
469:             border = DefaultOptionBorder,
470:             text = Color.Black
471:         )
472:     }
473: 
474:     return when {
475:         // This is the correct answer
476:         feedback.correctOption == index -> OptionColors(
477:             background = CorrectGreenBg,
478:             border = CorrectGreen,
479:             text = CorrectGreen
480:         )
481:         // This is the selected but wrong answer
482:         feedback.selectedOption == index && !feedback.isCorrect -> OptionColors(
483:             background = IncorrectRedBg,
484:             border = IncorrectRed,
485:             text = IncorrectRed
486:         )
487:         // Unrelated option after answer
488:         else -> OptionColors(
489:             background = Color.White.copy(alpha = 0.5f),
490:             border = DefaultOptionBorder.copy(alpha = 0.5f),
491:             text = Color.Black.copy(alpha = 0.5f)
```
- **Suggested fix:** Add visible ✓/✗ labels and Compose semantics/stateDescription for selected/correct/incorrect options.
- **Severity rationale:** Accessibility regression for TalkBack and color-blind users.

### SF-5: New gamification and daily challenge UI bypasses BG/EN resources
- **File:** app/src/main/java/com/znam/app/ui/GamificationComponents.kt:70
- **Phase:** Phases 2,4,5 UI
- **Problem:** New user-visible strings are hardcoded English in Kotlin/XML instead of strings.xml / values-en resources.
- **Evidence:**
```
68:         ) {
69:             Text(
70:                 text = "Level $level",
71:                 style = MaterialTheme.typography.titleMedium,
72:                 fontWeight = FontWeight.Bold,
73:                 color = MaterialTheme.colorScheme.primary
74:             )
75:             Text(
76:                 text = "${totalXp - currentLevelXp} / ${nextLevelXp - currentLevelXp} XP",
77:                 style = MaterialTheme.typography.bodySmall,
78:                 color = MaterialTheme.colorScheme.onSurfaceVariant
79:             )
80:         }
81:         Spacer(modifier = Modifier.height(4.dp))
82:         LinearProgressIndicator(
83:             progress = { animatedProgress },
84:             modifier = Modifier
85:                 .fillMaxWidth()
86:                 .height(8.dp)
87:                 .clip(RoundedCornerShape(4.dp)),
88:             color = MaterialTheme.colorScheme.primary,
89:             trackColor = MaterialTheme.colorScheme.surfaceVariant,
90:         )
91:     }
92: }
93: 
94: /**
95:  * Streak badge — shows current streak with fire icon.
96:  */
97: @Composable
98: fun StreakBadge(
99:     currentStreak: Int,
100:     modifier: Modifier = Modifier
101: ) {
102:     if (currentStreak <= 0) return
103: 
104:     Card(
105:         modifier = modifier,
106:         colors = CardDefaults.cardColors(
107:             containerColor = MaterialTheme.colorScheme.tertiaryContainer
108:         ),
109:         shape = RoundedCornerShape(12.dp)
110:     ) {
111:         Row(
112:             modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
113:             verticalAlignment = Alignment.CenterVertically
114:         ) {
115:             Text(
116:                 text = "🔥",  // fire emoji
117:                 fontSize = 20.sp
118:             )
119:             Spacer(modifier = Modifier.width(6.dp))
120:             Column {
121:                 Text(
122:                     text = "$currentStreak day${if (currentStreak != 1) "s" else ""}",
123:                     style = MaterialTheme.typography.titleSmall,
124:                     fontWeight = FontWeight.Bold,
125:                     color = MaterialTheme.colorScheme.onTertiaryContainer
126:                 )
127:                 Text(
128:                     text = "streak",
129:                     style = MaterialTheme.typography.bodySmall,
130:                     color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
100: 
101:                 <com.google.android.material.button.MaterialButton
102:                     android:id="@+id/dailyChallengeButton"
103:                     android:layout_width="match_parent"
104:                     android:layout_height="48dp"
105:                     android:text="⚡ Daily Challenge"
106:                     android:textSize="14sp"
107:                     android:textStyle="bold"
```
- **Suggested fix:** Move strings to resources, add BG and EN translations/plurals, and return challenge keys/resource IDs rather than display names from business logic.
- **Severity rationale:** Localized app now mixes English into Bulgarian flows and cannot pluralize correctly.

### SF-6: Release minification is defeated by blanket app keep rule
- **File:** app/proguard-rules.pro:4
- **Phase:** Phase 4 ProGuard
- **Problem:** -keep class com.znam.app.** { *; } preserves all app classes while release minify/shrink is enabled.
- **Evidence:**
```
1: # GettingBiology ProGuard/R8 Rules
2: 
3: # Keep application classes
4: -keep class com.znam.app.** { *; }
5: 
6: # Room
7: -keep class * extends androidx.room.RoomDatabase
8: -keep @androidx.room.Entity class *
9: -keep @androidx.room.Dao class *
10: -dontwarn androidx.room.paging.**
11: 
12: # Koin
13: -keep class org.koin.** { *; }
14: -dontwarn org.koin.**
15: 
16: # Compose (R8 handles most automatically, but keep these)
17: -dontwarn androidx.compose.**
18: -keep class androidx.compose.runtime.** { *; }
19: 
20: # Parcelable
48: 
49:     buildTypes {
50:         debug {
51:             minifyEnabled = true
52:             shrinkResources = true
53:             proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
54:             manifestPlaceholders = [ADMOB_APPLICATION_ID: debugAdMobApplicationId]
55:             buildConfigField 'String', 'ADMOB_BANNER_AD_UNIT_ID', "\"${debugBannerAdUnitId}\""
56:             buildConfigField 'String', 'ADMOB_INTERSTITIAL_AD_UNIT_ID', "\"${debugInterstitialAdUnitId}\""
57:         }
58: 
59:         release {
60:             minifyEnabled = true
61:             shrinkResources = true
62:             proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
63:             manifestPlaceholders = [ADMOB_APPLICATION_ID: releaseAdMobApplicationId]
```
- **Suggested fix:** Remove the blanket keep and keep only reflectively accessed members/classes; rely on library consumer rules where possible.
- **Severity rationale:** Release shrink/obfuscation quality is materially reduced.

### SF-7: StateFlow collection is not lifecycle-aware
- **File:** app/src/main/java/com/znam/app/ui/QuizScreen.kt:100
- **Phase:** Phase 1 Compose
- **Problem:** QuizScreen and StatsScreen use collectAsState rather than collectAsStateWithLifecycle.
- **Evidence:**
```
95:     viewModel: QuizViewModel,
96:     onNavigateToResults: (com.znam.app.QuizResults) -> Unit,
97:     onShowInterstitialAd: (com.znam.app.QuizResults) -> Unit,
98:     onNoQuestions: () -> Unit
99: ) {
100:     val uiState by viewModel.uiState.collectAsState()
101:     val event by viewModel.events.collectAsState()
102: 
103:     // Handle one-shot events
104:     LaunchedEffect(event) {
92:     viewModel: StatsViewModel,
93:     onNavigateBack: () -> Unit
94: ) {
95:     val state by viewModel.uiState.collectAsState()
96: 
97:     Scaffold(
98:         topBar = {
```
- **Suggested fix:** Use lifecycle-runtime-compose collectAsStateWithLifecycle for uiState and one-shot event collection.
- **Severity rationale:** Timer/event flows can continue while stopped, increasing lifecycle edge-case risk.

### SF-8: DatabaseProvider can create multiple Room instances for the same DB and is partly bypassed
- **File:** app/src/main/java/com/znam/app/DatabaseProvider.kt:22
- **Phase:** Phases 2-3 DI
- **Problem:** AppDatabase factory/build path creates a fresh Room instance per call, while QuizViewModel directly calls DatabaseProvider and bypasses DAO factories.
- **Evidence:**
```
10: 
11:     factory<AppDatabase> { (quizType: String) ->
12:         get<DatabaseProvider>().createDatabase(quizType)
13:     }
14: 
15:     factory<QuestionDao> { (quizType: String) ->
16:         get<AppDatabase> { parametersOf(quizType) }.questionDao()
17:     }
18: 
19:     factory<UserProgressDao> { (quizType: String) ->
20:         get<AppDatabase> { parametersOf(quizType) }.userProgressDao()
20:     }
21: 
22:     fun createDatabase(quizType: String): AppDatabase {
23:         val appContext = requireNotNull(context) { "Context is required to create AppDatabase" }.applicationContext
24:         val dbName = databaseNameForQuizType(quizType)
25:         return Room.databaseBuilder(appContext, AppDatabase::class.java, dbName)
26:             .createFromAsset(dbName)
27:             .addMigrations(*AppDatabase.ALL_MIGRATIONS)
28:             .build()
29:     }
190: 
191:         viewModelScope.launch {
192:             try {
193:                 val loadedQuestions = withContext(Dispatchers.IO) {
194:                     val database = databaseProvider.createDatabase(quizType)
195:                     db = database
196: 
```
- **Suggested fix:** Cache AppDatabase instances per quiz DB name or make DAO injection the single access path with explicit lifecycle/close policy.
- **Severity rationale:** Resource/lifecycle risk and weaker DI consistency.

### SF-9: Lint succeeds but leaves 97 warnings, including hardcoded text/default locale/unused resources
- **File:** app/build/reports/lint-results-debug.txt
- **Phase:** Phase 1 modernization
- **Problem:** The lint gate exits 0, but the report contains 0 errors and 97 warnings.
- **Evidence:**
```
app/build/reports/lint-results-debug.txt summary: 0 errors, 97 warnings. Build log also shows debug minify warning and Kotlin deprecation warnings.
```
- **Suggested fix:** Prioritize localization/default-locale warnings, then prune unused resources or baseline intentionally accepted warnings.
- **Severity rationale:** Not merge-blocking by itself, but quality debt remains measurable.

## Nice-to-have
### NTH-1: Speed Demon threshold and description disagree
- **File:** app/src/main/java/com/znam/app/GamificationManager.kt:28
- **Problem:** Logic unlocks speed achievement under 90 seconds while description says under 60 seconds.
- **Evidence:** `GamificationManager.kt:28 uses XP_SPEED_BONUS_THRESHOLD_SECONDS = 90; GamificationEntities.kt:115 says "Complete a quiz in under 60 seconds".`
- **Suggested fix:** Align the threshold and description.

### NTH-2: Manifest targetSdkVersion is stale/conflicting with Gradle targetSdk
- **File:** app/src/main/AndroidManifest.xml:20
- **Problem:** Manifest still declares targetSdkVersion/tools:targetApi 34 while Gradle targetSdk is 35.
- **Evidence:** `17:         android:label="@string/app_name"
18:         android:supportsRtl="true"
19:         android:theme="@style/Theme.GettingBiology"
20:         android:targetSdkVersion="34"
21:         tools:targetApi="34">
22: 
23:         <meta-data
26: android {
27:     namespace = 'com.znam.app'
28:     compileSdk = 35
29: 
30:     buildFeatures {
31:         buildConfig = true
32:         compose = true
33:     }
34: 
35:     defaultConfig {
36:         applicationId "com.znam.app"
37:         minSdk = 28
38:         targetSdk = 35
39:         versionCode = 6`
- **Suggested fix:** Remove manifest targetSdkVersion/tools:targetApi and keep SDK targeting in Gradle.

### NTH-3: KSP schemaLocation is configured while Room exportSchema=false
- **File:** app/build.gradle:44
- **Problem:** KSP room.schemaLocation is set, but Room databases disable schema export and app/schemas is absent.
- **Evidence:** `app/build.gradle configures ksp arg("room.schemaLocation", "$projectDir/schemas"); AppDatabase and StatsDatabase both declare exportSchema=false.`
- **Suggested fix:** Enable exportSchema and commit schemas if migrations are audited; otherwise remove the unused arg.

### NTH-4: Theme assumes LocalView context is an Activity
- **File:** app/src/main/java/com/znam/app/ui/theme/ZnamTheme.kt:157
- **Problem:** ZnamTheme casts view.context as Activity for status bar updates.
- **Evidence:** `ZnamTheme.kt:157-163 casts (view.context as Activity).window.`
- **Suggested fix:** Use a findActivityOrNull helper and skip system bar writes without an Activity.

### NTH-5: Debug minify warning makes R8 smoke testing ambiguous
- **File:** app/build.gradle:49
- **Problem:** Debug has minify/shrink enabled, but Gradle warns optimizations/obfuscation are disabled for debuggable builds.
- **Evidence:** `/tmp/phase1-build.log: BuildType debug is both debuggable and has isMinifyEnabled true; optimizations and obfuscation are disabled.`
- **Suggested fix:** Use a non-debuggable staging/benchmarkR8 build type for shrinker smoke tests, or disable debug minify for speed.

## What stayed solid
- Build, lint, and unit-test gates all complete successfully on Hydra.
- Prior audit lint/test blockers did not recur: lint has 0 errors and the unit suite is 34/34 passing.
- KSP migration is active: Gradle runs KSP tasks and Room compiler is wired through KSP.
- SmartQuestionSelector includes fallback-to-shuffle behavior and deduplication by question id in the normal 15-question path.
- Level XP formula implementation matches `100*L*(L-1)/2` even though one nearby comment is stale.
- `StatsDatabase` uses explicit migration objects and `CREATE TABLE IF NOT EXISTS`; fresh installs/builds are clean.
- Optional gamification fallback exists in `QuizViewModel` constructor and does not prevent quiz completion if the manager is absent.

## Recommended merge gate
Do not merge `modernization/phase-1` to `master` until MF-1, MF-2, and MF-3 are fixed and verified with `clean assembleDebug`, `lint`, `test`, and at least one migration validation path. SF items can be accepted as backlog only if the operator explicitly accepts degraded gamification correctness, accessibility/i18n debt, and weakened R8 shrink quality. Current branch is buildable and test-clean, but the daily challenge/reward flow and migration-risk items are too central to merge as-is.

## Raw artifact index
- Build log: `/tmp/phase1-build.log`
- Lint log: `/tmp/phase1-lint.log`
- Lint report: `app/build/reports/lint-results-debug.txt`
- Test log: `/tmp/phase1-test.log`
- Diff review: `/tmp/phase1-diff-review.json`
- Dimensions checklist: `/tmp/phase1-dimensions.json`
- Triage JSON scratch: `/tmp/phase1-triage.json`
- Checkpoint: `/tmp/phase1-audit-checkpoint.json`
